#!/bin/bash

# JNI DLL Compiler (Clang-cl) for Windows (via MinGW/MSYS2)
# Requires: clang, make, installed JDK

clear
echo "========================================="
echo "    JNI DLL Compiler (Clang-cl)"
echo "========================================="
echo

# Ask for CPP file name
while true; do
    read -p "CPP file (dwm1.cpp): " CPP_NAME
    CPP_NAME=${CPP_NAME:-dwm1.cpp}
    if [[ ! "$CPP_NAME" =~ \.cpp$ ]]; then
        CPP_NAME="$CPP_NAME.cpp"
    fi
    if [[ -f "$CPP_NAME" ]]; then
        echo "[+] Source: $CPP_NAME"
        break
    else
        echo "[!] $CPP_NAME not found!"
    fi
done

# Ask for output DLL name
read -p "DLL file (dwm.dll): " DLL_NAME
DLL_NAME=${DLL_NAME:-dwm.dll}
if [[ ! "$DLL_NAME" =~ \.dll$ ]]; then
    DLL_NAME="$DLL_NAME.dll"
fi
echo "[+] Output: $DLL_NAME"

# Ask for JDK path
while true; do
    read -p "JDK path (C:/Program Files/Java/jdk-21): " JDK_PATH
    if [[ -f "$JDK_PATH/include/jni.h" ]]; then
        echo "[+] JDK: $JDK_PATH"
        break
    else
        echo "[!] jni.h not found!"
    fi
done

# Compilation steps
echo
echo "[1/3] Cleanup..."
rm -f *.dll *.lib *.exp *.obj *.pdb 2>/dev/null
echo "[OK]"

echo "[2/3] Compiling..."
echo "clang-cl /LD /Fe:\"$DLL_NAME\" \"$CPP_NAME\" /I\"$JDK_PATH/include\" /I\"$JDK_PATH/include/win32\" dwmapi.lib user32.lib kernel32.lib"
echo

# Compile using clang for Windows DLL
clang-cl /LD /Fe:"$DLL_NAME" "$CPP_NAME" \
    /I"$JDK_PATH/include" \
    /I"$JDK_PATH/include/win32" \
    dwmapi.lib user32.lib kernel32.lib

if [[ $? -eq 0 ]]; then
    echo
    echo "[3/3] SUCCESS!"
    ls -la "$DLL_NAME" 2>/dev/null | head -1
    echo
    
    # Copy to target directory
    TARGET_DIR="Ebanina-VST/libraries/bin"
    read -p "Copy to $TARGET_DIR? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]] && [[ -d "$TARGET_DIR" ]]; then
        cp "$DLL_NAME" "$TARGET_DIR/"
        echo "[+] Copied!"
    elif [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "[!] Directory $TARGET_DIR does not exist!"
    fi
    
    echo
    echo "Java: System.loadLibrary(\"${DLL_NAME%.*}\");"
else
    echo "[!] COMPILATION ERROR!"
fi

echo
read -p "Press Enter to exit..."
