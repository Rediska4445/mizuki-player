#!/usr/bin/env python3
import os
from pathlib import Path

def count_lines_in_directory(dir_path):
    """Подсчитывает количество строк в файлах указанной директории по расширениям."""

    # Проверяем существование директории
    if not os.path.exists(dir_path):
        print(f"Папка не найдена: {dir_path}")
        input("Нажмите Enter для выхода...")
        return

    # Расширения файлов для подсчета (аналогично batch)
    extensions = ['.java', '.cpp', '.css', '.js', '.html', '.xml', '.json', '.fxml']

    print(f"Подсчет строк в: {dir_path}\n")

    # Цикл по расширениям
    for ext in extensions:
        total_lines = 0

        # Рекурсивный поиск всех файлов с данным расширением
        for file_path in Path(dir_path).rglob(f"*{ext}"):
            try:
                with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = len(f.readlines())
                    total_lines += lines
            except Exception:
                # Пропускаем нечитаемые файлы
                continue

        print(f"Строк в *{ext}: {total_lines:,}".replace(',', ' '))

    print("\nГотово!")
    input("Нажмите Enter для выхода...")

if __name__ == "__main__":
    # Запрос пути у пользователя
    dir_path = input("Введите путь к директории для подсчета строк: ").strip()

    # Нормализуем путь
    dir_path = os.path.abspath(dir_path)

    count_lines_in_directory(dir_path)
