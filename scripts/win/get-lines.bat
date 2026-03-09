@echo off
setlocal enabledelayedexpansion

rem Запрос директории у пользователя
set /p dirPath="Введите путь к директории для подсчёта строк: "

rem Провекра, что папка существует
if not exist "%dirPath%" (
    echo Папка не найдена: %dirPath%
    pause
    exit /b
)

rem Массив расширений файлов для подсчёта
set extensions=java cpp css js html xml json fxml

rem Перебор расширений и подсчёт строк
for %%e in (%extensions%) do (
    set count=0
    for /r "%dirPath%" %%f in (*%%e) do (
        for /f %%l in ('find /v /c "" ^< "%%f"') do (
            set /a count+=%%l
        )
    )
    echo Количество строк в *.%%e: !count!
)

pause
