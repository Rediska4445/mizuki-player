#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import sys
import zipfile
from datetime import datetime
from pathlib import Path


def zip_out_folder():
    out_dir = Path("out")
    if not out_dir.exists():
        print("ОШИБКА: папка 'out' не найдена!")
        return 1

    # Имя архива с датой/версией
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    zip_name = f"ebanina-{timestamp}.zip"

    print(f"Архивируем '{out_dir}' → '{zip_name}'...")

    # Создаем ZIP с сохранением структуры
    with zipfile.ZipFile(zip_name, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for root, dirs, files in os.walk(out_dir):
            for file in files:
                file_path = Path(root) / file
                arcname = file_path.relative_to(out_dir.parent)
                zf.write(file_path, arcname)
                print(f"  📄 {arcname}")

    print(f"ГОТОВО: {zip_name} ({os.path.getsize(zip_name)/1024/1024:.1f} MB)")
    return 0

if __name__ == "__main__":
    sys.exit(zip_out_folder())
