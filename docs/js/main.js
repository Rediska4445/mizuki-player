/**
 * Mizuka Docs — ЛОКАЛЬНЫЙ ЛОГИК
 * Куда нажал — то и показалось. Никаких `fetch`, только `display`.
 */

const contentArea = document.getElementById('content-area');
const treeNav = document.querySelector('.tree-nav');
const buttons = document.querySelectorAll('.sidebar-btn');

// 1. Сброс выделения и выделение ОДНОГО элемента
function forceHighlight(element) {
    // сброс всех кнопок
    buttons.forEach(b => b.classList.remove('active'));
    // сброс всех узлов дерева
    document.querySelectorAll('.tree-node, .tree-link').forEach(el => el.classList.remove('active'));

    if (element.classList.contains('sidebar-btn')) {
        element.classList.add('active');
        return;
    }

    if (element.classList.contains('tree-node')) {
        element.classList.add('active');
        const link = element.querySelector('.tree-link');
        if (link) link.classList.add('active');

        // развернуть родителей (визуально)
        let parentUl = element.parentElement;
        while (parentUl && parentUl.classList.contains('tree-nav') === false) {
            parentUl.classList.add('open');
            const prev = parentUl.previousElementSibling;
            if (prev && prev.classList.contains('tree-node')) {
                prev.classList.add('expanded');
            }
            parentUl = parentUl.parentElement.closest('ul');
        }
    }
}

// 2. Показ страницы по ID
function showPage(id) {
    // сначала показываем "Загрузка", чтобы не было мигания
    hideAllPages();
    document.getElementById('loading').style.display = 'flex';

    setTimeout(() => {
        hideAllPages();
        const target = document.getElementById(id);
        if (target) {
            target.style.display = 'block';
            history.pushState({ page: id }, '', `?page=${encodeURIComponent(id)}`);
            contentArea.scrollTop = 0;
        } else {
            showError(id);
        }
        document.getElementById('loading').style.display = 'none';
    }, 10);
}

function hideAllPages() {
    document.querySelectorAll('.page-content').forEach(el => el.style.display = 'none');
}

function showError(id) {
    contentArea.innerHTML = `
        <div style="color:#ff6b6b;padding:20px;">
            <h2>Ошибка</h2>
            <p>Страница не найдена: ${id}</p>
        </div>
    `;
}

// 3. Клик по дереву (ТЗ: куда нажал — то показалось)
treeNav?.addEventListener('click', (e) => {
    const target = e.target;
    const linkEl = target.closest('.tree-link');
    const nodeEl = target.closest('.tree-node');

    if (!nodeEl) return;
    e.preventDefault();

    forceHighlight(nodeEl);

    if (linkEl && linkEl.getAttribute('data-page')) {
        const pageId = linkEl.getAttribute('data-page');
        if (pageId.includes('#')) {
            const [id, anchor] = pageId.split('#');
            showPage(id);
            setTimeout(() => {
                const el = document.getElementById(anchor);
                if (el) el.scrollIntoView({ behavior: 'smooth' });
            }, 150);
        } else {
            showPage(pageId);
        }
    } else {
        // если есть data-page у самого узла
        const pageId = nodeEl.getAttribute('data-page');
        if (pageId) showPage(pageId);
        else toggleTreeNode(nodeEl);
    }
});

// 4. Клик по кнопкам слева
buttons.forEach(btn => {
    btn.addEventListener('click', () => {
        forceHighlight(btn);

        const pageId = btn.getAttribute('data-page');
        if (pageId) showPage(pageId);

        // сбрасываем дерево (на всякий)
        document.querySelectorAll('.tree-node, .tree-link').forEach(el => el.classList.remove('active'));
    });
});

// 5. Развернуть/свернуть узел дерева
function toggleTreeNode(node) {
    const ul = node.nextElementSibling;
    if (ul && !ul.classList.contains('tree-nav')) {
        ul.classList.toggle('open');
        node.classList.toggle('expanded');
    }
}

// 6. Кнопки браузера (Назад/Вперёд)
window.addEventListener('popstate', (e) => {
    if (e.state?.page) {
        const pageId = e.state.page;
        showPage(pageId);

        let targetLink = document.querySelector(`.tree-link[data-page="${pageId}"]`);
        if (!targetLink && pageId.includes('#')) {
            targetLink = document.querySelector(`.tree-link[data-page="${pageId.split('#')[0]}"]`);
        }
        if (targetLink) {
            forceHighlight(targetLink.closest('.tree-node'));
        } else {
            const btn = document.querySelector(`.sidebar-btn[data-page="${pageId}"]`);
            if (btn) forceHighlight(btn);
        }
    } else {
        showPage('home');
        const homeBtn = document.querySelector('.sidebar-btn[data-page="home"]');
        if (homeBtn) forceHighlight(homeBtn);
    }
});

// 7. Запуск при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const page = params.get('page');
    if (page) {
        const id = decodeURIComponent(page);
        showPage(id);

        let targetLink = document.querySelector(`.tree-link[data-page="${id}"]`);
        if (!targetLink && id.includes('#')) {
            targetLink = document.querySelector(`.tree-link[data-page="${id.split('#')[0]}"]`);
        }
        if (targetLink) {
            forceHighlight(targetLink.closest('.tree-node'));
        } else {
            const btn = document.querySelector(`.sidebar-btn[data-page="${id}"]`);
            if (btn) forceHighlight(btn);
        }
    } else {
        showPage('home');
        const homeBtn = document.querySelector('.sidebar-btn[data-page="home"]');
        if (homeBtn) forceHighlight(homeBtn);
    }
});