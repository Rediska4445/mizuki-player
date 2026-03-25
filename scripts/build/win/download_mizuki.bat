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
echo Ссылка на ZIP: %url%

curl -L -O "%url%"

del release.json
