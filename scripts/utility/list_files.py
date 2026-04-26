#!/usr/bin/env python3
"""
Простой скрипт выводит структуру директории.
Сначала спрашивает путь к проекту.
"""

import os
import sys
from pathlib import Path


def is_interesting_file(path: Path) -> bool:
    """
    Показывать ли этот файл в списке.
    """
    name = path.name

    if name.startswith('.'):
        return False
    if name in {'target', 'node_modules', 'venv', '__pycache__'}:
        return False
    if path.suffix in {'.class', '.jar', '.war', '.log'}:
        return False

    return True


def is_interesting_dir(path: Path) -> bool:
    """
    Показывать ли эту директорию.
    """
    name = path.name

    if name.startswith('.'):
        return False
    if name in {'target', 'node_modules', 'venv'}:
        return False

    return True


def walk_tree(path: Path, prefix: str = '', is_last: bool = True):
    """
    Рекурсивный вывод дерева.
    """
    if not path.exists():
        print(f"[!] Путь не существует: {path}")
        return

    if path.is_file():
        print(prefix + f"{'└─ ' if is_last else '├─ '}{path.name}")
        return

    # Это папка
    print(prefix + f"{'└─ ' if is_last else '├─ '}{path.name}/")

    if not is_interesting_dir(path):
        return

    children = sorted(
        [p for p in path.iterdir()],
        key=lambda x: (not x.is_dir(), x.name.lower())
    )

    children = [p for p in children if is_interesting_file(p) or is_interesting_dir(p)]

    for i, child in enumerate(children):
        is_last_child = (i == len(children) - 1)
        connector = '  ' if is_last else '│ '
        walk_tree(child, prefix + connector, is_last_child)


def main():
    while True:
        root_str = input("Введите путь к корню проекта (например C:\\Users\\user\\Desktop\\liama): ").strip()

        if not root_str:
            continue

        root = Path(root_str).resolve()

        if not root.exists():
            print(f"[!] Путь не существует: {root}")
            continue

        print(f"\nПроект: {root.name}")
        print("─" * 60)
        walk_tree(root, '', True)
        break


if __name__ == "__main__":
    main()
    input("\nНажмите Enter, чтобы закрыть...")