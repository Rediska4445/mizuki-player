#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBS_BASE="$SCRIPT_DIR/../../libraries"

echo "DEBUG: SCRIPT_DIR is $SCRIPT_DIR"
echo "DEBUG: Checking directory $LIBS_BASE"

if [ -d "$LIBS_BASE" ]; then
    echo "DEBUG: Directory exists. Listing files:"
    ls -R "$LIBS_BASE"
else
    echo "DEBUG: Directory does not exist!"
fi

while [ ! -d "$LIBS_BASE" ]; do
    echo "Error: Директория с библиотеками не найдена по пути: $LIBS_BASE"
    read -p "Пожалуйста, введите корректный путь к папке 'libraries': " user_input
    if [[ "$user_input" = /* ]]; then
        LIBS_BASE="$user_input"
    else
        LIBS_BASE="$PWD/$user_input"
    fi
done

echo "Библиотеки найдены в: $LIBS_BASE"

install_lib() {
    local group=$1
    local artifact=$2
    local version=$3

    local file_path=$(find "$LIBS_BASE" -name "${artifact}-${version}.jar" | head -n 1)

    if [ -z "$file_path" ]; then
        file_path=$(find "$LIBS_BASE" -name "${artifact}.jar" | head -n 1)
    fi

    if [ -f "$file_path" ]; then
        echo "Installing $artifact..."
        mvn install:install-file -Dfile="$file_path" -DgroupId="$group" -DartifactId="$artifact" -Dversion="$version" -Dpackaging=jar
    else
        echo "Warning: Could not find jar for $artifact (tried ${artifact}-${version}.jar and ${artifact}.jar)"
    fi
}

echo "Installing all local dependencies..."

# JavaFX
install_lib "org.controlsfx" "controlsfx" "11.1.2"
install_lib "org.openjfx" "javafx-swt" "21"
install_lib "org.openjfx" "javafx.base" "21"
install_lib "org.openjfx" "javafx.controls" "21"
install_lib "org.openjfx" "javafx.fxml" "21"
install_lib "org.openjfx" "javafx.graphics" "21"
install_lib "org.openjfx" "javafx.media" "21"
install_lib "org.openjfx" "javafx.swing" "21"
install_lib "org.openjfx" "javafx.web" "21"

# Modules
install_lib "me" "spotify-api-wrapper" "1.0.2"
install_lib "ebanina" "ebanina-soundcloud-api-wrapper" "1.0"
install_lib "api" "ebanina-mod-api" "1.0"
install_lib "deezer" "mizuka-deezer-api-wrapper" "1.0"
install_lib "mizuka" "mizuka-std-utils" "1.3.1"
install_lib "rf" "vst3host" "1.0"

# Other
install_lib "javazoom" "jlayer" "1.0.1.4"
install_lib "com.jfoenix" "jfoenix" "9.0.10"
install_lib "net.java.dev.jna" "jna" "5.14.0"
install_lib "net.java.dev.jna" "jna-platform" "5.14.0"
install_lib "com.github.kwhat.jnativehook" "jnativehook" "2.2.2"
install_lib "org.jsoup" "jsoup" "1.10.1"
install_lib "com.github.lastfmjava" "lastfm-java" "0.1.2"
install_lib "com.mpatric" "mp3agic" "0.9.1"
install_lib "com.jcraft" "jorbis" "0.0.17.4"
install_lib "com.smadja" "JVstHost" "1.0"
install_lib "com.googlecode.soundlibs" "mp3spi" "1.9.5.4"
install_lib "com.googlecode.soundlibs" "tritonus-share" "0.3.7.4"
install_lib "com.googlecode.soundlibs" "vorbisspi" "1.0.3.3"
install_lib "com.googlecode.json-simple" "json-simple" "1.1.1"
install_lib "com.squareup.okhttp3" "okhttp" "3.12.13"

echo "Installing test dependencies..."

# JUnit 5
install_lib "org.junit.jupiter" "junit-jupiter-api" "6.0.0-M2"
install_lib "org.junit.jupiter" "junit-jupiter-engine" "6.0.0-M2"
install_lib "org.junit.jupiter" "junit-jupiter-params" "6.0.0-M2"
install_lib "org.junit.platform" "junit-platform-console-standalone" "1.10.0"

# Mockito
install_lib "org.mockito" "mockito-core" "5.20.0"
install_lib "org.mockito" "mockito-inline" "5.2.0"
install_lib "org.mockito" "mockito-junit-jupiter" "5.20.0"

# TestFX
install_lib "org.testfx" "testfx-core" "4.0.18"
install_lib "org.testfx" "testfx-junit5" "4.0.18"

# Others
install_lib "org.assertj" "assertj-core" "3.27.6"
install_lib "net.bytebuddy" "byte-buddy" "1.17.8"
install_lib "net.bytebuddy" "byte-buddy-agent" "1.17.8"

# Okio
install_lib "com.squareup.okio" "okio" "1.15.0"

echo "All JAR dependencies installed."