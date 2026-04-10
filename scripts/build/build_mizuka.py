#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


def run_command(cmd, cwd=None):
    """Windows-совместимый запуск команд"""
    if os.name == 'nt':
        full_cmd = f'chcp 65001 >nul && {" ".join(cmd)}'
        return subprocess.run(full_cmd, cwd=cwd, shell=True,
                              capture_output=True, text=True,
                              encoding='utf-8', errors='replace').returncode == 0
    else:
        return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True).returncode == 0


def clean_cache_selective(cache_src, cache_dst):
    """Очистка cache: оставить файлы только в корне cache/ и cache/cache/, удалить всё остальное"""
    print("Очистка cache (оставляем корень + cache/cache/)...")

    for root, dirs, _ in os.walk(cache_src):
        rel_path = os.path.relpath(root, cache_src)
        target_dir = os.path.join(cache_dst, rel_path)
        Path(target_dir).mkdir(parents=True, exist_ok=True)

    allowed_roots = {".", "cache"}  # корень и cache/cache/

    for root, _, files in os.walk(cache_src):
        rel_root = os.path.relpath(root, cache_src)
        if rel_root in allowed_roots:
            for file in files:
                src_file = os.path.join(root, file)
                rel_path = os.path.relpath(src_file, cache_src)
                dst_file = os.path.join(cache_dst, rel_path)
                shutil.copy2(src_file, dst_file)
                print(f"  Копирован: {rel_path}")
        else:
            print(f"  ПРОПУЩЕНА подпапка: {rel_root}")

    print("Cache очищен!")


def read_version_serial(rootdir):
    """Читает version.serial из version.json -> version.serial"""
    version_file = os.path.join(rootdir, "version.json")
    if not os.path.exists(version_file):
        print(f"ОШИБКА: version.json не найден: {version_file}")
        return None

    try:
        with open(version_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
            version_info = data.get("version", {})
            version_serial = version_info.get("serial")
            if version_serial is None:
                print("ОШИБКА: поле 'version.serial' не найдено в version.json")
                print("Структура должна быть: {'version': {'serial': '1.4.10'}}")
                return None
            return str(version_serial)
    except (json.JSONDecodeError, KeyError) as e:
        print(f"ОШИБКА чтения version.json: {e}")
        return None


def main():
    print("=" * 47)
    print(" Mizuka build script (Java 21)")
    print("=" * 47)
    print()

    rootdir = input("ROOTDIR (пусто = текущая папка): ").strip()
    if not rootdir:
        rootdir = os.getcwd()

    version_serial = read_version_serial(rootdir)
    if version_serial is None:
        input("Нажмите Enter...")
        return 1

    print(f"Используется версия: {version_serial}")

    manifest_file = input("MANIFEST_FILE (пусто = src/META-INF/MANIFEST.MF): ").strip()
    if not manifest_file:
        manifest_file = os.path.join(rootdir, "src", "META-INF", "MANIFEST.MF")

    if not os.path.exists(manifest_file):
        print(f"Файл манифеста не найден, пробуем дефолтный: {manifest_file}")
        manifest_file = os.path.join(rootdir, "src", "META-INF", "MANIFEST.MF")

    if not os.path.exists(manifest_file):
        print(f"ОШИБКА: файл манифеста '{manifest_file}' не найден.")
        input("Нажмите Enter...")
        return 1

    src_path = input("SRC_PATH (пусто = ROOTDIR/src): ").strip() or os.path.join(rootdir, "src")
    resources_path = input("RESOURCES_PATH (пусто = ROOTDIR/res): ").strip() or os.path.join(rootdir, "res")
    config_path = input("CONFIG_PATH (пусто = ROOTDIR/config): ").strip() or os.path.join(rootdir, "config")

    default_cache = os.path.join(rootdir, "package", "cache")
    cache_path = input(f"CACHE_PATH (пусто = ROOTDIR/package/cache): ").strip()
    if not cache_path:
        cache_path = default_cache

    license_path = input("LICENSE_PATH (пусто = ROOTDIR/license): ").strip() or os.path.join(rootdir, "license")
    libs_path = input("LIBS_PATH (пусто = ROOTDIR/libraries): ").strip() or os.path.join(rootdir, "libraries")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    default_out = os.path.join(script_dir, "out")
    out_path = input(f"OUT_PATH (пусто = {default_out}): ").strip()
    if not out_path:
        out_path = default_out
    else:
        out_path = os.path.abspath(out_path)

    print(f"BUILD_OUTPUT = {out_path}")

    jar_name = input("JAR_NAME (пусто = mizuka.jar): ").strip()
    if not jar_name:
        jar_name = f"mizuka-{version_serial}.jar"
    if not jar_name.endswith('.jar'):
        jar_name += '.jar'

    classes_output = os.path.join(out_path, "classes")
    libs_modules_path = os.path.join(libs_path, "modules")
    libs_classes_path = os.path.join(libs_path, "classes")

    print()
    print(f"MANIFEST_FILE   = {manifest_file}")
    print(f"JAR_NAME        = {jar_name}")
    print(f"ROOTDIR         = {rootdir}")
    print(f"SRC_PATH        = {src_path}")
    print(f"RESOURCES_PATH  = {resources_path}")
    print(f"CONFIG_PATH     = {config_path}")
    print(f"CACHE_PATH      = {cache_path}")
    print(f"LICENSE_PATH    = {license_path}")
    print(f"LIBS_PATH       = {libs_path}")
    print(f"BUILD_OUTPUT    = {out_path}")
    print(f"CLASSES_OUTPUT  = {classes_output}")
    print()
    input("Нажмите Enter для продолжения...")

    # ---- ПРОВЕРКА JAVA ----
    if not shutil.which("javac"):
        print("ОШИБКА: javac не найден в PATH.")
        print("Убедитесь, что JDK установлен и добавлен в PATH")
        input("Нажмите Enter...")
        return 1

    # ---- ОЧИСТКА/СОЗДАНИЕ ПАПОК ----
    if os.path.exists(out_path):
        print(f"Удаление '{out_path}'...")
        shutil.rmtree(out_path)

    print("Создание структуры...")
    Path(out_path).mkdir(exist_ok=True)
    Path(classes_output).mkdir(exist_ok=True)
    Path(out_path, "libraries").mkdir(exist_ok=True)
    Path(out_path, "res").mkdir(exist_ok=True)
    Path(out_path, "cache").mkdir(exist_ok=True)
    Path(out_path, "license").mkdir(exist_ok=True)
    Path(out_path, "config").mkdir(exist_ok=True)

    # Копируем version.json и README.md из корня
    print("Копирование version.json и README.md...")
    for src_file in ["version.json", "README.md"]:
        src_path_full = os.path.join(rootdir, src_file)
        if os.path.exists(src_path_full):
            shutil.copy2(src_path_full, out_path)
            print(f"  Копирован: {src_file}")
        else:
            print(f"  {src_file} не найден: {src_path_full}")

    # ---- СПИСОК ИСХОДНИКОВ ----
    sources_file = os.path.join(tempfile.gettempdir(), "mizuka_sources.txt")

    java_files = list(Path(src_path).rglob("*.java"))
    if not java_files:
        print("ОШИБКА: .java файлы не найдены.")
        input("Нажмите Enter...")
        return 1

    print(f"Найдено {len(java_files)} .java файлов в '{src_path}'...")
    with open(sources_file, 'w', encoding='utf-8') as f:
        for java_file in java_files:
            f.write(str(java_file) + '\n')

    # ---- КОМПИЛЯЦИЯ (Java 21) ----
    classpath = f"{libs_modules_path}/*{os.pathsep}{libs_classes_path}/*"

    print("Компиляция (Java 21)...")
    javac_cmd = [
        "javac", "-source", "21", "-target", "21",
        "-encoding", "UTF-8",
        "-d", classes_output,
        "-cp", classpath,
        f"@{sources_file}"
    ]

    if not run_command(javac_cmd):
        print("ОШИБКА компиляции!")
        if os.path.exists(sources_file):
            os.unlink(sources_file)
        input("Нажмите Enter...")
        return 1

    os.unlink(sources_file)

    # ---- JAR с внешним манифестом ----
    print("Создание JAR...")
    jar_output = os.path.join(out_path, jar_name)
    jar_cmd = [
        "jar", "cfm", jar_output,
        manifest_file,
        "-C", classes_output, "."
    ]
    if not run_command(jar_cmd):
        print("ОШИБКА создания JAR!")
        input("Нажмите Enter...")
        return 1

    # ---- КОПИРОВАНИЕ LIBS ----
    print("Копирование libraries...")
    if os.path.exists(libs_path):
        shutil.copytree(libs_path, os.path.join(out_path, "libraries"), dirs_exist_ok=True)

        # Удаление тестовых JAR
        test_patterns = ["*test*.jar", "testfx*.jar", "mockito*.jar",
                         "junit*.jar", "assertj*.jar", "byte*.jar"]
        for pattern in test_patterns:
            for testjar in Path(out_path, "libraries").rglob(pattern):
                try:
                    testjar.unlink()
                    print(f"  Удалён тест: {testjar.name}")
                except:
                    pass

    # Копирование остальных папок
    for src_folder, dst_folder in [
        (resources_path, os.path.join(out_path, "res")),
        (config_path, os.path.join(out_path, "config")),
        (license_path, os.path.join(out_path, "license"))
    ]:
        if os.path.exists(src_folder):
            shutil.copytree(src_folder, dst_folder, dirs_exist_ok=True)
            print(f"  Скопирована папка: {os.path.basename(src_folder)}")

    # ---- КОПИРОВАНИЕ CACHE ----
    print("Копирование cache...")
    if os.path.exists(cache_path):
        print(f"Используется cache из: {cache_path}")
        clean_cache_selective(cache_path, os.path.join(out_path, "cache"))
    else:
        print(f"Папка cache не найдена: {cache_path}")

    # ---- LAUNCH.BAT ----
    scripts_launch = os.path.join(rootdir, "scripts", "launch.bat")
    if os.path.exists(scripts_launch):
        print("Копирование launch.bat...")
        shutil.copy2(scripts_launch, out_path)
    else:
        print(f"launch.bat не найден: {scripts_launch}")

    print()
    print("=" * 47)
    print(" СБОРКА УСПЕШНО ЗАВЕРШЕНА")
    print(f" Папка: {out_path}/")
    print(f" JAR: {out_path}/{jar_name}")
    print("=" * 47)
    input("Нажмите Enter для выхода...")

if __name__ == "__main__":
    try:
        sys.exit(main() or 0)
    except KeyboardInterrupt:
        print("\nПрервано пользователем")
        input("Нажмите Enter для выхода...")
    except Exception as e:
        print(f"\nКРИТИЧЕСКАЯ ОШИБКА: {e}")
        input("Нажмите Enter для выхода...")