@echo off
setlocal enabledelayedexpansion

set repo=Rediska4445/mizuki-player
curl -s https://api.github.com/repos/%repo%/releases/latest > release.json

for /f "delims=" %%a in ('findstr "browser_download_url" release.json') do (
    set "line=%%a"
    set "line=!line:*browser_download_url": =!"
    set "url=!line:"=!"
    goto :found
)

:found
echo ZIP download URL: !url!

curl -L -O "!url!"

del release.json

echo Extracting ZIP archive...

REM Find first .zip file in current folder and extract it
set "zipfile="
for %%z in (*.zip) do (
    set "zipfile=%%z"
    echo Extracting: %%z
    powershell -Command "Expand-Archive -Path '.\\%%z' -DestinationPath '.' -Force"
    goto :extract_done
)

:extract_done
if errorlevel 1 (
    echo ERROR: Failed to extract archive.
    pause
    exit /b
) else (
    echo Done! Archive contents extracted to current directory.
    
    REM Delete ZIP file after successful extraction
    if defined zipfile (
        echo Cleaning up: !zipfile!
        del "!zipfile!"
    )
)

echo.
echo Searching for executable files (.exe)...

set "found_exe=0"
set "first_exe="

REM Search for all .exe files in current folder and subfolders
for /r . %%f in (*.exe) do (
    set /a found_exe+=1
    if !found_exe! equ 1 set "first_exe=%%f"
    echo [!found_exe!] %%f
)

if !found_exe! equ 0 (
    echo No .exe files found.
    pause
    exit /b
)

echo.
echo Found !found_exe! executable file(s).

if !found_exe! equ 1 (
    echo Auto-launching only found executable:
    echo !first_exe!
    start "" "!first_exe!"
    goto :launched
)

echo Select file number to launch (or Enter to exit):
set /p choice="Number (1-!found_exe!): "

if "!choice!"=="" (
    echo Exit.
    pause
    exit /b
)

set count=0
for /r . %%f in (*.exe) do (
    set /a count+=1
    if !count! equ !choice! (
        echo Launching: %%f
        start "" "%%f"
        goto :launched
    )
)

echo Invalid number!
:launched
pause
