@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo  ZIP: out/ ^-^> ebanina-YYYYMMDD_HHMMSS.zip
echo ========================================

if not exist "out" (
    echo ОШИБКА: папка 'out' не найдена!
    pause
    exit /b 1
)

REM Имя архива с датой
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" ^& set "YYYY=%dt:~0,4%" ^& set "MM=%dt:~4,2%" ^& set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" ^& set "Min=%dt:~10,2%" ^& set "Sec=%dt:~12,2%"
set "ZIPNAME=ebanina-%YYYY%%MM%%DD%_%HH%%Min%%Sec%.zip"

echo Архивируем out/ -^> %ZIPNAME%...

REM Нужен PowerShell (Windows 10+)
powershell -command ^
"$z = [System.IO.Compression.ZipFile]::CreateEntryFromDirectory('out', '%ZIPNAME%'); ^
[System.IO.Compression.ZipFile]::Open('%ZIPNAME%', 'Create'); ^
Get-ChildItem 'out' -Recurse | ForEach { ^
    $rel = $_.FullName.Substring(3); ^
    Add-Type -AssemblyName System.IO.Compression.FileSystem; ^
    [System.IO.Compression.ZipFile]::Extensions.AddEntry((New-Object System.IO.Compression.ZipArchive), $rel, $_.FullName) ^
}"

if exist "%ZIPNAME%" (
    for %%F in ("%ZIPNAME%") do set SIZE=%%~zF
    set /a MB=!SIZE!/1024/1024
    echo ГОТОВО: %ZIPNAME% (^!MB! MB)
) else (
    echo ОШИБКА создания архива!
)

pause
