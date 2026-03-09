@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
title JNI DLL Compiler (Clang-cl)

echo ========================================
echo     JNI DLL Compiler ^(Clang-cl^)
echo ========================================
echo.

:: Запрос названия CPP файла
:input_cpp
set /p "CPP_NAME=CPP файл ^(dwm1.cpp^): "
if "!CPP_NAME!"=="" set "CPP_NAME=dwm1.cpp"
if not "!CPP_NAME:~-4!"==".cpp" set "CPP_NAME=!CPP_NAME!.cpp"
if not exist "!CPP_NAME!" (
    echo [!] !CPP_NAME! не найден!
    goto :input_cpp
)
echo [+] Исходник: !CPP_NAME!

:: Запрос названия выходного файла
:input_dll
set /p "DLL_NAME=DLL файл ^(dwm.dll^): "
if "!DLL_NAME!"=="" set "DLL_NAME=dwm.dll"
if not "!DLL_NAME:~-4!"==".dll" set "DLL_NAME=!DLL_NAME!.dll"
echo [+] Выход: !DLL_NAME!

:: Запрос пути к JDK
:ask_jdk
set /p "JDK_PATH=JDK путь ^(C:\Program Files\Java\jdk-21^): "
if not exist "%JDK_PATH%\include\jni.h" (
    echo [!] jni.h не найден!
    goto :ask_jdk
)
echo [+] JDK: %JDK_PATH%

:: ТОЧНАЯ ТВОЯ КОМАНДА
echo.
echo [1/3] Очистка...
del /q *.dll *.lib *.exp *.obj *.pdb 2>nul
echo [OK]

echo [2/3] Компиляция...
echo clang-cl /LD /Fe:"!DLL_NAME!" "!CPP_NAME!" /I"!JDK_PATH!\include" /I"!JDK_PATH!\include\win32" dwmapi.lib user32.lib kernel32.lib
echo.

clang-cl /LD /Fe:"!DLL_NAME!" "!CPP_NAME!" /I"!JDK_PATH!\include" /I"!JDK_PATH!\include\win32" dwmapi.lib user32.lib kernel32.lib

if %ERRORLEVEL%==0 (
    echo.
    echo [3/3] ^✓ УСПЕХ!
    dir "!DLL_NAME!" | findstr "!DLL_NAME!"
    echo.
    
    :: Копирование
    set "TARGET_DIR=C:\Users\2022\Desktop\программы\Ebanina-Test\Ebanina\Ebanina-VST\libraries\bin"
    choice /C YN /M "Скопировать в !TARGET_DIR! ? [Y/N] "
    if !ERRORLEVEL!==1 (
        if exist "!TARGET_DIR!\" (
            copy "!DLL_NAME!" "!TARGET_DIR!\"
            echo [+] ^✓ Скопировано!
        ) else (
            echo [!] Папка !TARGET_DIR! не существует!
        )
    )
    
    echo.
    echo Java: System.loadLibrary("!DLL_NAME:~0,-4!");
) else (
    echo [!] ОШИБКА КОМПИЛЯЦИИ!
)

echo.
pause
