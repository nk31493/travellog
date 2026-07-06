function loadNotifications() {
    fetch('/api/notifications/unread-count')
        .then(r => r.json())
        .then(data => {
            const badge = document.getElementById('notiCount');
            if (!badge) return;
            if (data.count > 0) {
                badge.textContent = data.count > 9 ? '9+' : data.count;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        });
}

function toggleNotifications() {
    const dropdown = document.getElementById('notiDropdown');
    if (!dropdown) return;
    if (dropdown.style.display === 'none') {
        dropdown.style.display = 'block';
        fetch('/api/notifications')
            .then(r => r.json())
            .then(notifications => {
                if (notifications.length === 0) {
                    document.getElementById('notiList').innerHTML =
                        '<div style="text-align:center;color:#B0B8C1;padding:24px;font-size:13px;">알림이 없어요</div>';
                    return;
                }
                document.getElementById('notiList').innerHTML = notifications.map(n => `
                    <a href="${n.link}" onclick="markAllRead()" style="text-decoration:none;">
                        <div style="padding:14px 20px;border-bottom:1px solid var(--border-light, #F2F4F6);background:${n.isRead ? 'var(--bg-card, white)' : 'var(--bg-subtle, #F8FAFF)'};">
                            <div style="font-size:13px;color:var(--text-primary, #191F28);margin-bottom:4px;">${n.message}</div>
                            <div style="font-size:11px;color:var(--text-muted, #B0B8C1);">${new Date(n.createdAt).toLocaleDateString('ko-KR')}</div>
                        </div>
                    </a>
                `).join('');
            });
    } else {
        dropdown.style.display = 'none';
    }
}

function markAllRead() {
    fetch('/api/notifications/read-all', { method: 'PUT' })
        .then(() => {
            const badge = document.getElementById('notiCount');
            const dropdown = document.getElementById('notiDropdown');
            if (badge) badge.style.display = 'none';
            if (dropdown) dropdown.style.display = 'none';
        });
}

// 드롭다운 외부 클릭 시 닫기
document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('notiDropdown');
    const btn = document.getElementById('notiBtn');
    if (dropdown && !dropdown.contains(e.target) && btn && e.target !== btn) {
        dropdown.style.display = 'none';
    }
});

// 페이지 로드 시 알림 개수 확인
document.addEventListener('DOMContentLoaded', loadNotifications);