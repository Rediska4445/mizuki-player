#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import requests
import sys
from pathlib import Path


def download_latest_mizuki_release():
    """Скачивает последний релиз Mizuki Player с GitHub"""

    # GitHub API для релизов
    repo_owner = "Rediska4445"
    repo_name = "mizuki-player"
    api_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/releases/latest"

    print("🔍 Получаем информацию о последнем релизе...")

    try:
        response = requests.get(api_url, timeout=10)
        response.raise_for_status()
        release_data = response.json()
    except requests.RequestException as e:
        print(f"ОШИБКА API запроса: {e}")
        return 1

    # Информация о релизе
    tag_name = release_data['tag_name']
    release_name = release_data['name'] or tag_name
    published_at = release_data['published_at'][:10]  # YYYY-MM-DD

    print(f" Найден релиз: {release_name} ({tag_name})")
    print(f" Дата: {published_at}")

    # Поиск ZIP архива (предпочтительно .zip или .7z)
    assets = release_data['assets']
    zip_asset = None

    # Приоритет: .zip → .7z → .exe → первый файл
    for asset in assets:
        asset_name = asset['name'].lower()
        if asset_name.endswith('.zip'):
            zip_asset = asset
            break
        elif asset_name.endswith('.7z') and not zip_asset:
            zip_asset = asset
        elif asset_name.endswith('.exe') and not zip_asset:
            zip_asset = asset

    if not zip_asset:
        print("ZIP архив не найден в активах релиза!")
        print("Доступные файлы:")
        for asset in assets:
            print(f"  - {asset['name']} ({asset['browser_download_url']})")
        return 1

    download_url = zip_asset['browser_download_url']
    filename = zip_asset['name']

    print(f"⬇ Скачиваем: {filename}")

    # Скачивание
    try:
        response = requests.get(download_url, stream=True, timeout=30)
        response.raise_for_status()

        total_size = int(response.headers.get('content-length', 0))
        downloaded = 0

        # Создаем папку downloads если нет
        Path("downloads").mkdir(exist_ok=True)
        filepath = Path("downloads") / filename

        with open(filepath, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"\r  Прогресс: {percent:.1f}% ({downloaded/1024/1024:.1f}/{total_size/1024/1024:.1f} MB)", end='')

        print(f"\nСкачано: {filepath}")
        print(f"   Размер: {filepath.stat().st_size/1024/1024:.1f} MB")

        # Создаем .txt с информацией
        info_file = filepath.with_suffix('.txt')
        with open(info_file, 'w', encoding='utf-8') as f:
            f.write(f"Mizuki Player - Последний релиз\n")
            f.write(f"===============================\n\n")
            f.write(f"Репозиторий: https://github.com/{repo_owner}/{repo_name}\n")
            f.write(f"Релиз: {release_name} ({tag_name})\n")
            f.write(f"Дата: {published_at}\n")
            f.write(f"Ссылка: {release_data['html_url']}\n")
            f.write(f"Файл: {filename}\n")
            f.write(f"Размер: {filepath.stat().st_size/1024/1024:.1f} MB\n")

        print(f"Информация: {info_file}")

    except requests.RequestException as e:
        print(f"\nОШИБКА скачивания: {e}")
        return 1

    return 0

if __name__ == "__main__":
    if not os.name == 'posix' or sys.platform != 'darwin':
        print("Требуется: requests")
        print("Установка: pip install requests")

    sys.exit(download_latest_mizuki_release())
