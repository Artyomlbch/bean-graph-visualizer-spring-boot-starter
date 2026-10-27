const API_URL = '../api/ioc-visualizer/graph';
const CYCLES_URL = '../api/ioc-visualizer/analysis/cycles';

const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

// Цвета узлов по стереотипу: заливка, рамка и текст. Совпадают с легендой в styles.css
const STEREOTYPES = {
    CONTROLLER:    { name: 'Controller',    fill: '#EFE9FC', border: '#6A3FC9', text: '#3A1F7A' },
    SERVICE:       { name: 'Service',       fill: '#DFF3EC', border: '#13806C', text: '#0B4D41' },
    REPOSITORY:    { name: 'Repository',    fill: '#FCEDD6', border: '#B26A0C', text: '#6B3F06' },
    CONFIGURATION: { name: 'Configuration', fill: '#FBE4EE', border: '#B0386B', text: '#6A1F3F' },
    UNKNOWN:       { name: 'No stereotype', fill: '#FFFFFF', border: '#67768A', text: '#1C2631' }
};
const FRAMEWORK_STYLE = { fill: '#F4F6F8', border: '#A4AFBB', text: '#5B6877' };

const INK = '#1C2631';
const PAPER = '#EBEFF3';
const GRID_COLOR = '#C3CCD6';
const EDGE_COLOR = '#7A8696';
const SERVICE_EDGE_COLOR = '#A4AFBB';
const LABEL_COLOR = '#5B6877';
// Рёбра найденного цикла. Красный нигде больше не используется, поэтому всегда значит «цикл»
const CYCLE_COLOR = '#C8322A';
const EDGE_WIDTH = 1.2;
const UI_FONT = '"Segoe UI Variable Text", "Segoe UI", system-ui, -apple-system, Arial, sans-serif';
const CODE_FONT = '"Cascadia Mono", "JetBrains Mono", Consolas, "SFMono-Regular", Menlo, monospace';

// Подписи рёбер и списков в панели деталей
const LINK_LABELS = {
    CONSTRUCTOR: 'constructor',
    FIELD: 'field',
    SETTER: 'method',
    FACTORY_METHOD: '@Bean method',
    UNKNOWN: 'unknown',
    DECLARED_IN: 'declared in',
    DEPENDS_ON: '@DependsOn'
};

const nodesDataSet = new vis.DataSet();
const edgesDataSet = new vis.DataSet();
let network = null;
let selectedId = null;
let requestCounter = 0;
// Какое направление связей показано в панели деталей. Запоминается при переходе к другому бину,
// чтобы можно было идти по цепочке зависимостей в одну сторону
let linkDirection = 'dependencies';
// Подсвеченный цикл: ключ кнопки в списке циклов и что подсвечивать
let activeCycleKey = null;
const cycleTargets = new Map();

const byId = (id) => document.getElementById(id);

document.addEventListener('DOMContentLoaded', () => {
    initNetwork();
    bindEvents();
    loadGraphData();
});

function initNetwork() {
    const options = {
        physics: {
            solver: 'forceAtlas2Based',
            forceAtlas2Based: {
                gravitationalConstant: -150,
                centralGravity: 0.01,
                springLength: 130,
                springConstant: 0.08
            },
            stabilization: { iterations: 250 }
        },
        layout: { improvedLayout: true },
        interaction: { hover: true, tooltipDelay: 300 },
        nodes: {
            shape: 'box',
            margin: { top: 7, right: 10, bottom: 7, left: 10 },
            font: { face: CODE_FONT, size: 13 },
            shapeProperties: { borderRadius: 4 }
        },
        edges: {
            width: EDGE_WIDTH,
            selectionWidth: 0.8,
            arrows: { to: { enabled: true, scaleFactor: 0.55 } },
            smooth: { type: 'continuous' },
            font: { face: UI_FONT, size: 10, color: LABEL_COLOR, strokeWidth: 4, strokeColor: PAPER, align: 'middle' },
            // При сильном отдалении подписи рёбер скрываются, чтобы не превращаться в шум
            scaling: { label: { drawThreshold: 8 } }
        }
    };

    network = new vis.Network(byId('network-container'), { nodes: nodesDataSet, edges: edgesDataSet }, options);

    network.on('beforeDrawing', drawGrid);

    // Когда раскладка готова, физика выключается: граф перестаёт «плыть», пока его разглядывают.
    // Перетаскивать узлы по-прежнему можно
    network.on('stabilizationIterationsDone', () => {
        network.setOptions({ physics: { enabled: false } });
        network.fit();
    });
    network.on('selectNode', (params) => selectBean(params.nodes[0], { focus: false }));
    network.on('deselectNode', (params) => {
        if (params.nodes.length === 0) {
            clearSelection();
        }
    });
    // Клик по пустому месту снимает подсветку цикла
    network.on('click', (params) => {
        if (params.nodes.length === 0 && params.edges.length === 0) {
            clearCycleHighlight();
        }
    });
}

// Сетка точек рисуется в координатах графа, поэтому двигается и масштабируется вместе с ним
function drawGrid(ctx) {
    const container = byId('network-container');
    const scale = network.getScale();

    let step = 24;
    while (step * scale < 14) {
        step *= 2;
    }

    const topLeft = network.DOMtoCanvas({ x: 0, y: 0 });
    const bottomRight = network.DOMtoCanvas({ x: container.clientWidth, y: container.clientHeight });
    const dot = 1.6 / scale;

    ctx.save();
    ctx.fillStyle = GRID_COLOR;
    for (let x = Math.floor(topLeft.x / step) * step; x <= bottomRight.x; x += step) {
        for (let y = Math.floor(topLeft.y / step) * step; y <= bottomRight.y; y += step) {
            ctx.fillRect(x - dot / 2, y - dot / 2, dot, dot);
        }
    }
    ctx.restore();
}

// Загрузка графа

// Адрес запроса графа: фильтры передаются параметрами, незаданные не передаются
function buildGraphUrl() {
    const params = new URLSearchParams();

    if (byId('filterSystem').checked) {
        params.set('type', 'USER');
    }
    if (byId('filterSolo').checked) {
        params.set('detailLevel', 'CONNECTED_ONLY');
    }

    const packagePrefix = byId('filterPackage').value.trim();
    if (packagePrefix) {
        params.set('packagePrefix', packagePrefix);
    }

    const scope = byId('filterScope').value;
    if (scope !== 'ALL') {
        params.set('scope', scope);
    }

    const stereotype = byId('filterStereotype').value;
    if (stereotype !== 'ALL') {
        params.set('stereotype', stereotype);
    }

    const query = params.toString();
    return query ? API_URL + '?' + query : API_URL;
}

async function loadGraphData() {
    // Если фильтры меняют быстрее, чем приходит ответ, рисуем только последний ответ
    const requestNumber = ++requestCounter;
    setStatus('Loading graph…');

    try {
        const response = await fetch(buildGraphUrl());
        if (requestNumber !== requestCounter) return;

        if (!response.ok) {
            showLoadError(response.status, await response.text());
            return;
        }
        drawGraph(await response.json());
    } catch (error) {
        if (requestNumber === requestCounter) {
            showLoadError(0, '');
        }
    }
}

function showLoadError(status, body) {
    clearGraph();
    setStatus('Graph not loaded');

    if (status === 404) {
        showCanvasMessage('The graph API is not available. Check that the application sets ioc-visualizer.enabled=true.');
    } else if (status === 400) {
        showCanvasMessage('The server rejected the filters: ' + (body || 'bad request') + '.');
    } else if (status === 0) {
        showCanvasMessage('Can’t reach the application. Check that it is running.');
    } else {
        showCanvasMessage('The server returned an error (HTTP ' + status + ').');
    }
}

function drawGraph(beanGraph) {
    clearGraph();

    const beans = [...(beanGraph.nodes || []), ...(beanGraph.soloNodes || [])];
    const edges = beanGraph.edges || [];
    setStatus(plural(beans.length, 'bean', 'beans') + ', ' + plural(edges.length, 'link', 'links'));

    if (beans.length === 0) {
        showCanvasMessage('No beans match these filters.');
        return;
    }

    hideCanvasMessage();
    nodesDataSet.add(beans.map(toVisNode));
    edgesDataSet.add(edges.map(toVisEdge));

    // Цикл, выбранный до смены фильтров, остаётся подсвеченным в новом наборе бинов
    if (activeCycleKey !== null) {
        applyActiveCycle({ fit: false });
    }

    // Раскладку для нового набора бинов считаем явно: при добавлении данных vis-network
    // сам её не запускает и не вписывает граф в экран
    network.setOptions({ physics: { enabled: true } });
    network.stabilize(250);
}

function clearGraph() {
    clearSelection();
    nodesDataSet.clear();
    edgesDataSet.clear();
}

function toVisNode(bean) {
    const style = bean.isSystem ? FRAMEWORK_STYLE : (STEREOTYPES[bean.stereotype] || STEREOTYPES.UNKNOWN);

    return {
        id: bean.id,
        label: bean.id,
        title: bean.fullClassName,
        color: {
            background: style.fill,
            border: style.border,
            highlight: { background: style.fill, border: INK },
            hover: { background: style.fill, border: INK }
        },
        font: { color: style.text, size: bean.isSystem ? 12 : 13 },
        borderWidth: bean.isSystem ? 1 : 1.5,
        borderWidthSelected: 2.5,
        // Служебные бины Spring и библиотек обведены пунктиром
        shapeProperties: { borderRadius: 4, borderDashes: bean.isSystem ? [4, 3] : false },
        meta: bean
    };
}

function toVisEdge(edge) {
    const injection = isInjection(edge);
    const baseColor = { color: injection ? EDGE_COLOR : SERVICE_EDGE_COLOR, highlight: INK, hover: INK };

    return {
        from: edge.source.id,
        to: edge.target.id,
        label: linkLabel(edge),
        color: baseColor,
        baseColor,
        // тип из кода в точке внедрения; показывается в панели, если отличается от класса бина
        declaredType: edge.declaredType || null,
        targetClass: edge.target.fullClassName,
        // Служебные связи (DECLARED_IN, DEPENDS_ON) рисуются пунктиром
        dashes: injection ? false : [5, 4]
    };
}

function isInjection(edge) {
    return !edge.kind || edge.kind === 'INJECTION';
}

function linkLabel(edge) {
    return isInjection(edge)
        ? (LINK_LABELS[edge.injectionType] || '')
        : (LINK_LABELS[edge.kind] || edge.kind);
}

// Выбор бина и подсветка его соседей

function selectBean(id, { focus = true } = {}) {
    const node = nodesDataSet.get(id);
    if (!node) return;

    deactivateCycle();
    selectedId = id;
    network.selectNodes([id]);
    highlightNeighbourhood(id);
    showNodeDetails(node.meta);

    if (focus) {
        network.focus(id, {
            scale: Math.max(network.getScale(), 1),
            animation: reduceMotion ? false : { duration: 600, easingFunction: 'easeInOutQuad' }
        });
    }
}

function highlightNeighbourhood(id) {
    highlight(new Set([id, ...network.getConnectedNodes(id)]), new Set(network.getConnectedEdges(id)));
}

// Выделенные узлы и рёбра остаются яркими, остальное приглушается.
// Если задан edgeColor, выделенные рёбра перекрашиваются и становятся толще
function highlight(nodeIds, edgeIds, edgeColor = null) {
    nodesDataSet.update(nodesDataSet.getIds().map(nodeId => ({
        id: nodeId,
        opacity: nodeIds.has(nodeId) ? 1 : 0.2
    })));
    edgesDataSet.update(edgesDataSet.get().map(edge => {
        const near = edgeIds.has(edge.id);
        const recolor = near && edgeColor !== null;
        return {
            id: edge.id,
            color: recolor
                ? { color: edgeColor, highlight: edgeColor, hover: edgeColor, opacity: 1 }
                : { ...edge.baseColor, opacity: near ? 1 : 0.15 },
            width: recolor ? 2.4 : EDGE_WIDTH,
            font: { color: near ? INK : 'rgba(91, 104, 119, 0.15)' }
        };
    }));
}

function resetHighlight() {
    nodesDataSet.update(nodesDataSet.getIds().map(id => ({ id, opacity: 1 })));
    edgesDataSet.update(edgesDataSet.get().map(edge => ({
        id: edge.id,
        color: { ...edge.baseColor, opacity: 1 },
        width: EDGE_WIDTH,
        font: { color: LABEL_COLOR }
    })));
}

function clearSelection() {
    if (selectedId === null) return;
    selectedId = null;
    resetHighlight();
    closeDetailsPanel();
}

function closeDetails() {
    network.unselectAll();
    clearSelection();
}

// Панель деталей. Все значения из графа вставляются через textContent, поэтому HTML в них не выполняется

function showNodeDetails(bean) {
    const info = byId('node-info');
    info.replaceChildren(
        el('h2', 'bean-id', bean.id),
        classLine(bean.fullClassName),
        chips(bean),
        linkSection(bean)
    );

    const panel = byId('details-panel');
    panel.classList.add('is-open');
    panel.setAttribute('aria-hidden', 'false');
    panel.inert = false;
    panel.scrollTop = 0;
}

function closeDetailsPanel() {
    const panel = byId('details-panel');
    panel.classList.remove('is-open');
    panel.setAttribute('aria-hidden', 'true');
    panel.inert = true;
}

// Пакет приглушён, простое имя класса выделено. Переносы строк — только после точек
function classLine(fullClassName) {
    const line = el('p', 'bean-class');
    const name = fullClassName || 'Unknown class';
    const dot = name.lastIndexOf('.');
    if (dot > 0) {
        name.slice(0, dot + 1).split(/(?<=\.)/).forEach(part => line.append(part, document.createElement('wbr')));
        line.append(el('strong', null, name.slice(dot + 1)));
    } else {
        line.append(el('strong', null, name));
    }
    return line;
}

function chips(bean) {
    const container = el('div', 'chips');

    const stereotype = el('span', 'chip', (STEREOTYPES[bean.stereotype] || STEREOTYPES.UNKNOWN).name);
    if (!bean.isSystem && STEREOTYPES[bean.stereotype] && bean.stereotype !== 'UNKNOWN') {
        stereotype.dataset.stereotype = bean.stereotype;
    }

    const scopeNames = { SINGLETON: 'Singleton', PROTOTYPE: 'Prototype' };
    container.append(
        stereotype,
        el('span', 'chip', scopeNames[bean.scope] || 'Unknown scope'),
        el('span', 'chip', bean.isSystem ? 'Framework or library bean' : 'Application bean')
    );
    return container;
}

// Связи бина: переключатель направления и список выбранного направления
function linkSection(bean) {
    const directions = [
        {
            key: 'dependencies',
            title: 'Depends on',
            edges: edgesDataSet.get({ filter: edge => edge.from === bean.id }),
            otherEnd: edge => edge.to,
            empty: 'Doesn’t depend on other beans in this view.'
        },
        {
            key: 'dependents',
            title: 'Used by',
            edges: edgesDataSet.get({ filter: edge => edge.to === bean.id }),
            otherEnd: edge => edge.from,
            empty: 'No bean in this view depends on it.'
        }
    ];

    const section = el('section', 'links');
    const switcher = el('div', 'link-switch');
    switcher.setAttribute('role', 'group');
    switcher.setAttribute('aria-label', 'Direction of links');
    const content = el('div');

    const buttons = directions.map(direction => {
        const button = el('button', 'link-switch-option', direction.title);
        button.type = 'button';
        button.append(el('span', 'link-count', String(direction.edges.length)));
        button.addEventListener('click', () => {
            linkDirection = direction.key;
            render();
        });
        return button;
    });

    function render() {
        directions.forEach((direction, index) =>
            buttons[index].setAttribute('aria-pressed', String(direction.key === linkDirection)));
        const current = directions.find(direction => direction.key === linkDirection);
        content.replaceChildren(linkList(current));
    }

    switcher.append(...buttons);
    section.append(switcher, content);
    render();
    return section;
}

function linkList(direction) {
    if (direction.edges.length === 0) {
        return el('p', 'no-links', direction.empty);
    }

    const list = el('ul', 'link-list');
    direction.edges
        .sort((a, b) => direction.otherEnd(a).localeCompare(direction.otherEnd(b)))
        .forEach(edge => {
            const name = direction.otherEnd(edge);
            const button = el('button');
            button.type = 'button';
            // В коде может быть интерфейс, а внедрена его реализация: тогда пишем, через какой тип
            const via = edge.declaredType && edge.declaredType !== edge.targetClass
                ? ' · via ' + simpleClassName(edge.declaredType)
                : '';
            const kind = el('span', 'link-kind', edge.label + via);
            if (via) {
                kind.title = edge.declaredType;
            }
            button.append(el('span', 'link-name', name), kind);
            button.addEventListener('click', () => selectBean(name));

            const item = el('li');
            item.append(button);
            list.append(item);
        });
    return list;
}

// Циклы. Ищутся по полному графу, а подсвечиваются в текущем: часть бинов цикла
// может быть скрыта фильтрами, об этом пишется под списком

async function findCycles() {
    const button = byId('findCyclesBtn');
    const results = byId('cycleResults');
    button.disabled = true;
    results.replaceChildren(el('p', 'cycle-note', 'Looking for cycles…'));

    try {
        const response = await fetch(CYCLES_URL);
        if (!response.ok) {
            results.replaceChildren(el('p', 'cycle-note', 'Cycles not loaded: the server returned HTTP ' + response.status + '.'));
            return;
        }
        renderCycles(await response.json());
    } catch (error) {
        results.replaceChildren(el('p', 'cycle-note', 'Can’t reach the application. Check that it is running.'));
    } finally {
        button.disabled = false;
    }
}

function renderCycles(report) {
    clearCycleHighlight();
    cycleTargets.clear();

    const moduleCycles = report.moduleCycles || [];
    const beanCycles = report.beanCycles || [];
    const results = byId('cycleResults');

    if (moduleCycles.length === 0 && beanCycles.length === 0) {
        results.replaceChildren(el('p', 'cycle-note', 'No cycles found.'));
        return;
    }

    const parts = [];
    if (moduleCycles.length > 0) {
        const group = cycleGroup('Between modules', moduleCycles.map(moduleCycleItem));
        group.append(el('p', 'hint', 'Usually the direction with the fewest links is the one to remove.'));
        parts.push(group);
    }
    if (beanCycles.length > 0) {
        parts.push(cycleGroup('Between beans', beanCycles.map(beanCycleItem)));
    }
    results.replaceChildren(...parts);
}

function cycleGroup(title, items) {
    const group = el('div', 'cycle-group');
    const heading = el('h3', null, title);
    heading.append(el('span', 'cycle-total', String(items.length)));
    const list = el('ul', 'cycle-list');
    list.append(...items);
    group.append(heading, list);
    return group;
}

// Цикл модулей и его связи, первой идёт самая слабая
function moduleCycleItem(cycle, index) {
    const item = el('li');
    const allEdges = cycle.links.flatMap(link => link.edges);
    item.append(cycleButton('module-' + index, joinCycle(cycle.modules.map(shortModule)),
        cycleTarget(allEdges), cycle.modules.join('\n')));

    const links = el('ul', 'cycle-links');
    cycle.links.forEach((link, linkIndex) => {
        const button = cycleButton('module-' + index + '-' + linkIndex,
            shortModule(link.from) + ' → ' + shortModule(link.to),
            cycleTarget(link.edges), link.from + ' → ' + link.to);
        const count = el('span', 'cycle-count', plural(link.count, 'link', 'links'));
        const next = cycle.links[linkIndex + 1];
        if (linkIndex === 0 && next && link.count < next.count) {
            count.prepend(el('span', 'cycle-flag', 'fewest'));
        }
        button.append(count);

        const linkItem = el('li');
        linkItem.append(button);
        links.append(linkItem);
    });
    item.append(links);
    return item;
}

function beanCycleItem(cycle, index) {
    const item = el('li');
    const shown = cycle.beans.slice(0, 3);
    const rest = cycle.beans.length - shown.length;
    const label = joinCycle(shown) + (rest > 0 ? ' and ' + rest + ' more' : '');
    item.append(cycleButton('bean-' + index, label, cycleTarget(cycle.edges, cycle.beans), cycle.beans.join('\n')));
    return item;
}

// com.example.app.orders.OrderSource → OrderSource, вложенный класс Outer$Inner → Inner
function simpleClassName(className) {
    return className.slice(Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1);
}

// Модуль — полное имя пакета, в списке достаточно последней части: com.example.app.orders → orders
function shortModule(module) {
    return module.slice(module.lastIndexOf('.') + 1);
}

function joinCycle(names) {
    return names.length === 2 ? names[0] + ' ⇄ ' + names[1] : names.join(', ');
}

function cycleTarget(edges, beans = []) {
    return {
        nodes: [...new Set([...beans, ...edges.flatMap(edge => [edge.source.id, edge.target.id])])],
        links: edges.map(edge => edge.source.id + '\u0000' + edge.target.id)
    };
}

function cycleButton(key, label, target, title) {
    const button = el('button', 'cycle-button');
    button.type = 'button';
    button.title = title;
    button.setAttribute('aria-pressed', 'false');
    button.append(el('span', 'cycle-label', label));
    button.addEventListener('click', () => toggleCycle(key));
    cycleTargets.set(key, { target, button });
    return button;
}

function toggleCycle(key) {
    if (activeCycleKey === key) {
        clearCycleHighlight();
        return;
    }
    closeDetails();
    activeCycleKey = key;
    applyActiveCycle({ fit: true });
}

function applyActiveCycle({ fit }) {
    const entry = cycleTargets.get(activeCycleKey);
    if (!entry) return;

    cycleTargets.forEach(({ button }, key) => button.setAttribute('aria-pressed', String(key === activeCycleKey)));

    const { nodes, links } = entry.target;
    const visible = nodes.filter(id => nodesDataSet.get(id) !== null);
    const linkSet = new Set(links);
    const edgeIds = new Set(edgesDataSet.get({ filter: edge => linkSet.has(edge.from + '\u0000' + edge.to) })
        .map(edge => edge.id));
    highlight(new Set(visible), edgeIds, CYCLE_COLOR);

    const hidden = nodes.length - visible.length;
    const note = byId('cycleHiddenNote');
    note.textContent = hidden > 0
        ? plural(hidden, 'bean', 'beans') + ' of this cycle ' + (hidden === 1 ? 'is' : 'are') + ' hidden by the filters.'
        : '';
    note.hidden = hidden === 0;

    if (fit && visible.length > 0) {
        network.fit({ nodes: visible, animation: reduceMotion ? false : { duration: 500, easingFunction: 'easeInOutQuad' } });
    }
}

// Сбрасывает выбор цикла, подсветку при этом меняет тот, кто вызвал
function deactivateCycle() {
    if (activeCycleKey === null) return;
    activeCycleKey = null;
    cycleTargets.forEach(({ button }) => button.setAttribute('aria-pressed', 'false'));
    byId('cycleHiddenNote').hidden = true;
}

function clearCycleHighlight() {
    if (activeCycleKey === null) return;
    deactivateCycle();
    resetHighlight();
}

// Поиск: сначала точное совпадение имени, потом имена, которые начинаются с запроса, потом содержащие его

function findBean(event) {
    event.preventDefault();
    const message = byId('searchMessage');
    const query = byId('searchInput').value.trim();
    if (!query) {
        message.textContent = '';
        return;
    }

    const lower = query.toLowerCase();
    const ids = nodesDataSet.getIds();
    const matches = ids.filter(id => id.toLowerCase() === lower)
        .concat(ids.filter(id => id.toLowerCase() !== lower && id.toLowerCase().startsWith(lower)))
        .concat(ids.filter(id => !id.toLowerCase().startsWith(lower) && id.toLowerCase().includes(lower)));

    if (matches.length === 0) {
        message.textContent = 'No bean matching “' + query + '” in this view.';
        return;
    }

    message.textContent = matches.length > 1 ? matches.length + ' matches, showing ' + matches[0] + '.' : '';
    selectBean(matches[0]);
}

function bindEvents() {
    byId('searchForm').addEventListener('submit', findBean);

    // Фильтры применяются сразу; поле пакета — после паузы в наборе
    ['filterSystem', 'filterSolo', 'filterScope', 'filterStereotype'].forEach(id =>
        byId(id).addEventListener('change', loadGraphData));

    let packageTimer = null;
    byId('filterPackage').addEventListener('input', () => {
        clearTimeout(packageTimer);
        packageTimer = setTimeout(loadGraphData, 400);
    });

    byId('fitBtn').addEventListener('click', fitGraph);
    byId('findCyclesBtn').addEventListener('click', findCycles);

    byId('detailsClose').addEventListener('click', closeDetails);
    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        if (selectedId !== null) {
            closeDetails();
        } else {
            clearCycleHighlight();
        }
    });
}

// Мелкие помощники

function el(tag, className, text) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
}

function plural(count, one, many) {
    return count + ' ' + (count === 1 ? one : many);
}

function fitGraph() {
    network.fit({ animation: reduceMotion ? false : { duration: 500, easingFunction: 'easeInOutQuad' } });
}

function setStatus(text) {
    byId('graphStatus').textContent = text;
}

// Сообщение показывается вместо графа, поэтому кнопка «Fit graph» в это время не нужна
function showCanvasMessage(text) {
    const message = byId('canvasMessage');
    message.textContent = text;
    message.hidden = false;
    byId('fitBtn').hidden = true;
}

function hideCanvasMessage() {
    byId('canvasMessage').hidden = true;
    byId('fitBtn').hidden = false;
}
