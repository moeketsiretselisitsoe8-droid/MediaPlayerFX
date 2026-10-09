# JavaFX Media Player - BIIM3210 Assignment 2

## Project layout
```
MediaPlayerFX/
├── pom.xml                      <- includes the javafx-media dependency
├── media/                       <- all media lives here (loaded into the playlist at start-up)
│   ├── logo.jpg                 <- university logo taken from the assignment document
│   ├── sample_audio_1.mp3
│   ├── sample_audio_2.mp3
│   └── sample_video.mp4
└── src/main/
    ├── java/com/limkokwing/mediaplayer/MediaPlayerApp.java
    └── resources/style.css
```

## Run (JDK 17+ and Maven required)
```
cd MediaPlayerFX
mvn clean javafx:run
```
Run it from the project folder so the app can find the `media/` folder.
In IntelliJ / Eclipse / NetBeans: open the folder as a Maven project and run `MediaPlayerApp`
(set the working directory to the project root).

## Key controls
| Key | Action |
|-----|--------|
| Space | Play / Pause |
| S | Stop |
| N / P | Next / Previous item |
| Up / Down | Volume +5% / -5% |
| M | Mute / Unmute |
| Left / Right | Seek -5s / +5s |
| Ctrl+O | Add files (FileChooser) |
| Delete | Remove selected playlist item |
| Enter (in playlist) | Play selected item |

Add your own mp3 / mp4 / wav / m4a files by copying them into `media/` or with **Add files...**.
Supported formats are those supported by JavaFX Media (MP3, AAC, WAV, AIFF, MP4 with H.264/AAC, FLV).
