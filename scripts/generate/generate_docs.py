#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт генерации JavaDoc документации для Ebanina Audio Player.
Запускать в корневом каталоге проекта.
"""

import os
import pathlib
import subprocess
import sys


def main():
    current_dir = pathlib.Path.cwd()
    print(f"Текущая директория: {current_dir}")

    # Проверка наличия папки libraries
    libraries_path = current_dir / "libraries"
    if not libraries_path.exists():
        print(f"Папка libraries не найдена в {current_dir}.")
        new_dir = input("Пожалуйста, введите путь к директории, которая будет использована вместо CURRENT_DIR: ").strip()
        current_dir = pathlib.Path(new_dir).resolve()
        libraries_path = current_dir / "libraries"
        print(f"Используем директорию: {current_dir}")

    if not libraries_path.exists():
        print(f"Ошибка: папка libraries не найдена по пути {libraries_path}")
        sys.exit(1)

    # Путь к javadoc (адаптируется под систему)
    jdk_path = r"C:\Program Files\Java\jdk-21\bin\javadoc.exe"
    if not os.path.exists(jdk_path):
        # Попытка найти javadoc в PATH
        try:
            result = subprocess.run(["javadoc", "-version"], capture_output=True, text=True)
            if result.returncode == 0:
                print("Используем javadoc из PATH")
                jdk_path = "javadoc"
            else:
                print("javadoc.exe не найден. Укажите путь вручную:")
                jdk_path = input("Путь к javadoc.exe: ").strip()
        except FileNotFoundError:
            print("javadoc не найден в PATH")
            jdk_path = input("Путь к javadoc.exe: ").strip()

    # Пути для аргументов
    module_path = str(current_dir / "libraries" / "modules")
    docs_output = str(current_dir / "java-docs")
    source_path = str(current_dir / "src")

    # Classpath - собираем все .jar файлы
    classes_path = current_dir / "libraries" / "classes"
    jar_files = [
        "vst3host.jar",
        "vorbisspi-1.0.3.3.jar",
        "tritonus-share-0.3.7.4.jar",
        "mp3spi-1.9.5.4.jar",
        "lastfm-java-0.1.2.jar",
        "JVstHost.jar",
        "jlayer-1.0.1.4.jar",
        "ebanina-mod-api.jar",
        "ebanina-audio-player.jar"
    ]

    classpath_parts = [str(classes_path)]
    for jar in jar_files:
        jar_path = classes_path / jar
        if jar_path.exists():
            classpath_parts.append(str(jar_path))
        else:
            print(f"Предупреждение: {jar_path} не найден")

    classpath = os.pathsep.join(classpath_parts)

    print(f"Classpath: {len(classpath_parts)} элементов")

    # Команда javadoc
    cmd = [
        jdk_path,
        "--module-path", module_path,
        "--add-modules",
        "javafx.controls,javafx.fxml,javafx.graphics,javafx.swing,javafx.media,"
        "org.controlsfx.controls,jdk.management,java.management,mp3agic,java.prefs,"
        "com.jfoenix,com.github.kwhat.jnativehook,jsoup,lastfm.java,java.desktop,"
        "com.sun.jna,com.sun.jna.platform,org.apache.commons.io,untitled10",
        "-classpath", classpath,
        "-d", docs_output,
        "-sourcepath", source_path,
        "-encoding", "UTF-8",
        "-docencoding", "UTF-8",
        "-charset", "UTF-8",
        "--add-opens", "java.base/java.lang.reflect=com.jfoenix",
        "--add-opens", "javafx.graphics/javafx.stage=ALL-UNNAMED",
        "--add-opens", "javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED",
        "-subpackages", "rf"
    ]

    print("Запуск javadoc...")
    print("Команда:", " ".join(cmd[:5]) + " ...")  # сокращаем вывод

    try:
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=current_dir)

        if result.returncode == 0:
            print("JavaDoc успешно сгенерирован в папку 'java-docs'")
        else:
            print("Ошибка генерации JavaDoc:")
            print(result.stderr)

    except FileNotFoundError:
        print(f"javadoc не найден по пути: {jdk_path}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nПрервано пользователем")
        sys.exit(1)

if __name__ == "__main__":
    main()
