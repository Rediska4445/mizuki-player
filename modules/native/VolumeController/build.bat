@echo off
setlocal enabledelayedexpansion

echo === VolumeControl JNI Builder (clang++) ===
echo.

:: Настройки по умолчанию для твоего проекта
set DEFAULT_CPP=VolumeControl.cpp
set DEFAULT_DLL=VolumeLib.dll
set DEFAULT_DEST=modules\native\VolumeController

set /p CPP_FILE="Enter .cpp file name [%DEFAULT_CPP%]: "
if "!CPP_FILE!"=="" set CPP_FILE=%DEFAULT_CPP%

if not exist "%CPP_FILE%" (
    echo Error: File '%CPP_FILE%' not found.
    exit /b 1
)

set /p OUTPUT_DLL="Enter output DLL name [%DEFAULT_DLL%]: "
if "!OUTPUT_DLL!"=="" set OUTPUT_DLL=%DEFAULT_DLL%
if "!OUTPUT_DLL:~-4!" neq ".dll" set OUTPUT_DLL=!OUTPUT_DLL!.dll

set /p JAVA_HOME_CUSTOM="Enter JAVA_HOME path (press Enter to use %JAVA_HOME%): "
if "!JAVA_HOME_CUSTOM!"=="" set JAVA_HOME_CUSTOM=%JAVA_HOME%

if "!JAVA_HOME_CUSTOM!"=="" (
    echo Error: JAVA_HOME is not set.
    exit /b 1
)

set INCLUDE1=!JAVA_HOME_CUSTOM!\include
set INCLUDE2=!JAVA_HOME_CUSTOM!\include\win32

echo.
set /p OPTIMIZE="Use -O3 (Recommended for production)? (Y/n): "
set /p STD="C++ standard [17]: "
if "!STD!"=="" set STD=c++17

set /p MOVE_DLL="Move DLL to output folder? (Y/n): "
if /i "!MOVE_DLL!" neq "n" set MOVE_DLL=Y

if /i "!MOVE_DLL!"=="Y" (
    set /p DEST_FOLDER="Enter destination folder [%DEFAULT_DEST%]: "
    if "!DEST_FOLDER!"=="" set DEST_FOLDER=%DEFAULT_DEST%
)

:: ФЛАГИ: Добавлен -lole32 для работы с Windows COM API
set FLAGS=-std=!STD! -shared -o !OUTPUT_DLL! "!CPP_FILE!" -I"!INCLUDE1!" -I"!INCLUDE2!" -lole32

if /i not "!OPTIMIZE!"=="n" set FLAGS=!FLAGS! -O3

echo.
echo Compile command:
echo clang++ !FLAGS!
echo.

echo Compiling...
clang++ !FLAGS!

if %ERRORLEVEL% neq 0 (
    echo.
    echo [!] Compilation failed. Check if clang++ is in PATH.
    pause
    exit /b %ERRORLEVEL%
)

echo Success: !OUTPUT_DLL! created.

if /i "!MOVE_DLL!"=="Y" (
    if not exist "!DEST_FOLDER!" (
        mkdir "!DEST_FOLDER!"
    )
    move /Y "!OUTPUT_DLL!" "!DEST_FOLDER!\!OUTPUT_DLL!" >nul
    echo DLL moved to: !DEST_FOLDER!\!OUTPUT_DLL!
)

echo.
echo Done.
pause
