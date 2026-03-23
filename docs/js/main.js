/**
 * Mizuka Docs — SIMPLEST LOGIC
 * ЛОГИКА: Куда нажал мышкой — ТО ЧИСТО ВЫДЕЛИЛОСЬ. Никакой магии.
 */

const contentArea = document.getElementById('content-area');
const treeNav = document.querySelector('.tree-nav');
const buttons = document.querySelectorAll('.sidebar-btn');

// 1. ГЛОБАЛЬНАЯ ФУНКЦИЯ: Сброс всего и выделение ОДНОГО элемента
function forceHighlight(element) {
    // А. СБРОС ВСЕХ
    buttons.forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tree-node, .tree-link').forEach(el => el.classList.remove('active'));

    // Б. Если это кнопка
    if (element.classList.contains('sidebar-btn')) {
        element.classList.add('active');
        return;
    }

    // В. Если это узел дерева (.tree-node)
    if (element.classList.contains('tree-node')) {
        element.classList.add('active');
        const link = element.querySelector('.tree-link');
        if (link) link.classList.add('active');

        // Г. Развернуть родителей (только визуально, без выделения!)
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

// 2. Загрузка страницы (БОЛЬШЕ НИЧЕГО НЕ ВЫДЕЛЯЕТ!)
async function loadPage(pageUrl) {
    try {
        contentArea.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#7092be;">Загрузка...</div>';
        const response = await fetch(pageUrl);
        if (!response.ok) throw new Error('Error: ' + pageUrl);
        const html = await response.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        contentArea.innerHTML = doc.querySelector('main')?.innerHTML || doc.body.innerHTML;

        history.pushState({ page: pageUrl }, '', `?page=${encodeURIComponent(pageUrl)}`);
        contentArea.scrollTop = 0;

        // ❌ ЗДЕСЬ НИЧЕГО НЕТ! Никакого выделения!
    } catch (err) {
        contentArea.innerHTML = `<div style="color:#ff6b6b;padding:20px;"><h2>Ошибка</h2><p>${err.message}</p></div>`;
    }
}

// 3. КЛИК ПО ДЕРЕВУ (ТЗ: Куда нажал — то выделилось)
treeNav?.addEventListener('click', (e) => {
    const target = e.target;
    const linkEl = target.closest('.tree-link');
    const nodeEl = target.closest('.tree-node');

    if (!nodeEl) return; // Клик мимо
    e.preventDefault();

    // 1. СРАЗУ ВЫДЕЛЯЕМ ЭТОТ УЗЕЛ (первым делом!)
    forceHighlight(nodeEl);

    // 2. ПРОВЕРЯЕМ, что это за узел
    if (linkEl && linkEl.getAttribute('data-page')) {
        const pageUrl = linkEl.getAttribute('data-page');

        if (pageUrl.includes('#')) {
            const [url, anchor] = pageUrl.split('#');
            loadPage(url).then(() => {
                const el = document.getElementById(anchor);
                if (el) setTimeout(() => el.scrollIntoView({behavior: 'smooth'}), 150);
            });
        } else {
            loadPage(pageUrl);
        }
    } else {
        // Если это просто родитель (без data-page) — просто разворачиваем
        toggleTreeNode(nodeEl);
    }
});

// 4. КЛИК ПО КНОПКАМ
buttons.forEach(btn => {
    btn.addEventListener('click', () => {
        // 1. СРАЗУ ВЫДЕЛЯЕМ КНОПКУ
        forceHighlight(btn);

        // 2. Загружаем
        const page = btn.getAttribute('data-page');
        if (page) loadPage(page);

        // 3. Сбрасываем дерево (на всякий случай, хотя forceHighlight уже сбросил)
        document.querySelectorAll('.tree-node, .tree-link').forEach(el => el.classList.remove('active'));
    });
});

// 5. Развернуть/свернуть
function toggleTreeNode(node) {
    const ul = node.nextElementSibling;
    if (ul && !ul.classList.contains('tree-nav')) {
        ul.classList.toggle('open');
        node.classList.toggle('expanded');
    }
}

// 6. Кнопки браузера (Назад/Вперёд) — единственное место, где ищем что выделить
window.addEventListener('popstate', (e) => {
    if (e.state?.page) {
        const pageUrl = e.state.page;
        loadPage(pageUrl);

        // Ищем, что выделить (так как клик не был)
        let targetLink = document.querySelector(`.tree-link[data-page="${pageUrl}"]`);
        if (!targetLink && pageUrl.includes('#')) {
            targetLink = document.querySelector(`.tree-link[data-page="${pageUrl.split('#')[0]}"]`);
        }
        if (targetLink) {
            forceHighlight(targetLink.closest('.tree-node'));
        } else {
            const btn = document.querySelector(`.sidebar-btn[data-page="${pageUrl}"]`);
            if (btn) forceHighlight(btn);
        }
    } else {
        loadPage('pages/home.html');
        // По умолчанию выделяем кнопку "О проекте" если есть, или первый узел
        const homeBtn = document.querySelector('.sidebar-btn[data-page="pages/home.html"]');
        if (homeBtn) forceHighlight(homeBtn);
    }
});

// 7. СТАРТ
document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const page = params.get('page');
    if (page) {
        loadPage(decodeURIComponent(page));
        // Пытаемся найти, что выделить
        let targetLink = document.querySelector(`.tree-link[data-page="${decodeURIComponent(page)}"]`);
        if (!targetLink && page.includes('#')) {
            targetLink = document.querySelector(`.tree-link[data-page="${page.split('#')[0]}"]`);
        }
        if (targetLink) {
            forceHighlight(targetLink.closest('.tree-node'));
        } else {
            const btn = document.querySelector(`.sidebar-btn[data-page="${decodeURIComponent(page)}"]`);
            if (btn) forceHighlight(btn);
        }
    } else {
        loadPage('pages/home.html');
        const homeBtn = document.querySelector('.sidebar-btn[data-page="pages/home.html"]');
        if (homeBtn) forceHighlight(homeBtn);
    }
});
