<div align="center">
  <img src="res/visual/gui/logo-hd.png" width="500">
</div>


<div align="center" style="margin-bottom: 22px;">
  <img src="https://img.shields.io/badge/Java-19-orange?style=for-the-badge&logo=java&logoColor=white" alt="Java 19"/>
  <img src="https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=javafx&logoColor=white" alt="JavaFX"/>
  <img src="https://img.shields.io/badge/VST-VST3-brightgreen?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDUiIGhlaWdodD0iNDUiIHZpZXdCb3g9IjAgMCA0NSA0NSIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjIuNSIgY3k9IjIyLjUiIHI9IjIyLjUiIGZpbGw9IiM0RDcyRjQiLz4KPHN2ZyB4PSI0IiB5PSI0IiB3aWR0aD0iMzciIGhlaWdodD0iMzciIHZpZXdCb3g9IjAgMCAzNyAzNyIgZmlsbD0ibm9uZSI+CjxwYXRoIGQ9Ik0xOC41IDEyQzE5LjgyIDEyIDIwLjkgMTIuMTcgMjEuMjUgMTIuNjVDMjEuNiAxMy4xNSAyMS42IDEzLjgyIDIxLjI1IDE0LjM3QzIwLjkgMTQuODIgMTkuODIgMTQuOTkgMTguNSAxNC45OUgxNC4yNUgxMC4xMjVDOS44IDE0Ljk5IDkuNzUgMTUuMTMgOS40NSAxNS41NUM5LjE1IDE1Ljk1IDkuMTUgMTYuNzIgOS40NSAxNy4yN0wxMC4xMjUgMTguNUMxMC40NSAxOC45NSAxMC42IDE5LjA5IDExIDE5LjA5SDIzSDEyLjVMMTIuNSAxOS4wOUMxMi44NCAxOS4wOSAxMy4wNSAxOC45NSAxMy4zNSAxOC41TDQuMDUgMTUuNUw0LjA1IDE1LjVMMy4zNSA0LjVMMTguNSAxMiBaIiBmaWxsPSJ3aGl0ZSIvPgo8L3N2Zz4KPC9zdmc+" alt="VST Support"/>
</div>

<div align="center">
<span style="font-size: 36px; font-weight: bold;">Ebanina (Mizuki)</span><br>
<sup style="font-size: 18px;">v1.4</sup>
</div>

---
<h4>This project wasn't created for consumer use, and none of the features it includes offer anything serious.</h4>
In fact, a README is also unnecessary!

---

<div align="center">
  <span style="font-size: 16px; font-weight: light;">Mizuki</span><br>

  Pet project for audio experiments, VST hosting, network streaming & Java tech exploration.
  The project is not intended for users and serves as a support for experiments 
  and fantasies in the field of: audio processing, working with technologies, working with streams and the Java/C++ language in general.
</div>

<p align="center">
  <a href="https://openjfx.io">
    <img alt="JavaFX" src="https://img.shields.io/badge/JavaFX-21-blue?style=social&logo=javafx&logoColor=white">
  </a>
  <a href="https://www.last.fm/api">
    <img alt="LastFM" src="https://img.shields.io/badge/LastFM-API-brightgreen?style=social&logo=music&logoColor=white">
  </a>
  <a href="https://steinbergmedia.github.io/vst3_dev_portal/">
    <img alt="VST3" src="https://img.shields.io/badge/VST3-SDK-purple?style=social&logo=music&logoColor=white">
  </a>
</p>

---

# Easy Start

To open the app, you don't need any technical skills. Just:

### Launch the Setup Script
The fastest way to get started is to run the automated script, which handles the latest engine download and setup:

#### **Windows:**
*   [Download download-mizuka.bat](https://github.com/Rediska4445/mizuki-player/blob/main/scripts/build/download-mizuka.bat)
*   Run: `download-mizuka.bat`

#### **Linux:**
*   [Download download-mizuka.sh](https://github.com/Rediska4445/mizuki-player/blob/main/scripts/build/download-mizuka.sh)
*   Run: `bash download_mizuka.sh`

---


# Features 

### Overview

| **VST/VST3**                  | **Network**                                      | **Audio DSP**      | **Local Media**    |
|-------------------------------|--------------------------------------------------|--------------------|--------------------|
| Plugin hosting (without Juce) | Spotify, Deezer, SoundCloud, LastFM, Apple Music | Volume/Tempo/Pitch | Internalization    |
| I/O routing                   | Track download                                   | Smart normalizer   | Advanced tagging   |
| Editor GUI                    | Metadata extraction                              | Skip drop, intro   | Complex search     |
| Parameter control             | Track translation                                | MP3/WAV support    | State persistence  |
| State save/restore            | Network playlists                                | Custom formats     | Moddable resources |

> [!NOTE]
> For SoundCloud and LastFM need use proxy

<details>
  <summary>Click to see full details of features</summary>

  - Ability to use VST/VST3 plugins;
    - Change plugin inputs/outputs;
      - Open the plugin editor;
      - Change mix.
        - Save and restore plugin states;
        - Change parameters outside the editor;
        - Configure plugin hosts;
  - Ability to interact with network resources;
    - Download tracks;
      - Download to local playlist;
      - Play in the player in streaming or buffer mode;
    - View and play/download recommendations from third-party services:
      - Spotify;
      - LastFm;
      - Apple;
      - Deezer;
      - SoundCloud (may require track id)
    - Obtaining and downloading metadata from third-party services;
      - Obtaining cover art;
      - Obtaining statistics;
      - Obtaining similar tracks;
      - Obtaining lyrics;
    - Translation of the track title, author, and, if possible, lyrics;
    - Forming quick network playlists;
  - Ability to use low-level audio processing.
    - Change low-level parameters:
      - Volume;
      - Tempo;
      - Pitch;
      - Pan;
    - Input normalizer the output volume;
  - Ability to:
    - Skip intro;
    - Skip outro;
    - Skip drop;
    - Skip pit;
  - Ability to play MP3, WAV, and other own formats included as mod;
  - Working with local dependencies
    - Localization;
    - Resource loading;
      - Unpackaged resources (for modification);
      - Resource bundles;
    - Configurability;
    - Tagging tracks, for categorize tracks by genre, feature, type, mood (basically, anything, since tags can be created)
    - Ability to create and load modifications;
    - Possibility to use track cache;
    - Restoring state:
      - Track:
        - tempo, volume, time, pan, (plugins and vst states in future)
      - Application data:
    - Complex parsing search;
      - Search for tags (starts with #) using AND/OR;
      - Search for tracks using annotations (specific search refinements - playlists, author, mood, tag, format, duration, etc.);

</details>

---

## Getting Started

### With your hands

1. Create project and move code into it
2. Add all dependencies per file structure
3. Use these **VM parameters**:

Launch parameters:
```c
-Dfile.encoding=UTF-8
--module-path
"libraries\modules"
--add-modules
javafx.controls,javafx.fxml,javafx.graphics,javafx.swing,javafx.media,org.controlsfx.controls,jdk.management,java.management,mp3agic,java.prefs,com.jfoenix,com.github.kwhat.jnativehook,jsoup,lastfm.java,java.desktop,com.sun.jna,com.sun.jna.platform,org.apache.commons.io
--add-opens
java.base/java.lang.reflect=com.jfoenix
--add-opens
javafx.graphics/javafx.stage=ALL-UNNAMED
--add-opens
javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED
--add-exports
junit.platform.console.standalone/org.hamcrest.internal=ALL-UNNAMED
```

### Intellij Idea
To build, simply open the project in IntelliJ IDEA.

### Eclipse
For Eclipse:
1) Create a project and move the code there.
2) Add local dependencies.
3) Insert VM options

---

## CI

Automatic Maven dependency download and workspace preparation is not available yet, but everything is possible!

| Tool       | Description                                               |
|------------|-----------------------------------------------------------|
| **Maven**  | Configuration: [`pom.xml`](./package/build/maven/pom.xml) |                                                                                                                                                |
---

## Prerequisites

### Java 19+
| Provider                               | Download    | Recommended     |
|----------------------------------------|-------------|-----------------|
| [Adoptium](https://adoptium.net/)      | **OpenJDK** | **Best choice** |
| [Oracle](https://www.oracle.com/java/) | JDK 19+     | Commercial      |

> **Add to PATH** and verify: `java --version`

---

### C++ Runtime
| Platform                                                  | Download                          | Recommended              |
|-----------------------------------------------------------|-----------------------------------|--------------------------|
| [Windows](https://aka.ms/vs/17/release/vc_redist.x64.exe) | **Visual C++ Redistributable**    | **Required**             |
| [Linux](https://www.gnu.org/software/libc/)               | **glibc** (usually pre-installed) | `sudo apt install libc6` |
| [macOS](https://developer.apple.com/xcode/)               | **Xcode Command Line Tools**      | `xcode-select --install` |

> **No compiler needed** - only runtime libraries for VST plugins

---

## Technical Stack

| Framework/Library                                            | Description                    |
|--------------------------------------------------------------|--------------------------------|
| [JavaFX 21](https://openjfx.io/)                             | **Main UI Framework**          | 
| [Maven](https://maven.apache.org/)                           | **Build Automation**           | 
| [JUnit 5](https://junit.org/)                                | **Unit Testing**               | 
| [Byte Buddy](https://bytebuddy.net/)                         | **Runtime Code Generation**    | 
| [AssertJ](https://assertj.github.io/doc/)                    | **Fluent Assertions**          | 
| [Mockito](https://site.mockito.org/)                         | **Mocking Framework**          |
| [JLayer](https://mvnrepository.com/artifact/javazoom/jlayer) | **MP3 Audio Decoding**         | 
| [MP3SPI](https://github.com/jsaw.mp3spi)                     | **MP3 Plugin for SPI**         | 
| [VST3 SDK](https://github.com/steinbergmedia/vst3sdk)        | **VST3 Plugin Standard**       | 
| [JVSTHost](https://github.com/mhroth/jvsthost)               | **VST Plugin Hosting**         | 
| [Spotify](https://developer.spotify.com/)                    | **Spotify for developers**     | 
| [Soundcloud](https://developers.soundcloud.com/)             | **Soundcloud for developers**  | 
| [LastFM SDK](https://www.last.fm/api)                        | **LastFM for developers**      | 
| [Apple](https://music.apple.com/ru/new)                      | **Apple Music for developers** | 

---

## System Requirements

### Supported Platforms
| OS             | Architecture     |
|----------------|------------------|
| **Windows 11** | x86 • **x86_64** |
| **Windows 10** | x86 • **x86_64** |
| Windows 8.1    | x86 • x86_64     |

### Technical Specs
| OS                 | **CPU**                                              | **RAM**   |
|--------------------|------------------------------------------------------|-----------|
| **Windows 11**     | i3-4160 / i3-6100 / AMD FX<br>2-4 cores @ **2.0GHz** | **1.5GB** |
| **Windows 10/8.1** | i3-8100 / Ryzen 3 2000<br>2 cores @ **2.0GHz**       | **1GB**   |

> VST plugins may require additional CPU/RAM

---
<div id='300'/>

## Issues

All resolved and (mostly) unresolved issues are listed in a directory of the same name.  
This allows us to keep track of issues (not just bugs) and resolve them over time, according to priority.

[Issues here](https://github.com/Rediska4445/mizuki-player/tree/main/issues)

---

## Modifications
  
### **Extend app with custom mods!**

### Create a Mod in 4 Steps

1. **Create a mod project** in your Java IDE
2. **Add the base app JAR** as a dependency
3. **Implement the `AudioMod` interface**:

```java
public class mod implements AudioMod {

  @Override
  public void applyMod() {
    Log.println("Mod Init");
  }
} 
```

4. **Build JAR and place** in the folder from `resources.properties`

---
