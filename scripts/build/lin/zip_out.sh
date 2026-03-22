#!/bin/bash
set -e

echo "========================================"
echo " ZIP: out/ -> ebanina-YYYYMMDD_HHMMSS.zip"
echo "========================================"

if [[ ! -d "out" ]]; then
    echo "ОШИБКА: папка 'out' не найдена!"
    exit 1
fi

# Имя архива с датой
ZIPNAME="ebanina-$(date +%%Y%%m%%d_%%H%%M%%S).zip"

echo "Архивируем out/ → $ZIPNAME..."

# Создаем ZIP с компрессией
zip -r "$ZIPNAME" out/ -q

SIZE=$(stat -f%z "$ZIPNAME" 2>/dev/null || stat -c%s "$ZIPNAME")
MB=$(echo "scale=1; $SIZE / 1024 / 1024" | bc -l 2>/dev/null || awk "BEGIN{printf \"%.1f\", $SIZE/1024/1024}")

echo "ГОТОВО: $ZIPNAME ($MB MB)"
