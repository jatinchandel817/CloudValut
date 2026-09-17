/* ============================================================
   Distributed File Storage – Frontend JavaScript
   ============================================================ */

const API_BASE = '/api';

// ===== Navigation =====
function showSection(sectionId) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.add('d-none'));
    document.getElementById(sectionId).classList.remove('d-none');

    document.querySelectorAll('.navbar .nav-link').forEach(link => link.classList.remove('active'));
    const links = document.querySelectorAll('.navbar .nav-link');
    links.forEach(link => {
        if (link.getAttribute('onclick') && link.getAttribute('onclick').includes(sectionId)) {
            link.classList.add('active');
        }
    });

    if (sectionId === 'dashboard') loadDashboard();
    if (sectionId === 'files') loadFiles();
    if (sectionId === 'nodes') loadNodes();
}

// ===== Toast Helper =====
function showToast(title, message, type = 'info') {
    const toastEl = document.getElementById('toast');
    document.getElementById('toastTitle').textContent = title;
    document.getElementById('toastBody').textContent = message;
    toastEl.className = 'toast';
    if (type === 'success') toastEl.classList.add('text-bg-success');
    else if (type === 'error') toastEl.classList.add('text-bg-danger');
    else if (type === 'warning') toastEl.classList.add('text-bg-warning');
    const toast = bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 4000 });
    toast.show();
}

// ===== File Size Formatter =====
function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

// ===== Date Formatter =====
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ===== Storage Badge =====
function storageBadge(type) {
    const cls = type === 'LOCAL' ? 'badge-storage-local'
              : type === 'S3' ? 'badge-storage-s3'
              : 'badge-storage-gcs';
    return `<span class="badge ${cls}">${type}</span>`;
}

// ===== Status Badge =====
function statusBadge(status) {
    const cls = status === 'ACTIVE' ? 'badge-status-active'
              : status === 'DELETED' ? 'badge-status-deleted'
              : status === 'REPLICATED' ? 'badge-status-replicated'
              : 'badge-status-failed';
    return `<span class="badge ${cls}">${status}</span>`;
}

// ===== Dashboard =====
async function loadDashboard() {
    try {
        const res = await fetch(`${API_BASE}/files/status`);
        const data = await res.json();

        document.getElementById('totalFiles').textContent = data.totalFiles || 0;
        document.getElementById('totalStorage').textContent = formatSize(data.totalStorageUsed);
        document.getElementById('localFiles').textContent = (data.localFiles || 0) + ' files';
        document.getElementById('localStorage').textContent = formatSize(data.localStorageUsed);
        document.getElementById('s3Files').textContent = (data.s3Files || 0) + ' files';
        document.getElementById('s3Storage').textContent = formatSize(data.s3StorageUsed);
        document.getElementById('gcsFiles').textContent = (data.gcsFiles || 0) + ' files';
        document.getElementById('gcsStorage').textContent = formatSize(data.gcsStorageUsed);
    } catch (err) {
        showToast('Error', 'Failed to load dashboard data', 'error');
    }
}

// ===== Files Table =====
let allFiles = [];

async function loadFiles() {
    try {
        const res = await fetch(`${API_BASE}/files`);
        allFiles = await res.json();
        renderFiles(allFiles);
    } catch (err) {
        document.getElementById('filesTableBody').innerHTML =
            '<tr><td colspan="7" class="text-center text-danger py-4">Failed to load files</td></tr>';
    }
}

function renderFiles(files) {
    const tbody = document.getElementById('filesTableBody');
    if (!files || files.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No files found. Upload a file to get started.</td></tr>';
        return;
    }

    tbody.innerHTML = files.map(f => `
        <tr>
            <td class="fw-medium">${escapeHtml(f.originalName)}</td>
            <td><small class="text-muted">${escapeHtml(f.fileType || 'unknown')}</small></td>
            <td>${formatSize(f.fileSize)}</td>
            <td>${storageBadge(f.storageType)}</td>
            <td><small class="text-muted">${formatDate(f.uploadedAt)}</small></td>
            <td>${statusBadge(f.status)}</td>
            <td class="text-end">
                <a href="${API_BASE}/files/download/${f.id}" class="btn btn-sm btn-outline-primary me-1" title="Download">
                    <i class="bi bi-download"></i>
                </a>
                <button class="btn btn-sm btn-outline-danger" title="Delete"
                    onclick="confirmDelete('${f.id}', '${escapeHtml(f.originalName)}')">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ===== Search =====
function searchFiles() {
    const query = document.getElementById('searchInput').value.trim();
    if (!query) {
        renderFiles(allFiles);
        return;
    }
    fetch(`${API_BASE}/files/search?name=${encodeURIComponent(query)}`)
        .then(res => res.json())
        .then(data => renderFiles(data))
        .catch(() => showToast('Error', 'Search failed', 'error'));
}

// ===== Delete =====
let pendingDeleteId = null;

function confirmDelete(fileId, fileName) {
    pendingDeleteId = fileId;
    document.getElementById('deleteFileName').textContent = fileName;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}

document.getElementById('confirmDeleteBtn').addEventListener('click', async () => {
    if (!pendingDeleteId) return;
    try {
        const res = await fetch(`${API_BASE}/files/${pendingDeleteId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Success', 'File deleted successfully', 'success');
            loadFiles();
            loadDashboard();
        } else {
            const err = await res.json();
            showToast('Error', err.message || 'Delete failed', 'error');
        }
    } catch (err) {
        showToast('Error', 'Delete failed', 'error');
    }
    bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
    pendingDeleteId = null;
});

// ===== Upload (with XMLHttpRequest for progress) =====
const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');

dropZone.addEventListener('click', () => fileInput.click());

dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
        uploadFile(e.dataTransfer.files[0]);
    }
});

function handleFileSelect(input) {
    if (input.files.length > 0) {
        uploadFile(input.files[0]);
    }
}

function uploadFile(file) {
    const progressDiv = document.getElementById('uploadProgress');
    const progressBar = document.getElementById('progressBar');
    const percentLabel = document.getElementById('uploadPercent');
    const statusLabel = document.getElementById('uploadStatus');
    const fileNameLabel = document.getElementById('uploadFileName');
    const resultDiv = document.getElementById('uploadResult');

    progressDiv.classList.remove('d-none');
    resultDiv.innerHTML = '';
    fileNameLabel.textContent = file.name + ' (' + formatSize(file.size) + ')';
    statusLabel.textContent = 'Uploading...';
    progressBar.style.width = '0%';
    percentLabel.textContent = '0%';

    const formData = new FormData();
    formData.append('file', file);

    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100);
            progressBar.style.width = percent + '%';
            percentLabel.textContent = percent + '%';
            if (percent < 100) {
                statusLabel.textContent = 'Uploading... ' + percent + '%';
            }
        }
    });

    xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
            const data = JSON.parse(xhr.responseText);
            progressBar.style.width = '100%';
            percentLabel.textContent = '100%';
            progressBar.classList.remove('progress-bar-animated');
            statusLabel.textContent = 'Upload complete!';
            resultDiv.innerHTML = `
                <div class="alert alert-success upload-success mt-3">
                    <i class="bi bi-check-circle-fill me-2"></i>
                    <strong>File uploaded successfully!</strong><br>
                    <small>Storage: ${data.storageType} | Node: ${data.storageNode || 'N/A'}</small><br>
                    <small>Replication: ${data.replicationStatus || 'N/A'}</small><br>
                    <small>Checksum: ${data.checksum ? data.checksum.substring(0, 16) + '...' : 'N/A'}</small>
                </div>`;
            showToast('Success', 'File uploaded to ' + data.storageType, 'success');
        } else {
            let msg = 'Upload failed';
            try { msg = JSON.parse(xhr.responseText).message || msg; } catch (e) {}
            statusLabel.textContent = 'Upload failed';
            resultDiv.innerHTML = `<div class="alert alert-danger mt-3"><i class="bi bi-x-circle-fill me-2"></i>${escapeHtml(msg)}</div>`;
            showToast('Error', msg, 'error');
        }
    });

    xhr.addEventListener('error', () => {
        statusLabel.textContent = 'Upload failed (network error)';
        resultDiv.innerHTML = `<div class="alert alert-danger mt-3"><i class="bi bi-x-circle-fill me-2"></i>Network error during upload</div>`;
        showToast('Error', 'Network error during upload', 'error');
    });

    xhr.open('POST', `${API_BASE}/files/upload`);
    xhr.send(formData);
}

// ===== Storage Nodes =====
async function loadNodes() {
    try {
        const res = await fetch(`${API_BASE}/storage/nodes`);
        const nodes = await res.json();
        renderNodes(nodes);
    } catch (err) {
        document.getElementById('nodesContainer').innerHTML =
            '<div class="col-12 text-center text-danger py-4">Failed to load storage nodes</div>';
    }
}

function renderNodes(nodes) {
    const container = document.getElementById('nodesContainer');
    if (!nodes || nodes.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted py-4">No storage nodes configured.</div>';
        return;
    }

    container.innerHTML = nodes.map(n => {
        const icon = n.storageType === 'LOCAL' ? 'bi-pc-display text-success'
                   : n.storageType === 'S3' ? 'bi-cloud text-warning'
                   : 'bi-clouds text-danger';
        const statusCls = n.status === 'ACTIVE' ? 'badge-node-active'
                        : n.status === 'INACTIVE' ? 'badge-node-inactive'
                        : 'badge-node-offline';
        const avail = n.availableSpace != null ? formatSize(n.availableSpace) : 'Unlimited';
        const used = formatSize(n.usedSpace);
        const usagePct = n.availableSpace != null && n.availableSpace > 0
            ? Math.min(100, (n.usedSpace / (n.usedSpace + n.availableSpace)) * 100)
            : 0;

        return `
            <div class="col-md-4">
                <div class="card node-card h-100">
                    <div class="card-body text-center">
                        <i class="bi ${icon} node-icon mb-3"></i>
                        <h5 class="card-title">${escapeHtml(n.nodeName)}</h5>
                        <p class="text-muted small">${escapeHtml(n.endpoint || '')}</p>
                        <span class="badge ${statusCls} mb-3">${n.status}</span>
                        <div class="text-start mt-3">
                            <div class="d-flex justify-content-between mb-1">
                                <small class="text-muted">Type</small>
                                <small class="fw-medium">${n.storageType}</small>
                            </div>
                            <div class="d-flex justify-content-between mb-1">
                                <small class="text-muted">Used</small>
                                <small class="fw-medium">${used}</small>
                            </div>
                            <div class="d-flex justify-content-between mb-2">
                                <small class="text-muted">Available</small>
                                <small class="fw-medium">${avail}</small>
                            </div>
                            <div class="progress" style="height: 6px;">
                                <div class="progress-bar" style="width: ${usagePct}%"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
    }).join('');
}

// ===== Initialize on page load =====
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();
});
