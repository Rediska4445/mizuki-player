#!/bin/bash

REPO="Rediska4445/mizuki-player"
API_URL="https://api.github.com"

echo "Poluchenie informatsii o relize..."
# Получаем URL ассета через curl и парсим его с помощью grep/sed
URL=$(curl -s $API_URL | grep "browser_download_url" | grep ".zip" | head -n 1 | cut -d '"' -f 4)

if [ -z "$URL" ]; then
    echo "Oshibka: Ne udalos nayti URL dlya skachivaniya."
    exit 1
fi

echo "ZIP download URL: $URL"
FILE_NAME=$(basename "$URL")

# Скачивание
echo "Skachivanie: $FILE_NAME..."
curl -L -O "$URL"

# Распаковка
echo "Raspakovka..."
unzip -o "$FILE_NAME"
rm "$FILE_NAME"

echo "Poisk ispolnyaemyh failov..."
# В Linux исполняемые файлы часто не имеют расширения,
# но так как проект под Windows/Wine, ищем .exe
mapfile -t EXE_FILES < <(find . -type f -name "*.exe")

COUNT=${#EXE_FILES[@]}

if [ "$COUNT" -eq 0 ]; then
    echo "Oshibka: .exe faily ne naydeny."
    exit 1
fi

echo "Naydeno $COUNT fail(ov)."

launch_exe() {
    local target="$1"
    echo "Zapusk: $target"
    # В Linux .exe запускаются через wine
    if command -v wine >/dev/null 2>&1; then
        wine "$target"
    else
        echo "Vnimanie: Wine ne ustanovlen. Prosto vyvodim put: $target"
    fi
}

if [ "$COUNT" -eq 1 ]; then
    launch_exe "${EXE_FILES[0]}"
else
    for i in "${!EXE_FILES[@]}"; do
        echo "[$((i+1))] ${EXE_FILES[$i]}"
    done

    read -p "Vvedite nomer dlya zapuska (Enter dlya vyhoda): " CHOICE
    if [[ "$CHOICE" =~ ^[0-9]+$ ]] && [ "$CHOICE" -le "$COUNT" ]; then
        launch_exe "${EXE_FILES[$((CHOICE-1))]}"
    else
        echo "Vyhod."
    fi
fi
