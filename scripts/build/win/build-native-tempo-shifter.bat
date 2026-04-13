@echo off
setlocal enabledelayedexpansion

echo === JNI Builder (clang++) ===
echo.

set /p CPP_FILE="Enter .cpp file name (e.g., tempo_shifter.cpp): "
if not exist "%CPP_FILE%" (
    echo Error: File '%CPP_FILE%' not found.
    exit /b 1
)

set /p OUTPUT_DLL="Enter output DLL name (e.g., tempo_shifter.dll): "
if "!OUTPUT_DLL:~-4!" neq ".dll" set OUTPUT_DLL=!OUTPUT_DLL!.dll

set /p JAVA_HOME_CUSTOM="Enter JAVA_HOME path (press Enter to use %%JAVA_HOME%%): "
if "!JAVA_HOME_CUSTOM!"=="" set JAVA_HOME_CUSTOM=%JAVA_HOME%

if "!JAVA_HOME_CUSTOM!"=="" (
    echo Error: JAVA_HOME is not set.
    exit /b 1
)

set INCLUDE1=!JAVA_HOME_CUSTOM!\include
set INCLUDE2=!JAVA_HOME_CUSTOM!\include\win32

if not exist "!INCLUDE1!" (
    echo Error: Directory not found: !INCLUDE1!
    exit /b 1
)

echo.
set /p OPTIMIZE="Use -O3 -march=native? (Y/n): "
set /p DEBUG="Enable debug info? (y/N): "
set /p STD="C++ standard [17]: "
if "!STD!"=="" set STD=c++17

set /p MOVE_DLL="Move DLL to output folder? (Y/n): "
if /i "!MOVE_DLL!" neq "n" set MOVE_DLL=Y

set DEST_FOLDER=libraries\bin
if /i "!MOVE_DLL!"=="Y" (
    set /p DEST_FOLDER_INPUT="Enter destination folder [libraries\bin]: "
    if not "!DEST_FOLDER_INPUT!"=="" set DEST_FOLDER=!DEST_FOLDER_INPUT!
)

set FLAGS=-std=!STD! -shared -o !OUTPUT_DLL! !CPP_FILE! -I"!INCLUDE1!" -I"!INCLUDE2!"

if /i not "!OPTIMIZE!"=="n" set FLAGS=!FLAGS! -O3 -march=native
if /i "!DEBUG!"=="y" set FLAGS=!FLAGS! -g

set FLAGS=!FLAGS! -Wl,--add-stdcall-alias

echo.
echo Compile command:
echo clang++ !FLAGS!
echo.

echo Compiling...
clang++ !FLAGS!

if %ERRORLEVEL% neq 0 (
    echo.
    echo Compilation failed. Exit code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

echo.
echo Success: !OUTPUT_DLL! created.

if /i "!MOVE_DLL!"=="Y" (
    if not exist "!DEST_FOLDER!" (
        echo Creating directory: !DEST_FOLDER!
        mkdir "!DEST_FOLDER!"
    )

    echo Moving !OUTPUT_DLL! to !DEST_FOLDER!\...
    move /Y "!OUTPUT_DLL!" "!DEST_FOLDER!\!OUTPUT_DLL!" >nul

    if %ERRORLEVEL% equ 0 (
        echo DLL moved to: !DEST_FOLDER!\!OUTPUT_DLL!
    ) else (
        echo Warning: Failed to move DLL. It remains in current directory.
    )
)

endlocal
