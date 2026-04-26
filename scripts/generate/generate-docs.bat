rem "Запускать в корневом каталоге"

set CURRENT_DIR=%CD%

if not exist "%CURRENT_DIR%\libraries" (
    echo Папка libraries не найдена в %CURRENT_DIR%.
    set /p CURRENT_DIR=Пожалуйста, введите путь к директории, которая будет использована вместо CURRENT_DIR:
)

"C:\Program Files\Java\jdk-21\bin\javadoc.exe" --module-path="%CURRENT_DIR%\libraries\modules" --add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.swing,javafx.media,org.controlsfx.controls,jdk.management,java.management,mp3agic,java.prefs,com.jfoenix,com.github.kwhat.jnativehook,jsoup,lastfm.java,java.desktop,com.sun.jna,com.sun.jna.platform,org.apache.commons.io,untitled10 -classpath "%CURRENT_DIR%\libraries\classes;%CURRENT_DIR%\libraries\classes\ebanina-std-utils.jar;%CURRENT_DIR%\libraries\classes\ebanina-soundcloud-api-wrapper.jar;%CURRENT_DIR%\libraries\classes\vst3host.jar;%CURRENT_DIR%\libraries\classes\vorbisspi-1.0.3.3.jar;%CURRENT_DIR%\libraries\classes\tritonus-share-0.3.7.4.jar;%CURRENT_DIR%\libraries\classes\mp3spi-1.9.5.4.jar;%CURRENT_DIR%\libraries\classes\lastfm-java-0.1.2.jar;%CURRENT_DIR%\libraries\classes\JVstHost.jar;%CURRENT_DIR%\libraries\classes\jlayer-1.0.1.4.jar;%CURRENT_DIR%\libraries\classes\ebanina-mod-api.jar;%CURRENT_DIR%\libraries\classes\ebanina-audio-player.jar" -d "%CURRENT_DIR%\java-docs" -sourcepath "%CURRENT_DIR%\src" -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 --add-opens=java.base/java.lang.reflect=com.jfoenix --add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED --add-opens=javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED -subpackages rf