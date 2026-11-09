// Раскладка графа. Координаты считаются здесь, vis-network только рисует их с выключенной физикой.
//
// Этапы:
// 1. ForceAtlas2 (graphology) раскладывает граф: связанные бины притягиваются, все отталкиваются.
//    Отталкивание хаба растёт с числом его связей, поэтому конфигурации со множеством @Bean-методов
//    становятся центрами звёзд. dissuadeHubs уводит хабы в центр их группы, сильная гравитация
//    не даёт группам разлетаться.
// 2. Масштаб: ForceAtlas2 выдаёт координаты в своих единицах, приводим их к пикселям так,
//    чтобы типичное ребро было длиной TARGET_EDGE_LENGTH.
// 3. Листья по кругу: бины с единственной связью ставятся по окружности вокруг своего хаба.
// 4. Наложения: ForceAtlas2 считает узлы точками, а у нас прямоугольники с длинными подписями.
//    Пересекающиеся прямоугольники раздвигаются, пока наложений не останется.
// 5. Упаковка: несвязанные между собой части графа раскладываются рядами, крупные первыми,
//    одиночные бины — сеткой в конце. Без этого они разлетаются по краям и тянут длинные рёбра.
//
// На графе из 183 бинов (приложение + Spring Boot) это даёт ~60 пересечений рёбер вместо ~240
// у forceAtlas2Based из vis-network, без наложений узлов, за ~0.3 с.

const IocLayout = (() => {
    const TARGET_EDGE_LENGTH = 180;
    const OVERLAP_MARGIN = 10;
    const COMPONENT_GAP = 60;
    // Начальные позиции случайные, но с постоянным зерном: один и тот же граф раскладывается одинаково
    const SEED = 1;

    // Линейный конгруэнтный генератор: Math.random нельзя задать зерно
    function seededRandom(seed) {
        let state = seed >>> 0;
        return () => {
            state = (state * 1664525 + 1013904223) >>> 0;
            return state / 4294967296;
        };
    }

    // nodes: [{ id, width, height }], edges: [{ from, to }] → { id: { x, y } }
    function compute(nodes, edges) {
        if (nodes.length === 0) return {};
        const links = uniqueLinks(nodes, edges);
        const neighbours = adjacency(nodes, links);

        let positions = forceAtlas2(nodes, links);
        positions = scaleToEdgeLength(positions, links, TARGET_EDGE_LENGTH);
        positions = placeLeavesInRings(positions, nodes, neighbours);
        positions = removeOverlaps(positions, nodes, OVERLAP_MARGIN);
        positions = packComponents(positions, nodes, neighbours, COMPONENT_GAP);
        return positions;
    }

    // Одна связь на пару бинов без учёта направления: для раскладки важно только, связаны ли они
    function uniqueLinks(nodes, edges) {
        const known = new Set(nodes.map(node => node.id));
        const seen = new Set();
        const links = [];
        for (const { from, to } of edges) {
            if (from === to || !known.has(from) || !known.has(to)) continue;
            const key = from < to ? from + '\u0000' + to : to + '\u0000' + from;
            if (seen.has(key)) continue;
            seen.add(key);
            links.push({ from, to });
        }
        return links;
    }

    function adjacency(nodes, links) {
        const neighbours = new Map(nodes.map(node => [node.id, new Set()]));
        for (const { from, to } of links) {
            neighbours.get(from).add(to);
            neighbours.get(to).add(from);
        }
        return neighbours;
    }

    // 1. ForceAtlas2. Алгоритм работает с плоскими массивами: 10 чисел на узел, 3 на ребро
    function forceAtlas2(nodes, links) {
        const NODE_FIELDS = 10;
        const EDGE_FIELDS = 3;
        const random = seededRandom(SEED);
        const offset = new Map();

        const nodeMatrix = new Float32Array(nodes.length * NODE_FIELDS);
        nodes.forEach((node, i) => {
            const at = i * NODE_FIELDS;
            offset.set(node.id, at);
            nodeMatrix[at] = (random() - 0.5) * 1000;     // x
            nodeMatrix[at + 1] = (random() - 0.5) * 1000; // y
            nodeMatrix[at + 6] = 1;                       // масса: 1 + число связей, дополняется ниже
            nodeMatrix[at + 7] = 1;                       // сходимость
            nodeMatrix[at + 8] = 1;                       // размер, не используется (adjustSizes выключен)
        });

        const edgeMatrix = new Float32Array(links.length * EDGE_FIELDS);
        links.forEach(({ from, to }, i) => {
            const at = i * EDGE_FIELDS;
            edgeMatrix[at] = offset.get(from);
            edgeMatrix[at + 1] = offset.get(to);
            edgeMatrix[at + 2] = 1; // вес ребра
            nodeMatrix[offset.get(from) + 6] += 1;
            nodeMatrix[offset.get(to) + 6] += 1;
        });

        // Без оптимизации Барнса — Хата отталкивание считается для всех пар, это O(n²) на итерацию.
        // До нескольких сотен бинов так точнее и достаточно быстро, дальше включаем приближение
        const large = nodes.length > 400;
        const settings = {
            linLogMode: false,
            outboundAttractionDistribution: true, // dissuadeHubs
            adjustSizes: false,
            edgeWeightInfluence: 1,
            scalingRatio: 10,
            strongGravityMode: true,
            gravity: 0.5,
            slowDown: 1,
            barnesHutOptimize: large,
            barnesHutTheta: 0.5
        };
        const iterations = large ? 800 : 1500;
        for (let i = 0; i < iterations; i++) {
            window.forceAtlas2Iterate(settings, nodeMatrix, edgeMatrix);
        }

        const positions = {};
        nodes.forEach(node => {
            const at = offset.get(node.id);
            positions[node.id] = { x: nodeMatrix[at], y: nodeMatrix[at + 1] };
        });
        return positions;
    }

    // 2. Медианная длина ребра становится равной target
    function scaleToEdgeLength(positions, links, target) {
        if (links.length === 0) return positions;
        const lengths = links
            .map(({ from, to }) => Math.hypot(positions[from].x - positions[to].x, positions[from].y - positions[to].y))
            .sort((a, b) => a - b);
        const median = lengths[Math.floor(lengths.length / 2)] || 1;
        const factor = target / median;
        const scaled = {};
        for (const id in positions) {
            scaled[id] = { x: positions[id].x * factor, y: positions[id].y * factor };
        }
        return scaled;
    }

    // 3. Листья (бины с одной связью) вокруг хаба, если у хаба их хотя бы три.
    // Порядок по углу сохраняется, чтобы не перекрещивать то, что ForceAtlas2 уже развёл
    function placeLeavesInRings(positions, nodes, neighbours) {
        const MIN_LEAVES = 3;
        const GAP = 18;
        const size = new Map(nodes.map(node => [node.id, node]));
        const result = copy(positions);

        const leavesByHub = new Map();
        for (const node of nodes) {
            const links = neighbours.get(node.id);
            if (links.size !== 1) continue;
            const hub = links.values().next().value;
            // два бина, связанные только друг с другом, — это не звезда
            if (neighbours.get(hub).size === 1) continue;
            if (!leavesByHub.has(hub)) leavesByHub.set(hub, []);
            leavesByHub.get(hub).push(node.id);
        }

        leavesByHub.forEach((leaves, hub) => {
            if (leaves.length < MIN_LEAVES) return;
            const center = result[hub];
            const angle = id => Math.atan2(result[id].y - center.y, result[id].x - center.x);
            leaves.sort((a, b) => angle(a) - angle(b));

            // Радиус такой, чтобы листья поместились по окружности. Подписи длинные, но соседние
            // листья на круге стоят под разными углами, поэтому берём часть ширины, а не всю
            const perimeter = leaves.reduce((sum, id) =>
                sum + Math.max(size.get(id).width * 0.6, size.get(id).height * 2) + GAP, 0);
            const radius = Math.max(perimeter / (2 * Math.PI), size.get(hub).width / 2 + 60);
            const start = angle(leaves[0]);
            leaves.forEach((id, i) => {
                const a = start + (2 * Math.PI * i) / leaves.length;
                result[id] = { x: center.x + radius * Math.cos(a), y: center.y + radius * Math.sin(a) };
            });
        });
        return result;
    }

    // 4. Раздвигаем пересекающиеся прямоугольники по оси, где они перекрываются меньше.
    // Чтобы не проверять все пары, узлы сортируются по x: дальше по списку пары уже не пересекаются
    function removeOverlaps(positions, nodes, margin) {
        const MAX_ROUNDS = 300;
        const result = copy(positions);
        const maxHalfWidth = Math.max(...nodes.map(node => node.width / 2));
        const order = nodes.slice();

        for (let round = 0; round < MAX_ROUNDS; round++) {
            let moved = false;
            order.sort((a, b) => result[a.id].x - result[b.id].x);
            for (let i = 0; i < order.length; i++) {
                const a = order[i];
                const pa = result[a.id];
                for (let j = i + 1; j < order.length; j++) {
                    const b = order[j];
                    const pb = result[b.id];
                    if (pb.x - pa.x > a.width / 2 + maxHalfWidth + margin) break;

                    const overlapX = (a.width + b.width) / 2 + margin - Math.abs(pa.x - pb.x);
                    const overlapY = (a.height + b.height) / 2 + margin - Math.abs(pa.y - pb.y);
                    if (overlapX <= 0 || overlapY <= 0) continue;

                    moved = true;
                    if (overlapX < overlapY) {
                        const shift = (overlapX / 2) * (pa.x <= pb.x ? -1 : 1);
                        pa.x += shift;
                        pb.x -= shift;
                    } else {
                        const shift = (overlapY / 2) * (pa.y <= pb.y ? -1 : 1);
                        pa.y += shift;
                        pb.y -= shift;
                    }
                }
            }
            if (!moved) break;
        }
        return result;
    }

    // 5. Связные части графа рядами («полками»): каждая сохраняет свою раскладку,
    // крупные идут первыми, одиночные бины оказываются в конце сеткой
    function packComponents(positions, nodes, neighbours, gap) {
        const size = new Map(nodes.map(node => [node.id, node]));
        const components = connectedComponents(nodes, neighbours).map(ids => {
            let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
            for (const id of ids) {
                const { x, y } = positions[id];
                const { width, height } = size.get(id);
                minX = Math.min(minX, x - width / 2);
                maxX = Math.max(maxX, x + width / 2);
                minY = Math.min(minY, y - height / 2);
                maxY = Math.max(maxY, y + height / 2);
            }
            return { ids, minX, minY, width: maxX - minX, height: maxY - minY };
        });
        components.sort((a, b) => b.ids.length - a.ids.length || b.width * b.height - a.width * a.height);

        // Ширина ряда — чтобы итог был примерно 1.4 : 1, как экран, но не уже самой широкой части
        const area = components.reduce((sum, c) => sum + (c.width + gap) * (c.height + gap), 0);
        const rowWidth = Math.max(components[0].width, Math.sqrt(area * 1.4));

        const result = {};
        let x = 0, y = 0, rowHeight = 0;
        for (const component of components) {
            if (x > 0 && x + component.width > rowWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            for (const id of component.ids) {
                result[id] = {
                    x: positions[id].x - component.minX + x,
                    y: positions[id].y - component.minY + y
                };
            }
            x += component.width + gap;
            rowHeight = Math.max(rowHeight, component.height);
        }
        return result;
    }

    function connectedComponents(nodes, neighbours) {
        const seen = new Set();
        const components = [];
        for (const node of nodes) {
            if (seen.has(node.id)) continue;
            const ids = [];
            const stack = [node.id];
            seen.add(node.id);
            while (stack.length > 0) {
                const id = stack.pop();
                ids.push(id);
                neighbours.get(id).forEach(next => {
                    if (!seen.has(next)) {
                        seen.add(next);
                        stack.push(next);
                    }
                });
            }
            components.push(ids);
        }
        return components;
    }

    function copy(positions) {
        const result = {};
        for (const id in positions) {
            result[id] = { x: positions[id].x, y: positions[id].y };
        }
        return result;
    }

    return { compute };
})();
