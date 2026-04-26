@echo off
setlocal enabledelayedexpansion

rem Ask user for directory path
set /p dirPath="Enter path to directory for line counting: "

rem Check if folder exists
if not exist "%dirPath%" (
    echo Folder not found: %dirPath%
    pause
    exit /b
)

rem Array of file extensions to count
set extensions=java cpp css js html xml json fxml

rem Loop through extensions and count lines
for %%e in (%extensions%) do (
    set count=0
    for /r "%dirPath%" %%f in (*%%e) do (
        for /f %%l in ('find /v /c "" ^< "%%f"') do (
            set /a count+=%%l
        )
    )
    echo Lines in *.%%e: !count!
)

pause
