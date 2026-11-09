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
// Строка статуса с размером графа, возвращается после индикатора раскладки
let graphSummary = '';
// Граф хотя бы раз загружен: до этого метки видимости циклов не считаются
let graphLoaded = false;

const byId = (id) => document.getElementById(id);

document.addEventListener('DOMContentLoaded', () => {
    initNetwork();
    bindEvents();
    loadGraphData();
    loadCycles();
});

function initNetwork() {
    const options = {
        // Координаты считает layout.js (ForceAtlas2 + постобработка), vis-network только рисует.
        // Физика выключена: граф не «плывёт», узлы можно перетаскивать, и они остаются, где их оставили
        physics: { enabled: false },
        layout: { improvedLayout: false },
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

// Адрес запроса графа: фильтры передаются параметрами, незаданные не передаются.
// withPackage: false — без фильтра пакета, по такому графу строится индекс подсказок пакетов
function buildGraphUrl({ withPackage = true } = {}) {
    const params = new URLSearchParams();

    if (byId('filterSystem').checked) {
        params.set('type', 'USER');
    }
    if (byId('filterSolo').checked) {
        params.set('detailLevel', 'CONNECTED_ONLY');
    }

    const packagePrefix = withPackage ? byId('filterPackage').value.trim() : '';
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
        const beanGraph = await response.json();
        drawGraph(beanGraph, requestNumber);
        updatePackageIndex(beanGraph);
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

async function drawGraph(beanGraph, requestNumber) {
    clearGraph();

    const beans = [...(beanGraph.nodes || []), ...(beanGraph.soloNodes || [])];
    const edges = beanGraph.edges || [];
    graphSummary = plural(beans.length, 'bean', 'beans') + ', ' + plural(edges.length, 'link', 'links');
    setStatus(graphSummary);

    graphLoaded = true;
    if (beans.length === 0) {
        showCanvasMessage('No beans match these filters.');
        updateCycleVisibility();
        return;
    }

    hideCanvasMessage();
    setStatus('Laying out the graph…');
    // Раскладка занимает основной поток (на графе из ~200 бинов около 0.3 с).
    // Сначала даём браузеру отрисовать статус, потом считаем
    await new Promise(resolve => requestAnimationFrame(() => setTimeout(resolve, 0)));
    // пока ждали, могли сменить фильтры — тогда рисовать будет более новый запрос
    if (requestNumber !== requestCounter) return;

    const positions = IocLayout.compute(
        beans.map(bean => ({ id: bean.id, ...nodeSize(bean) })),
        edges.map(edge => ({ from: edge.source.id, to: edge.target.id })));
    nodesDataSet.add(beans.map(bean => ({ ...toVisNode(bean), ...positions[bean.id] })));
    edgesDataSet.add(edges.map(toVisEdge));
    updateCycleVisibility();
    network.fit();
    setStatus(graphSummary);

    // Цикл, выбранный до смены фильтров, остаётся подсвеченным в новом наборе бинов
    if (activeCycleKey !== null) {
        applyActiveCycle({ fit: false });
    }
}

// Размер узла нужен раскладке до того, как vis-network его нарисует, поэтому считаем сами:
// ширина подписи тем же шрифтом плюс поля. Поля = nodes.margin (10 + 10 и 7 + 7) и ещё 8 px,
// которые vis-network добавляет к прямоугольнику; сверено с network.getBoundingBox()
const measureContext = document.createElement('canvas').getContext('2d');
const NODE_MARGIN_X = 28;
const NODE_MARGIN_Y = 22;

function nodeFontSize(bean) {
    return bean.isSystem ? 12 : 13;
}

function nodeSize(bean) {
    const fontSize = nodeFontSize(bean);
    measureContext.font = fontSize + 'px ' + CODE_FONT;
    return {
        width: measureContext.measureText(bean.id).width + NODE_MARGIN_X,
        height: fontSize + NODE_MARGIN_Y
    };
}

function clearGraph() {
    clearSelection();
    resetCompletion();
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
        font: { color: style.text, size: nodeFontSize(bean) },
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
        kind: edge.kind || 'INJECTION',
        hidden: !injection && !showServiceLinks(),
        color: baseColor,
        baseColor,
        // тип из кода в точке внедрения; показывается в панели, если отличается от класса бина
        declaredType: edge.declaredType || null,
        targetClass: edge.target.fullClassName,
        // Служебные связи (DECLARED_IN, DEPENDS_ON) рисуются пунктиром
        dashes: injection ? false : [5, 4]
    };
}

// Служебные связи можно спрятать переключателем. Скрытое ребро в vis-network всё равно участвует
// в раскладке, поэтому бины остаются рядом со своей конфигурацией
function showServiceLinks() {
    return byId('filterServiceLinks').checked;
}

function isHiddenByDefault(edge) {
    return edge.kind !== 'INJECTION' && !showServiceLinks();
}

function toggleServiceLinks() {
    if (activeCycleKey !== null) {
        applyActiveCycle({ fit: false });
    } else if (selectedId !== null) {
        highlightNeighbourhood(selectedId);
    } else {
        resetHighlight();
    }
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
            // Подсвеченные рёбра видны, даже если это скрытые служебные связи
            hidden: !near && isHiddenByDefault(edge),
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
        hidden: isHiddenByDefault(edge),
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
// может быть скрыта фильтрами, об этом пишется под списком.
// Граф на сервере строится один раз при старте и от фильтров не зависит, поэтому циклы
// загружаются один раз при открытии страницы, а не при каждой смене фильтров

async function loadCycles() {
    const results = byId('cycleResults');
    results.replaceChildren(el('p', 'cycle-note', 'Looking for cycles…'));

    try {
        const response = await fetch(CYCLES_URL);
        if (!response.ok) {
            results.replaceChildren(el('p', 'cycle-note', 'Cycles not loaded: the server returned HTTP ' + response.status + '.'));
            return;
        }
        const report = await response.json();
        renderCycles(report);
        showCycleBadge((report.moduleCycles || []).length + (report.beanCycles || []).length);
    } catch (error) {
        results.replaceChildren(el('p', 'cycle-note', 'Can’t reach the application. Check that it is running.'));
    }
}

// Сколько циклов найдено, в заголовке панели. Красный, если циклы есть
function showCycleBadge(count) {
    const badge = byId('cycleBadge');
    badge.textContent = count > 0 ? plural(count, 'cycle', 'cycles') : 'No cycles';
    badge.toggleAttribute('data-found', count > 0);
    badge.hidden = false;
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
    updateCycleVisibility();
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
        cycleTarget(allEdges), cycle.modules.join('\n'), { showVisibility: true }));

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
    item.append(cycleButton('bean-' + index, label, cycleTarget(cycle.edges, cycle.beans), cycle.beans.join('\n'),
        { showVisibility: true }));
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

// showVisibility — показывать у кнопки, сколько бинов цикла видно при текущих фильтрах.
// Нужно только у самих циклов: у связей внутри модульного цикла справа уже стоит число рёбер
function cycleButton(key, label, target, title, { showVisibility = false } = {}) {
    const button = el('button', 'cycle-button');
    button.type = 'button';
    button.title = title;
    button.setAttribute('aria-pressed', 'false');
    button.append(el('span', 'cycle-label', label));
    let visibility = null;
    if (showVisibility) {
        visibility = el('span', 'cycle-visibility');
        visibility.hidden = true;
        button.append(visibility);
    }
    button.addEventListener('click', () => toggleCycle(key));
    cycleTargets.set(key, { target, button, visibility });
    return button;
}

// Циклы ищутся по полному графу, а показываются в отфильтрованном. Метка в списке заранее
// говорит, что часть бинов цикла (или все) скрыта фильтрами. Обновляется после каждой загрузки графа
function updateCycleVisibility() {
    // пока граф ни разу не загружен, бинов в виде нет, и все циклы выглядели бы скрытыми
    if (!graphLoaded) return;
    cycleTargets.forEach(({ target, button, visibility }) => {
        if (!visibility) return;
        const total = target.nodes.length;
        const shown = target.nodes.filter(id => nodesDataSet.get(id) !== null).length;
        visibility.textContent = shown === 0 ? 'hidden by filters' : shown + ' of ' + total + ' shown';
        visibility.hidden = shown === total;
        button.classList.toggle('is-filtered-out', shown === 0);
    });
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

    if (fit && visible.length > 0) {
        network.fit({ nodes: visible, animation: reduceMotion ? false : { duration: 500, easingFunction: 'easeInOutQuad' } });
    }
}

// Сбрасывает выбор цикла, подсветку при этом меняет тот, кто вызвал
function deactivateCycle() {
    if (activeCycleKey === null) return;
    activeCycleKey = null;
    cycleTargets.forEach(({ button }) => button.setAttribute('aria-pressed', 'false'));
}

function clearCycleHighlight() {
    if (activeCycleKey === null) return;
    deactivateCycle();
    resetHighlight();
}

// Поиск. Бин ищется по полному имени (id) и по короткому: у бинов Spring имя часто полное
// имя класса, и набирать удобнее JdbcClientAutoConfiguration, чем org.springframework…
// Порядок: точное совпадение, потом начинается с запроса, потом содержит его.
// Внутри группы сначала бины приложения, потом более короткие имена, потом по алфавиту

const MATCH_EXACT = 0;
const MATCH_PREFIX = 1;
const MATCH_CONTAINS = 2;

// Найденные бины по порядку. text — та форма имени (id или подпись), которая совпала с запросом,
// её же подставляет автодополнение
function rankMatches(query, { prefixOnly = false } = {}) {
    const lower = query.toLowerCase();
    if (!lower) return [];

    const matches = [];
    nodesDataSet.forEach(node => {
        let best = null;
        for (const text of new Set([node.id, shortBeanName(node.id)])) {
            const name = text.toLowerCase();
            const level = name === lower ? MATCH_EXACT
                : name.startsWith(lower) ? MATCH_PREFIX
                : name.includes(lower) ? MATCH_CONTAINS
                : null;
            if (level === null || (prefixOnly && level === MATCH_CONTAINS)) continue;
            // из двух форм имени берём более точное совпадение, при равенстве — более короткое
            if (best === null || level < best.level || (level === best.level && text.length < best.text.length)) {
                best = { id: node.id, text, level, isSystem: Boolean(node.meta.isSystem) };
            }
        }
        if (best !== null) matches.push(best);
    });

    return matches.sort((a, b) =>
        a.level - b.level
        || Number(a.isSystem) - Number(b.isSystem)
        || a.text.length - b.text.length
        || a.text.localeCompare(b.text));
}

// org.example.FooAutoConfiguration → FooAutoConfiguration,
// spring.web-org.springframework…WebProperties → WebProperties, orderService → orderService
function shortBeanName(id) {
    return id.slice(id.lastIndexOf('.') + 1) || id;
}

function findBean(event) {
    event.preventDefault();
    hideSearchGhost();
    const message = byId('searchMessage');
    const query = byId('searchInput').value.trim();
    if (!query) {
        message.textContent = '';
        return;
    }

    const matches = rankMatches(query);
    if (matches.length === 0) {
        message.textContent = 'No bean matching “' + query + '” in this view.';
        return;
    }

    message.textContent = matches.length > 1 ? matches.length + ' matches, showing ' + matches[0].id + '.' : '';
    selectBean(matches[0].id);
}

// Автодополнение: серый хвост первого подходящего имени, Tab принимает его,
// следующие Tab перебирают остальные варианты, Shift+Tab идёт назад.
// Подсказываются только имена, которые начинаются с набранного: иначе хвост нельзя дописать к тексту
//
// completion хранит варианты для текста, который человек набрал сам:
// query — этот текст, matches — варианты, index — какой показан,
// accepted — вариант уже подставлен в поле, и Tab переключает на следующий
let completion = null;

function onSearchInput(event) {
    byId('searchMessage').textContent = '';
    if (event.isComposing) return;
    const query = byId('searchInput').value;
    completion = { query, matches: rankMatches(query, { prefixOnly: true }), index: 0, accepted: false };
    renderSearchGhost();
}

function onSearchKeydown(event) {
    const input = byId('searchInput');

    if (event.key === 'Tab' && !event.altKey && !event.ctrlKey && !event.metaKey) {
        if (!completion || completion.matches.length === 0) return;
        const current = completion.matches[completion.index];
        const hasTail = !completion.accepted && current.text.length > completion.query.length;
        // Нечего дописывать и не из чего выбирать — Tab работает как обычно и переводит фокус
        if (!hasTail && completion.matches.length === 1) return;
        if (event.shiftKey && !completion.accepted) return;

        event.preventDefault();
        if (completion.accepted || !hasTail) {
            const step = event.shiftKey ? -1 : 1;
            const count = completion.matches.length;
            completion.index = (completion.index + step + count) % count;
        }
        acceptCompletion();
        return;
    }

    // Стрелка вправо в конце строки принимает подсказку, как в браузерах и терминалах
    if (event.key === 'ArrowRight' && !byId('searchGhost').hidden && isCaretAtEnd(input)) {
        event.preventDefault();
        acceptCompletion();
        return;
    }

    // Esc сначала убирает подсказку, и только следующий Esc закрывает панель или снимает подсветку
    // preventDefault нужен, потому что поле type="search" по Esc само стирает весь текст
    if (event.key === 'Escape' && !byId('searchGhost').hidden) {
        event.preventDefault();
        event.stopPropagation();
        // подсказка отклонена: не вернётся при фокусе, а Tab снова переводит фокус
        resetCompletion();
    }
}

function acceptCompletion() {
    const input = byId('searchInput');
    const match = completion.matches[completion.index];
    // Присваивание value не вызывает событие input, поэтому набранный запрос и варианты сохраняются
    input.value = match.text;
    input.setSelectionRange(match.text.length, match.text.length);
    completion.accepted = true;
    hideSearchGhost();

    const count = completion.matches.length;
    byId('searchMessage').textContent = count > 1
        ? (completion.index + 1) + ' of ' + count + ' · Tab for next, Shift+Tab for previous'
        : '';
}

function renderSearchGhost() {
    const match = completion && !completion.accepted ? completion.matches[completion.index] : null;
    if (match) {
        showGhost(byId('searchGhost'), completion.query, match.text);
    } else {
        hideSearchGhost();
    }
}

function hideSearchGhost() {
    hideGhost(byId('searchGhost'));
}

// Варианты подсказки берутся из текущего вида; после смены фильтров их нужно пересчитать
function resetCompletion() {
    completion = null;
    hideSearchGhost();
}

// Серая подсказка, общая для поиска бина и поля пакета.
// Набранная часть повторяется невидимой, чтобы серый хвост начинался ровно после курсора.
// Берём текст из поля, а не из варианта: у них может отличаться регистр, а значит и ширина букв
function showGhost(ghost, typed, full) {
    if (full.length <= typed.length) {
        hideGhost(ghost);
        return;
    }
    ghost.replaceChildren(el('span', 'ghost-typed', typed), full.slice(typed.length));

    // Если текст не помещается и поле начало прокручиваться, подсказка разъедется с ним, лучше не показывать.
    // Ширину можно измерить только у видимого элемента, поэтому сначала показываем
    ghost.hidden = false;
    ghost.hidden = ghost.scrollWidth > ghost.clientWidth;
}

function hideGhost(ghost) {
    ghost.hidden = true;
    ghost.replaceChildren();
}

function isCaretAtEnd(input) {
    return input.selectionStart === input.value.length && input.selectionEnd === input.value.length;
}

// Автодополнение пакета. Подсказывается только следующий сегмент с точкой на конце:
// com.ex → com.example. → com.example.shop. Tab принимает и сразу предлагает следующий уровень,
// стрелки ↑↓ переключают соседние пакеты. Точка в конце заодно делает фильтр точным:
// сервер сравнивает обычным startsWith, и com.example.sh захватил бы и shop, и shipping
//
// packageIndex — пакет → сколько бинов в нём и во всех подпакетах, вместе с промежуточными
// уровнями (com, com.example, com.example.shop, …). Строится по графу без фильтра пакета,
// иначе после применения фильтра соседние пакеты пропали бы из подсказок.
// packageIndexKey — адрес запроса без фильтра пакета, по которому построен индекс:
// пока остальные фильтры не меняются, индекс не перестраивается
let packageIndex = new Map();
let packageIndexKey = null;
// query — текст в поле, matches — варианты { pkg, full, count }, index — какой показан
let packageCompletion = null;
let packageTimer = null;

function buildPackageIndex(beanGraph) {
    const index = new Map();
    for (const bean of [...(beanGraph.nodes || []), ...(beanGraph.soloNodes || [])]) {
        const className = bean.fullClassName;
        const lastDot = className ? className.lastIndexOf('.') : -1;
        // Unknown и классы без пакета пропускаем
        if (lastDot <= 0) continue;
        const pkg = className.slice(0, lastDot);
        // бин считается в своём пакете и во всех его предках
        let dot = -1;
        do {
            dot = pkg.indexOf('.', dot + 1);
            const prefix = dot < 0 ? pkg : pkg.slice(0, dot);
            index.set(prefix, (index.get(prefix) || 0) + 1);
        } while (dot >= 0);
    }
    return index;
}

// Вызывается после каждой загрузки графа
async function updatePackageIndex(beanGraph) {
    const key = buildGraphUrl({ withPackage: false });
    if (key === packageIndexKey) return;
    packageIndexKey = key;

    if (!byId('filterPackage').value.trim()) {
        // фильтра пакета нет — пришедший граф и есть нужный, лишний запрос не нужен
        packageIndex = buildPackageIndex(beanGraph);
    } else {
        try {
            const response = await fetch(key);
            // пока ждали ответ, фильтры могли смениться ещё раз
            if (key !== packageIndexKey) return;
            if (!response.ok) {
                packageIndexKey = null;
                return;
            }
            packageIndex = buildPackageIndex(await response.json());
        } catch (error) {
            if (key === packageIndexKey) packageIndexKey = null;
            return;
        }
    }
    // если человек сейчас в поле, подсказка пересчитывается по новому индексу
    if (document.activeElement === byId('filterPackage')) {
        updatePackageCompletion();
    }
}

// Пакеты, которые дописывают набранный текст ровно до конца текущего сегмента.
// Больше бинов — раньше, при равенстве по алфавиту
function packageMatches(query) {
    if (!query) return [];
    const lower = query.toLowerCase();
    const matches = [];
    packageIndex.forEach((count, pkg) => {
        const full = pkg + '.';
        if (full.length <= query.length) return;
        if (!full.toLowerCase().startsWith(lower)) return;
        // точка после набранного означает, что это уже следующий уровень, а не текущий сегмент
        if (pkg.indexOf('.', query.length) >= 0) return;
        matches.push({ pkg, full, count });
    });
    return matches.sort((a, b) => b.count - a.count || a.pkg.localeCompare(b.pkg));
}

function updatePackageCompletion() {
    const query = byId('filterPackage').value;
    packageCompletion = { query, matches: packageMatches(query), index: 0 };
    renderPackageGhost();
}

function renderPackageGhost() {
    const ghost = byId('packageGhost');
    const message = byId('packageMessage');
    const match = currentPackageMatch();
    if (!match) {
        hidePackageGhost();
        return;
    }
    // Имена пакетов длинные, а поле узкое: серый хвост часто не помещается и не рисуется.
    // Подсказка при этом продолжает работать, а что будет подставлено, видно в подписи под полем
    showGhost(ghost, packageCompletion.query, match.full);

    const count = packageCompletion.matches.length;
    const tail = match.full.slice(packageCompletion.query.length);
    message.textContent = (ghost.hidden ? 'Tab → …' + tail : match.pkg.slice(match.pkg.lastIndexOf('.') + 1))
        + ' · ' + plural(match.count, 'bean', 'beans')
        + (count > 1 ? ' · ' + (packageCompletion.index + 1) + '/' + count + ' ↑↓' : '');
}

function currentPackageMatch() {
    return packageCompletion ? packageCompletion.matches[packageCompletion.index] || null : null;
}

function hidePackageGhost() {
    hideGhost(byId('packageGhost'));
    byId('packageMessage').textContent = '';
}

function onPackageInput(event) {
    schedulePackageFilter();
    if (event.isComposing) return;
    updatePackageCompletion();
}

function onPackageKeydown(event) {
    const input = byId('filterPackage');
    if (!currentPackageMatch()) return;

    const plainTab = event.key === 'Tab' && !event.shiftKey && !event.altKey && !event.ctrlKey && !event.metaKey;
    if (plainTab || (event.key === 'ArrowRight' && isCaretAtEnd(input))) {
        event.preventDefault();
        acceptPackage();
    } else if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && packageCompletion.matches.length > 1) {
        event.preventDefault();
        const count = packageCompletion.matches.length;
        const step = event.key === 'ArrowDown' ? 1 : -1;
        packageCompletion.index = (packageCompletion.index + step + count) % count;
        renderPackageGhost();
    } else if (event.key === 'Escape') {
        // убираем подсказку, а не закрываем панель или подсветку
        event.stopPropagation();
        packageCompletion = null;
        hidePackageGhost();
    }
}

function acceptPackage() {
    const input = byId('filterPackage');
    const match = packageCompletion.matches[packageCompletion.index];
    input.value = match.full;
    input.setSelectionRange(match.full.length, match.full.length);
    // присваивание value не вызывает событие input, поэтому фильтр и подсказку запускаем сами;
    // подсказка сразу предлагает следующий уровень
    schedulePackageFilter();
    updatePackageCompletion();
    updateResetButton();
}

// Фильтр пакета применяется после паузы в наборе, чтобы не дёргать сервер на каждую букву
function schedulePackageFilter() {
    clearTimeout(packageTimer);
    packageTimer = setTimeout(loadGraphData, 400);
}

// Сброс фильтров. Значения по умолчанию берутся из разметки (checked, value, выбранный option),
// поэтому при изменении умолчаний в index.html код менять не нужно
const FILTER_IDS = ['filterSystem', 'filterSolo', 'filterServiceLinks', 'filterPackage', 'filterScope', 'filterStereotype'];

function defaultOptionIndex(select) {
    const index = [...select.options].findIndex(option => option.defaultSelected);
    // если ни у одного option нет атрибута selected, браузер выбирает первый
    return index < 0 ? 0 : index;
}

function hasDefaultValue(control) {
    if (control.type === 'checkbox') return control.checked === control.defaultChecked;
    if (control.tagName === 'SELECT') return control.selectedIndex === defaultOptionIndex(control);
    return control.value === control.defaultValue;
}

function restoreDefaultValue(control) {
    if (control.type === 'checkbox') {
        control.checked = control.defaultChecked;
    } else if (control.tagName === 'SELECT') {
        control.selectedIndex = defaultOptionIndex(control);
    } else {
        control.value = control.defaultValue;
    }
}

// Кнопка неактивна, когда сбрасывать нечего
function updateResetButton() {
    byId('resetFiltersBtn').disabled = FILTER_IDS.every(id => hasDefaultValue(byId(id)));
}

function resetFilters() {
    const changed = FILTER_IDS.filter(id => !hasDefaultValue(byId(id)));
    if (changed.length === 0) return;

    changed.forEach(id => restoreDefaultValue(byId(id)));
    // отложенное применение уже стёртого текста пакета больше не нужно
    clearTimeout(packageTimer);
    packageCompletion = null;
    hidePackageGhost();

    // служебные связи переключаются на месте, остальные фильтры требуют нового графа с сервера
    if (changed.includes('filterServiceLinks')) {
        toggleServiceLinks();
    }
    if (changed.some(id => id !== 'filterServiceLinks')) {
        loadGraphData();
    }
    updateResetButton();
}

function bindEvents() {
    byId('searchForm').addEventListener('submit', findBean);
    const searchInput = byId('searchInput');
    searchInput.addEventListener('input', onSearchInput);
    searchInput.addEventListener('keydown', onSearchKeydown);
    // После ввода через IME (китайский, японский) событие input приходит раньше окончания набора
    searchInput.addEventListener('compositionend', onSearchInput);
    searchInput.addEventListener('blur', hideSearchGhost);
    searchInput.addEventListener('focus', renderSearchGhost);

    // Фильтры применяются сразу; поле пакета — после паузы в наборе
    ['filterSystem', 'filterSolo', 'filterScope', 'filterStereotype'].forEach(id =>
        byId(id).addEventListener('change', loadGraphData));
    // Служебные связи уже загружены, переключатель только показывает или прячет их
    byId('filterServiceLinks').addEventListener('change', toggleServiceLinks);

    const packageInput = byId('filterPackage');
    packageInput.addEventListener('input', onPackageInput);
    packageInput.addEventListener('keydown', onPackageKeydown);
    packageInput.addEventListener('compositionend', onPackageInput);
    packageInput.addEventListener('blur', hidePackageGhost);
    packageInput.addEventListener('focus', updatePackageCompletion);

    // input — набор в поле пакета, change — переключатели и списки
    byId('filtersPanel').addEventListener('input', updateResetButton);
    byId('filtersPanel').addEventListener('change', updateResetButton);
    byId('resetFiltersBtn').addEventListener('click', resetFilters);
    updateResetButton();

    byId('fitBtn').addEventListener('click', fitGraph);

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
