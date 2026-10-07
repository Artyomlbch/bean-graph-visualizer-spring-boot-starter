let network = null;
let nodesDataSet = new vis.DataSet();
let edgesDataSet = new vis.DataSet();

const API_URL = '/api/ioc-visualizer/graph';

const STEREOTYPE_COLORS = {
    'CONTROLLER': { background: '#D1C4E9', border: '#673AB7' },
    'SERVICE':    { background: '#C8E6C9', border: '#4CAF50' },
    'REPOSITORY': { background: '#FFE0B2', border: '#FF9800' },
    'CONFIGURATION':{ background: '#B3E5FC', border: '#03A9F4' },
    'UNKNOWN':    { background: '#F5F5F5', border: '#9E9E9E' }
};

document.addEventListener('DOMContentLoaded', () => {
    initNetwork();
    bindEvents();
    loadGraphData();
});

function initNetwork() {
    const container = document.getElementById('network-container');
    const data = { nodes: nodesDataSet, edges: edgesDataSet };

    const options = {
        physics: {
            forceAtlas2Based: {
                gravitationalConstant: -150,
                centralGravity: 0.01,
                springLength: 100,
                springConstant: 0.08
            },
            solver: 'forceAtlas2Based',
            stabilization: { iterations: 200 }
        },
        layout: { improvedLayout: true },
        interaction: { hover: true, tooltipDelay: 200 }
    };

    network = new vis.Network(container, data, options);

    network.on("selectNode", (params) => {
        const nodeId = params.nodes[0];
        const nodeData = nodesDataSet.get(nodeId);
        showNodeDetails(nodeData.meta);
    });

    network.on("deselectNode", () => {
        document.getElementById('details-panel').classList.add('hidden');
    });
}

function loadGraphData() {
    const requestDto = { filters: [] };

    if (document.getElementById('filterSystem').checked) {
        requestDto.filters.push({ type: 'TYPE', value: 'USER' });
    }

    if (document.getElementById('filterSolo').checked) {
        requestDto.filters.push({ type: 'DETAIL_LEVEL', value: 'CONNECTED_ONLY' });
    }

    const packageVal = document.getElementById('filterPackage').value.trim();
    if (packageVal) {
        requestDto.filters.push({ type: 'PACKAGE', value: packageVal });
    }

    const scopeVal = document.getElementById('filterScope').value;
    if (scopeVal !== 'ALL') {
        requestDto.filters.push({ type: 'SCOPE', value: scopeVal });
    }

    const stereoVal = document.getElementById('filterStereotype').value;
    if (stereoVal !== 'ALL') {
        requestDto.filters.push({ type: 'STEREOTYPE', value: stereoVal });
    }

    fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestDto)
    })
        .then(response => {
            if (!response.ok) throw new Error("Server error: " + response.status);
            return response.json();
        })
        .then(data => drawGraph(data))
        .catch(error => {
            console.error("Error loading graph:", error);
            nodesDataSet.clear();
            edgesDataSet.clear();
        });
}

function drawGraph(beanGraph) {
    document.getElementById('details-panel').classList.add('hidden');

    nodesDataSet.clear();
    edgesDataSet.clear();

    if (!beanGraph) return;
    const backendNodes = beanGraph.nodes || [];
    const backendSoloNodes = beanGraph.soloNodes || [];
    const backendEdges = beanGraph.edges || [];

    const allBackendNodes = [...backendNodes, ...backendSoloNodes];

    if (allBackendNodes.length === 0) {
        console.warn("The graph is empty with current filters.");
        return;
    }

    const visNodes = allBackendNodes.map(node => {
        const colors = STEREOTYPE_COLORS[node.stereotype] || STEREOTYPE_COLORS['UNKNOWN'];

        return {
            id: node.id,
            label: node.id,
            title: node.fullClassName,
            shape: 'box',
            color: {
                background: node.isSystem ? '#E0E0E0' : colors.background,
                border: node.isSystem ? '#9E9E9E' : colors.border,
                highlight: { background: '#BBDEFB', border: '#2196F3' }
            },
            font: { color: '#333' },
            borderWidth: node.isSystem ? 1 : 2,
            meta: node
        };
    });

    const visEdges = backendEdges.map(edge => ({
        from: edge.source.id,
        to: edge.target.id,
        label: edge.injectionType,
        arrows: 'to',
        font: { size: 11, align: 'top', color: '#555' },
        color: { color: '#BDBDBD', highlight: '#2196F3' },
        dashes: false
    }));

    nodesDataSet.add(visNodes);
    edgesDataSet.add(visEdges);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function showNodeDetails(meta) {
    const panel = document.getElementById('details-panel');
    const infoDiv = document.getElementById('node-info');

    const scopeClass = meta.scope === 'SINGLETON' ? 'singleton' : 'prototype';
    const typeClass = meta.isSystem ? 'system' : 'user';
    const typeText = meta.isSystem ? 'System (Spring)' : 'User';

    const outgoingEdges = edgesDataSet.get({
        filter: function (edge) {
            return edge.from === meta.id;
        }
    });

    let dependenciesHtml = '';
    if (outgoingEdges.length > 0) {
        dependenciesHtml = '<ul class="dependencies-list">';
        outgoingEdges.forEach(edge => {
            dependenciesHtml += `<li><strong style="color: #0d6efd;">${escapeHtml(edge.to)}</strong> <span style="color: #777; font-size: 0.8rem;">(${escapeHtml(edge.label)})</span></li>`;
        });
        dependenciesHtml += '</ul>';
    } else {
        dependenciesHtml = '<span style="color: #888; font-style: italic;">No dependencies</span>';
    }

    infoDiv.innerHTML = `
        <div class="meta-row">
            <span class="meta-label">Component ID:</span>
            <span class="meta-value">${escapeHtml(meta.id)}</span>
        </div>
        <div class="meta-row">
            <span class="meta-label">Java Class:</span>
            <span class="meta-value" style="font-size: 0.8rem;">${escapeHtml(meta.fullClassName)}</span>
        </div>
        <div class="meta-row">
            <span class="meta-label">Stereotype:</span>
            <span class="tag" style="background: #e2e3e5;">@${escapeHtml(meta.stereotype)}</span>
        </div>
        <div class="meta-row">
            <span class="meta-label">Characteristics:</span>
            <span class="tag ${scopeClass}">${escapeHtml(meta.scope)}</span>
            <span class="tag ${typeClass}">${typeText}</span>
        </div>
        <div class="meta-row" style="margin-top: 15px; border-top: 1px solid #ddd; padding-top: 10px;">
            <span class="meta-label">Depends on:</span>
            ${dependenciesHtml}
        </div>
    `;

    panel.classList.remove('hidden');
}

function bindEvents() {
    document.getElementById('applyFiltersBtn').addEventListener('click', () => {
        loadGraphData();
    });

    document.getElementById('searchBtn').addEventListener('click', () => {
        const query = document.getElementById('searchInput').value.trim();
        if (!query) return;

        const foundNode = nodesDataSet.get({
            filter: function (item) {
                return item.id.toLowerCase().includes(query.toLowerCase());
            }
        });

        if (foundNode.length > 0) {
            const nodeId = foundNode[0].id;
            network.selectNodes([nodeId]);
            network.focus(nodeId, {
                scale: 1.2,
                animation: { duration: 1000, easingFunction: 'easeInOutQuad' }
            });
            showNodeDetails(foundNode[0].meta);
        } else {
            alert('Bean with this name was not found in the current graph!');
        }
    });

    document.getElementById('searchInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') document.getElementById('searchBtn').click();
    });
}