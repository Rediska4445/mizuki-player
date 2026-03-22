import os

def check_javadoc():
    path = input("Enter the directory path: ").strip().strip('"')

    if not os.path.exists(path):
        print(f"Error: Path '{path}' not found.")
        return

    java_files = []
    for root, _, files in os.walk(path):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))

    total = len(java_files)
    if total == 0:
        print("No Java files found.")
        return

    documented = []
    not_documented = []
    empty_files = []

    for file_path in java_files:
        try:
            # Проверяем размер файла
            if os.path.getsize(file_path) == 0:
                empty_files.append(file_path)
                continue

            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                if "/**" in content:
                    documented.append(file_path)
                else:
                    # Сохраняем путь и размер для сортировки
                    not_documented.append((file_path, len(content)))
        except Exception as e:
            print(f"Error reading {file_path}: {e}")

    # Расчеты
    doc_count = len(documented)
    undoc_count = len(not_documented)
    empty_count = len(empty_files)

    # Процент считаем от непустых файлов
    active_files = total - empty_count
    percentage = (doc_count / active_files * 100) if active_files > 0 else 0

    print("\n" + "="*30)
    print("   DETAILED JAVADOC REPORT")
    print("="*30)
    print(f"Total .java files:      {total}")
    print(f"Empty files (skipped):  {empty_count}")
    print(f"Documented files:       {doc_count}")
    print(f"Missing JavaDoc:        {undoc_count}")
    print(f"Documentation Coverage: {percentage:.2f}%")
    print("-" * 30)

    if undoc_count > 0:
        # Сортируем недокументированные по размеру (самые большие сверху)
        not_documented.sort(key=lambda x: x[1], reverse=True)

        print(f"\nTOP {min(5, undoc_count)} largest undocumented files:")
        for i, (f_path, size) in enumerate(not_documented[:5], 1):
            print(f"{i}. {os.path.basename(f_path)} ({size} chars)")

        # Сохраняем полный список в файл
        with open("missing_docs.txt", "w", encoding="utf-8") as log:
            log.write("FILES MISSING JAVADOC:\n" + "="*25 + "\n")
            for f_path, _ in not_documented:
                log.write(f"{f_path}\n")

        print(f"\n[!] Full list of undocumented files saved to: missing_docs.txt")

if __name__ == "__main__":
    check_javadoc()
    input("\nPress Enter to exit...")
