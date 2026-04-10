#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import sys
import zipfile
from datetime import datetime
from pathlib import Path

def zip_out_folder():
    # Ввод пути к папке out (дефолт: ./out)
    default_out = Path("out")
    out_input = input(f"Путь к папке OUT (пусто = {default_out}): ").strip()
    out_dir = Path(out_input) if out_input else default_out
    
    if not out_dir.exists():
        print(f"ОШИБКА: папка '{out_dir}' не найдена!")
        return 1

    # Ввод имени ZIP (дефолт: ebanina-YYYYMMDD_HHMMSS.zip)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    default_zip = f"ebanina-{timestamp}.zip"
    zip_input = input(f"Имя ZIP (пусто = {default_zip}): ").strip()
    zip_name = zip_input if zip_input else default_zip
    
    if zip_name and not zip_name.endswith('.zip'):
        zip_name += '.zip'

    print(f"Архивируем '{out_dir}' → '{zip_name}'...")

    # Создаем ZIP с сохранением структуры
    with zipfile.ZipFile(zip_name, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for root, dirs, files in os.walk(out_dir):
            for file in files:
                file_path = Path(root) / file
                arcname = file_path.relative_to(out_dir.parent)
                zf.write(file_path, arcname)
                print(f"  {arcname}")

    size_mb = os.path.getsize(zip_name) / 1024 / 1024
    print(f"ГОТОВО: {zip_name} ({size_mb:.1f} MB)")
    return 0

if __name__ == "__main__":
    sys.exit(zip_out_folder())