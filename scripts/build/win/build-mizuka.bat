@echo off
setlocal

chcp 65001 >nul

echo ========================================
echo  Ebanina build script (Java 17)
echo ========================================
echo.

REM ---- ВВОД ПУТЕЙ ----

echo Укажите корень проекта (где src, res, config, cache, license, libraries).
echo Оставьте пустым для текущей папки.
set /p ROOTDIR=ROOTDIR= 
if "%ROOTDIR%"=="" set ROOTDIR=%CD%

echo Укажите путь к файлу MANIFEST (пусто = %ROOTDIR%\src\META-INF\MANIFEST.MF).
set /p MANIFEST_FILE=MANIFEST_FILE= 
if "%MANIFEST_FILE%"=="" set MANIFEST_FILE=%ROOTDIR%\src\META-INF\MANIFEST.MF

if not exist "%MANIFEST_FILE%" (
    echo Файл манифеста не найден, пробуем дефолтный: %ROOTDIR%\src\META-INF\MANIFEST.MF
    set MANIFEST_FILE=%ROOTDIR%\src\META-INF\MANIFEST.MF
)

if not exist "%MANIFEST_FILE%" (
    echo ОШИБКА: файл манифеста "%MANIFEST_FILE%" не найден.
    pause
    exit /b 1
)

echo Укажите папку src (пусто = %ROOTDIR%\src).
set /p SRC_PATH=SRC_PATH= 
if "%SRC_PATH%"=="" set SRC_PATH=%ROOTDIR%\src

echo Укажите папку res (пусто = %ROOTDIR%\res).
set /p RESOURCES_PATH=RESOURCES_PATH= 
if "%RESOURCES_PATH%"=="" set RESOURCES_PATH=%ROOTDIR%\res

echo Укажите папку config (пусто = %ROOTDIR%\config).
set /p CONFIG_PATH=CONFIG_PATH= 
if "%CONFIG_PATH%"=="" set CONFIG_PATH=%ROOTDIR%\config

echo Укажите папку cache (пусто = %ROOTDIR%\cache).
set /p CACHE_PATH=CACHE_PATH= 
if "%CACHE_PATH%"=="" set CACHE_PATH=%ROOTDIR%\cache

echo Укажите папку license (пусто = %ROOTDIR%\license).
set /p LICENSE_PATH=LICENSE_PATH= 
if "%LICENSE_PATH%"=="" set LICENSE_PATH=%ROOTDIR%\license

echo Укажите папку libraries (пусто = %ROOTDIR%\libraries).
set /p LIBS_PATH=LIBS_PATH= 
if "%LIBS_PATH%"=="" set LIBS_PATH=%ROOTDIR%\libraries

echo Укажите папку вывода (пусто = out).
set /p BUILD_OUTPUT=BUILD_OUTPUT= 
if "%BUILD_OUTPUT%"=="" set BUILD_OUTPUT=out

REM Папка для классов — отдельная, чтобы не тащить в JAR res/cache/и т.д.
set CLASSES_OUTPUT=%BUILD_OUTPUT%\classes

set LIBS_MODULES_PATH=%LIBS_PATH%\modules
set LIBS_CLASSES_PATH=%LIBS_PATH%\classes

echo.
echo MANIFEST_FILE   = %MANIFEST_FILE%
echo ROOTDIR         = %ROOTDIR%
echo SRC_PATH        = %SRC_PATH%
echo RESOURCES_PATH  = %RESOURCES_PATH%
echo CONFIG_PATH     = %CONFIG_PATH%
echo CACHE_PATH      = %CACHE_PATH%
echo LICENSE_PATH    = %LICENSE_PATH%
echo LIBS_PATH       = %LIBS_PATH%
echo LIBS_MODULES    = %LIBS_MODULES_PATH%
echo LIBS_CLASSES    = %LIBS_CLASSES_PATH%
echo BUILD_OUTPUT    = %BUILD_OUTPUT%
echo CLASSES_OUTPUT  = %CLASSES_OUTPUT%
echo.
pause

REM ---- ПРОВЕРКА JAVA ----

where javac >nul 2>nul
if errorlevel 1 (
    echo ОШИБКА: javac не найден в PATH.
    pause
    exit /b 1
)

REM ---- ОЧИСТКА/СОЗДАНИЕ ПАПОК ----

if exist "%BUILD_OUTPUT%" (
    echo Удаление "%BUILD_OUTPUT%"...
    rmdir /s /q "%BUILD_OUTPUT%"
)

echo Создание структуры...
mkdir "%BUILD_OUTPUT%"
mkdir "%CLASSES_OUTPUT%"
mkdir "%BUILD_OUTPUT%\libraries"
mkdir "%BUILD_OUTPUT%\res"
mkdir "%BUILD_OUTPUT%\cache"
mkdir "%BUILD_OUTPUT%\license"
mkdir "%BUILD_OUTPUT%\config"

REM ---- СПИСОК ИСХОДНИКОВ ----

set "SOURCES_FILE=%TEMP%\ebanina_sources.txt"
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"

echo Поиск .java в "%SRC_PATH%"...
for /r "%SRC_PATH%" %%f in (*.java) do (
    echo %%f>>"%SOURCES_FILE%"
)

if not exist "%SOURCES_FILE%" (
    echo ОШИБКА: .java файлы не найдены.
    pause
    exit /b 1
)

REM ---- КОМПИЛЯЦИЯ ----

set "CLASSPATH=%LIBS_MODULES_PATH%\*;%LIBS_CLASSES_PATH%\*"

echo Компиляция...
javac -source 17 -target 17 -encoding UTF-8 -d "%CLASSES_OUTPUT%" -cp "%CLASSPATH%" @"%SOURCES_FILE%"
if errorlevel 1 (
    echo ОШИБКА компиляции.
    del "%SOURCES_FILE%"
    pause
    exit /b 1
)
del "%SOURCES_FILE%"

REM ---- JAR с внешним манифестом ----

echo Создание JAR...

REM В JAR добавляем только классы (и ресурсы, если захочешь, их можно положить внутрь CLASSES_OUTPUT)
jar cfm "%BUILD_OUTPUT%\ebanina-1.4.9.jar" "%MANIFEST_FILE%" -C "%CLASSES_OUTPUT%" .

REM ---- КОПИРОВАНИЕ LIBS/RES/CONFIG/LICENSE ----

echo Копирование libraries...
xcopy "%LIBS_PATH%\*" "%BUILD_OUTPUT%\libraries\" /E /I /Y >nul

for /r "%BUILD_OUTPUT%\libraries" %%f in (*test*.jar testfx*.jar mockito*.jar junit*.jar assertj*.jar byte*.jar) do (
    del "%%f" >nul 2>nul
)

echo Копирование res...
if exist "%RESOURCES_PATH%" xcopy "%RESOURCES_PATH%\*" "%BUILD_OUTPUT%\res\" /E /I /Y >nul

echo Копирование config...
if exist "%CONFIG_PATH%" xcopy "%CONFIG_PATH%\*" "%BUILD_OUTPUT%\config\" /E /I /Y >nul

echo Копирование license...
if exist "%LICENSE_PATH%" xcopy "%LICENSE_PATH%\*" "%BUILD_OUTPUT%\license\" /E /I /Y >nul

REM ---- КОПИРОВАНИЕ CACHE ----
echo Копирование cache...

REM 1. Создаем структуру папок (включая пустые)
if exist "%CACHE_PATH%" (
    xcopy "%CACHE_PATH%" "%BUILD_OUTPUT%\cache\" /T /E /I /Y >nul 2>nul
)

REM 2. Копируем ВСЕ файлы из ВСЕХ подпапок (включая cache\cache)
if exist "%CACHE_PATH%" (
    xcopy "%CACHE_PATH%\*.*" "%BUILD_OUTPUT%\cache\" /E /I /Y /H >nul 2>nul
)

echo Cache полностью скопирован (структура + все файлы)

REM ---- launch.bat из %ROOTDIR%\scripts ----

if exist "%ROOTDIR%\scripts\launch.bat" (
    echo Копирование launch.bat...
    copy "%ROOTDIR%\scripts\launch.bat" "%BUILD_OUTPUT%\" >nul
) else (
    echo launch.bat не найден по пути %ROOTDIR%\scripts\launch.bat
)

echo.
echo ========================================
echo  СБОРКА УСПЕШНО ЗАВЕРШЕНА
echo  JAR: %BUILD_OUTPUT%\ebanina-1.4.9.jar
echo ========================================
pause
endlocal
