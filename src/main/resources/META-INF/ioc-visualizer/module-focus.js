// Схема модульного цикла. По клику на модульный цикл основной граф временно заменяется схемой:
// модули — облака по кругу в порядке цикла, внутри облаков — бины, стрелки — рёбра внедрения,
// из которых сложился цикл. «Show in full graph», Esc или повторный клик возвращают граф как был.
//
// Всё берётся из отчёта о циклах (ModuleCycle), а не из текущего графа, поэтому схема полная
// при любых фильтрах. В какое облако ставить бин, решает сам анализатор: ребро лежит в связи
// ModuleLink(from, to), значит его источник относится к модулю from, а цель — к модулю to.
// Так стрелки всегда соединяют правильные облака, даже когда зависимость записана в @Bean-методе
// конфигурации из другого модуля или идёт через интерфейс из другого пакета.

// Пока схема открыта: ключ цикла в списке, облака и сохранённый основной граф
let moduleFocus = null;

// Мягкие цвета облаков. Красный занят циклом, цвета стереотипов — узлами, поэтому оттенки синего и нейтральные
const CLOUD_COLORS = ['#2F6FDE', '#0F8FA8', '#5B5FC7', '#8A6D3B', '#3F7D5A', '#6B7A8C'];
const CLOUD_PADDING = 22;
const CLOUD_HEADER = 26;
const CLOUD_ROW_GAP = 14;
const CLOUD_COLUMN_GAP = 24;
const CLOUD_GAP = 170;
const MAX_ROWS_PER_COLUMN = 8;
const SCHEME_EDGE_WIDTH = 1.8;

function isModuleFocusOpen() {
    return moduleFocus !== null;
}

// Узлы основного графа, сохранённые на время показа схемы: по ним продолжает работать поиск
function focusSavedNodes() {
    return moduleFocus.saved.nodes;
}

function enterModuleFocus(key, cycle) {
    if (moduleFocus !== null) exitModuleFocus({ restore: true });

    // Сохраняем основной граф без подсветки и с текущими позициями: если человек что-то
    // перетащил, после выхода узлы окажутся там же
    resetHighlight();
    const positions = network.getPositions();
    const saved = {
        nodes: nodesDataSet.get().map(node => ({ ...node, ...positions[node.id] })),
        edges: edgesDataSet.get(),
        view: { position: network.getViewPosition(), scale: network.getScale() },
        message: byId('canvasMessage').hidden ? null : byId('canvasMessage').textContent
    };

    const scheme = buildModuleScheme(cycle);
    moduleFocus = { key, clouds: scheme.clouds, saved };

    nodesDataSet.clear();
    edgesDataSet.clear();
    hideCanvasMessage();
    nodesDataSet.add(scheme.nodes);
    edgesDataSet.add(scheme.edges);
    network.fit();

    showFocusBanner(scheme.order);
    setStatus('Module cycle: ' + plural(scheme.beanCount, 'bean', 'beans') + ', '
        + plural(scheme.edges.length, 'link', 'links'));
}

// restore: true — вернуть сохранённый граф. false — граф всё равно сейчас перезагрузится (смена фильтров)
function exitModuleFocus({ restore }) {
    if (moduleFocus === null) return;
    const { saved } = moduleFocus;
    moduleFocus = null;
    hideFocusBanner();
    if (!restore) return;

    nodesDataSet.clear();
    edgesDataSet.clear();
    nodesDataSet.add(saved.nodes);
    edgesDataSet.add(saved.edges);
    network.moveTo({ position: saved.view.position, scale: saved.view.scale });
    if (saved.message !== null) showCanvasMessage(saved.message);
    // пока была открыта схема, могли переключить показ служебных связей
    resetHighlight();
    setStatus(graphSummary);
}

// ---- Построение схемы ----

function buildModuleScheme(cycle) {
    // Какие бины в каком облаке. Бин может оказаться в двух облаках, если анализатор отнёс его
    // к разным модулям в разных рёбрах; тогда на схеме две копии с разными id
    const members = new Map(cycle.modules.map(module => [module, new Map()]));
    const placements = new Map(); // beanId → Set(module)
    const place = (bean, module, reason) => {
        const inModule = members.get(module);
        if (!inModule.has(bean.id)) inModule.set(bean.id, { bean, reasons: new Set() });
        if (reason) inModule.get(bean.id).reasons.add(reason);
        if (!placements.has(bean.id)) placements.set(bean.id, new Set());
        placements.get(bean.id).add(module);
    };

    const edges = [];
    // Соседи каждого бина на схеме (ключ — модуль|бин): по ним выбирается порядок бинов в облаке
    const neighbours = new Map();
    const connect = (a, b) => {
        if (!neighbours.has(a)) neighbours.set(a, new Set());
        neighbours.get(a).add(b);
    };
    for (const link of cycle.links) {
        for (const edge of link.edges) {
            place(edge.source, link.from, edge.injectionType === 'FACTORY_METHOD'
                ? 'its @Bean method is declared in ' + shortModule(link.from) : null);
            place(edge.target, link.to, edge.declaredType && edge.declaredType !== edge.target.fullClassName
                ? 'injected as ' + simpleClassName(edge.declaredType) : null);
            edges.push({ edge, fromModule: link.from, toModule: link.to });
            connect(link.from + '|' + edge.source.id, link.to + '|' + edge.target.id);
            connect(link.to + '|' + edge.target.id, link.from + '|' + edge.source.id);
        }
    }

    // id узла на схеме: имя бина, а у копий — модуль|имя бина
    const nodeId = (beanId, module) => placements.get(beanId).size > 1 ? module + '|' + beanId : beanId;

    const order = moduleOrder(cycle);
    const clouds = layoutClouds(order, members, neighbours);

    const nodes = [];
    clouds.forEach(cloud => cloud.members.forEach(member => {
        const { bean, reasons, x, y } = member;
        const id = nodeId(bean.id, cloud.module);
        // облако потом находит свои узлы по этому id
        member.nodeId = id;
        const node = { ...toVisNode(bean), id, x, y };
        if (placements.get(bean.id).size > 1 && reasons.size > 0) {
            node.title = bean.fullClassName + '\nShown in ' + shortModule(cloud.module) + ': ' + [...reasons].join(', ');
        }
        nodes.push(node);
    }));

    // Все рёбра схемы — рёбра цикла, поэтому сразу красные; подсветка и её сброс это сохраняют
    const cycleColor = { color: CYCLE_COLOR, highlight: CYCLE_COLOR, hover: CYCLE_COLOR };
    const visEdges = edges.map(({ edge, fromModule, toModule }) => ({
        ...toVisEdge(edge),
        from: nodeId(edge.source.id, fromModule),
        to: nodeId(edge.target.id, toModule),
        hidden: false,
        color: cycleColor,
        baseColor: cycleColor,
        width: SCHEME_EDGE_WIDTH,
        baseWidth: SCHEME_EDGE_WIDTH,
        // На схеме бины выровнены по сетке, а у строго горизонтального ребра с изгибом continuous
        // vis-network ставит подпись мимо линии. Лёгкая дуга по часовой стрелке этого не делает
        // и заодно разводит встречные рёбра между облаками
        smooth: { type: 'curvedCW', roundness: 0.12 }
    }));

    return { nodes, edges: visEdges, clouds, order, beanCount: placements.size };
}

// Порядок модулей по кругу: перебираем расстановки и берём ту, где больше всего связей
// идёт к следующему модулю по часовой стрелке. В цикле обычно 2–5 модулей, перебор мгновенный.
// Первый модуль закреплён: повороты одной и той же расстановки ничего не меняют
function moduleOrder(cycle) {
    const modules = cycle.modules;
    if (modules.length <= 2 || modules.length > 7) return modules.slice();

    const linked = new Set(cycle.links.map(link => link.from + '\u0000' + link.to));
    const score = order => order.reduce((sum, module, i) =>
        sum + (linked.has(module + '\u0000' + order[(i + 1) % order.length]) ? 1 : 0), 0);

    let best = modules.slice();
    let bestScore = score(best);
    const permute = (prefix, rest) => {
        if (rest.length === 0) {
            const value = score(prefix);
            if (value > bestScore) {
                best = prefix;
                bestScore = value;
            }
            return;
        }
        rest.forEach((module, i) => permute([...prefix, module], [...rest.slice(0, i), ...rest.slice(i + 1)]));
    };
    permute([modules[0]], modules.slice(1));
    return best;
}

// Облака по кругу, бины внутри облака колонками.
// Порядок бинов в облаке: связанные с облаками выше — сверху, с облаками ниже — снизу.
// Иначе стрелка из верхнего облака к нижнему бину проходит сквозь бины над ним
// и выглядит как связь с ними. Затем порядок уточняется по высоте самих соседей,
// чтобы стрелки между двумя облаками не перекрещивались
function layoutClouds(order, members, neighbours) {
    const clouds = order.map((module, index) => ({
        module,
        color: CLOUD_COLORS[index % CLOUD_COLORS.length],
        members: [...members.get(module).values()]
            .sort((a, b) => a.bean.id.localeCompare(b.bean.id))
            .map(member => ({ ...member, size: nodeSize(member.bean) }))
    }));

    // 1. Размеры облаков: от порядка бинов почти не зависят, поэтому считаем их до сортировки
    clouds.forEach(arrangeMembers);

    // 2. Центры облаков по кругу. Радиус — чтобы соседние облака не налезали друг на друга
    const n = clouds.length;
    const largest = Math.max(...clouds.map(cloud => Math.hypot(cloud.width, cloud.height)));
    const radius = n === 1 ? 0 : (largest + CLOUD_GAP) / (2 * Math.sin(Math.PI / n));
    // Два модуля — слева и справа, больше — по кругу от верхней точки по часовой стрелке
    const startAngle = n === 2 ? Math.PI : -Math.PI / 2;
    const center = new Map();
    clouds.forEach((cloud, i) => {
        const angle = startAngle + (2 * Math.PI * i) / n;
        center.set(cloud.module, { x: radius * Math.cos(angle), y: radius * Math.sin(angle) });
    });

    const keyOf = (cloud, member) => cloud.module + '|' + member.bean.id;
    const place = cloud => {
        arrangeMembers(cloud);
        const { x, y } = center.get(cloud.module);
        cloud.members.forEach(member => {
            member.x = x + member.dx;
            member.y = y + member.dy;
        });
    };
    const meanY = (keys, yOf) => keys.length === 0 ? 0 : keys.reduce((sum, key) => sum + yOf(key), 0) / keys.length;

    // 3. Бины сортируются по средней высоте облаков, с которыми они связаны
    clouds.forEach(cloud => {
        const towards = member => meanY([...(neighbours.get(keyOf(cloud, member)) || [])],
            key => center.get(key.slice(0, key.lastIndexOf('|'))).y);
        cloud.members.sort((a, b) => towards(a) - towards(b) || a.bean.id.localeCompare(b.bean.id));
        place(cloud);
    });

    // 4. Метод барицентров: несколько раз сортируем бины облака по средней высоте их соседей
    // в других облаках. Стандартный приём против пересечений рёбер между двумя рядами узлов
    const positionOf = new Map();
    const remember = () => clouds.forEach(cloud =>
        cloud.members.forEach(member => positionOf.set(keyOf(cloud, member), member.y)));
    remember();
    for (let pass = 0; pass < 4; pass++) {
        clouds.forEach(cloud => {
            const rank = new Map(cloud.members.map((member, i) => [member, i]));
            const towards = member => meanY([...(neighbours.get(keyOf(cloud, member)) || [])],
                key => positionOf.get(key));
            cloud.members.sort((a, b) => towards(a) - towards(b) || rank.get(a) - rank.get(b));
            place(cloud);
        });
        remember();
    }
    return clouds;
}

// Бины облака колонками по MAX_ROWS_PER_COLUMN. Координаты dx, dy — относительно центра облака
function arrangeMembers(cloud) {
    const columns = [];
    for (let i = 0; i < cloud.members.length; i += MAX_ROWS_PER_COLUMN) {
        columns.push(cloud.members.slice(i, i + MAX_ROWS_PER_COLUMN));
    }
    const columnWidths = columns.map(column => Math.max(...column.map(m => m.size.width)));
    const columnHeights = columns.map(column =>
        column.reduce((sum, m) => sum + m.size.height, 0) + CLOUD_ROW_GAP * (column.length - 1));
    const innerWidth = columnWidths.reduce((sum, w) => sum + w, 0) + CLOUD_COLUMN_GAP * (columns.length - 1);
    const innerHeight = Math.max(...columnHeights);

    let left = -innerWidth / 2;
    columns.forEach((column, c) => {
        let top = -innerHeight / 2 + CLOUD_HEADER / 2;
        column.forEach(member => {
            member.dx = left + columnWidths[c] / 2;
            member.dy = top + member.size.height / 2;
            top += member.size.height + CLOUD_ROW_GAP;
        });
        left += columnWidths[c] + CLOUD_COLUMN_GAP;
    });

    cloud.width = innerWidth + 2 * CLOUD_PADDING;
    cloud.height = innerHeight + 2 * CLOUD_PADDING + CLOUD_HEADER;
}

// ---- Облака рисуются под узлами в хуке beforeDrawing, по текущим позициям узлов:
// если перетащить бин, облако растянется за ним ----

function drawModuleClouds(ctx) {
    if (moduleFocus === null) return;
    const positions = network.getPositions();

    ctx.save();
    for (const cloud of moduleFocus.clouds) {
        let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
        for (const member of cloud.members) {
            const p = positions[member.nodeId];
            if (!p) continue;
            minX = Math.min(minX, p.x - member.size.width / 2);
            maxX = Math.max(maxX, p.x + member.size.width / 2);
            minY = Math.min(minY, p.y - member.size.height / 2);
            maxY = Math.max(maxY, p.y + member.size.height / 2);
        }
        if (minX === Infinity) continue;

        const x = minX - CLOUD_PADDING;
        const y = minY - CLOUD_PADDING - CLOUD_HEADER;
        const width = maxX - minX + 2 * CLOUD_PADDING;
        const height = maxY - minY + 2 * CLOUD_PADDING + CLOUD_HEADER;

        ctx.beginPath();
        if (ctx.roundRect) {
            ctx.roundRect(x, y, width, height, 14);
        } else {
            ctx.rect(x, y, width, height); // старые браузеры без roundRect
        }
        ctx.fillStyle = cloud.color + '14';   // ~8% непрозрачности
        ctx.fill();
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = cloud.color + '80'; // ~50%
        ctx.stroke();

        ctx.fillStyle = cloud.color;
        ctx.font = '600 14px ' + UI_FONT;
        ctx.textBaseline = 'top';
        ctx.fillText(shortModule(cloud.module), x + 14, y + 11);
    }
    ctx.restore();
}

// ---- Плашка над схемой ----

function showFocusBanner(order) {
    byId('focusTitle').textContent = [...order, order[0]].map(shortModule).join(' → ');
    byId('focusTitle').title = order.join('\n');
    byId('focusBanner').hidden = false;
}

function hideFocusBanner() {
    byId('focusBanner').hidden = true;
}

// Вернуться к основному графу, оставив цикл подсвеченным красным, как при обычном клике
function showFocusedCycleInGraph() {
    if (moduleFocus === null) return;
    closeDetails();
    exitModuleFocus({ restore: true });
    applyActiveCycle({ fit: true });
}

function bindModuleFocusEvents() {
    byId('focusShowInGraph').addEventListener('click', showFocusedCycleInGraph);
    byId('focusClose').addEventListener('click', () => clearCycleHighlight());
}
