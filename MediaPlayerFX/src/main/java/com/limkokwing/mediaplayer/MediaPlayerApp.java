package com.limkokwing.mediaplayer;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * BIIM3210 - Assignment 2: Media Player in JavaFX.
 *
 * Part A: MediaView + Play / Pause / Stop buttons
 * Part B: Keyboard controls (Space, S, N, P, Up, Down, M  + extras)
 * Part C: Playlist (ListView) with add (FileChooser) and remove
 */
public class MediaPlayerApp extends Application {

    private static final List<String> SUPPORTED = List.of(
            ".mp3", ".mp4", ".wav", ".m4a", ".m4v", ".aac", ".aif", ".aiff", ".flv", ".fxm");
    private static final double VOLUME_STEP = 5;   // percent
    private static final double SEEK_STEP = 5;     // seconds

    // ---- model ----
    private final ObservableList<File> playlist = FXCollections.observableArrayList();
    private MediaPlayer player;
    private int currentIndex = -1;
    private double volume = 0.7;
    private boolean muted = false;
    private File mediaDir;
    private File lastDir;

    // ---- UI ----
    private Stage stage;
    private final ListView<File> listView = new ListView<>(playlist);
    private final MediaView mediaView = new MediaView();
    private final VBox placeholder = new VBox(14);
    private final Label nowPlaying = new Label("Nothing playing");
    private final Slider seekSlider = new Slider(0, 1, 0);
    private final Slider volumeSlider = new Slider(0, 100, 70);
    private final Label timeLabel = new Label("00:00 / 00:00");
    private final Label volumeLabel = new Label("70%");
    private final Label statusLabel = new Label("Ready");
    private final Button muteButton = new Button("Mute");

    // =====================================================================
    //  Start-up
    // =====================================================================
    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        mediaDir = findMediaDir();

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildStage());
        root.setRight(buildPlaylistPane());
        root.setBottom(buildControls());

        Scene scene = new Scene(root, 1050, 650);
        var css = getClass().getResource("/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        // Event FILTER so the shortcuts work no matter which control has focus.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);

        stage.setTitle("BIIM3210 - JavaFX Media Player");
        stage.setMinWidth(820);
        stage.setMinHeight(520);
        Image logo = loadLogo();
        if (logo != null) {
            stage.getIcons().add(logo);
        }
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> disposePlayer());
        stage.show();

        loadDefaultPlaylist();
        root.requestFocus();
    }

    // =====================================================================
    //  UI construction
    // =====================================================================
    private Region buildHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);

        Image logo = loadLogo();
        if (logo != null) {
            ImageView iv = new ImageView(logo);
            iv.setFitHeight(44);
            iv.setPreserveRatio(true);
            header.getChildren().add(iv);
        }
        Label title = new Label("Interactive Multimedia - Media Player");
        title.getStyleClass().add("header-title");
        Label sub = new Label("BIIM3210 - Assignment 2");
        sub.getStyleClass().add("header-sub");
        header.getChildren().add(new VBox(2, title, sub));
        return header;
    }

    private Region buildStage() {
        mediaView.setPreserveRatio(true);
        mediaView.setVisible(false);

        // Shown instead of the video when playing audio / when idle.
        placeholder.setAlignment(Pos.CENTER);
        Image logo = loadLogo();
        if (logo != null) {
            ImageView iv = new ImageView(logo);
            iv.setFitWidth(260);
            iv.setPreserveRatio(true);
            StackPane card = new StackPane(iv);
            card.getStyleClass().add("logo-card");
            card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            placeholder.getChildren().add(card);
        }
        nowPlaying.getStyleClass().add("now-playing");
        placeholder.getChildren().add(nowPlaying);

        StackPane stagePane = new StackPane(placeholder, mediaView);
        stagePane.getStyleClass().add("stage-pane");
        stagePane.setMinSize(0, 0);
        stagePane.setPrefSize(700, 400);
        mediaView.fitWidthProperty().bind(stagePane.widthProperty());
        mediaView.fitHeightProperty().bind(stagePane.heightProperty());
        return stagePane;
    }

    private Region buildPlaylistPane() {
        Label title = new Label("Playlist");
        title.getStyleClass().add("playlist-title");

        listView.setPlaceholder(new Label("Playlist is empty.\nPress Ctrl+O to add files."));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    String marker = (getIndex() == currentIndex) ? "\u25B6  " : "     ";
                    setText(marker + (getIndex() + 1) + ". " + f.getName());
                }
            }
        });
        // Selecting an item with the mouse updates the player automatically.
        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                int sel = listView.getSelectionModel().getSelectedIndex();
                if (sel >= 0 && (sel != currentIndex || e.getClickCount() == 2)) {
                    playIndex(sel);
                }
            }
        });

        Button add = button("Add files...", "Add files to the playlist (Ctrl+O)");
        add.setOnAction(e -> addFiles());
        Button remove = button("Remove", "Remove the selected item (Delete)");
        remove.setOnAction(e -> removeSelected());
        add.setMaxWidth(Double.MAX_VALUE);
        remove.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(add, Priority.ALWAYS);
        HBox.setHgrow(remove, Priority.ALWAYS);

        VBox pane = new VBox(8, title, listView, new HBox(8, add, remove));
        VBox.setVgrow(listView, Priority.ALWAYS);
        pane.setPadding(new Insets(12));
        pane.setPrefWidth(300);
        return pane;
    }

    private Region buildControls() {
        // seek row
        seekSlider.setFocusTraversable(false);
        HBox.setHgrow(seekSlider, Priority.ALWAYS);
        timeLabel.getStyleClass().add("time-label");
        HBox seekRow = new HBox(10, seekSlider, timeLabel);
        seekRow.setAlignment(Pos.CENTER);
        seekSlider.valueProperty().addListener((obs, oldV, newV) -> {
            // user clicked the track / dragged / used keys -> seek
            if (player != null && Math.abs(newV.doubleValue() - player.getCurrentTime().toSeconds()) > 1.0) {
                player.seek(Duration.seconds(newV.doubleValue()));
            }
        });

        // buttons
        Button prev = button("Previous", "Previous item (P)");
        prev.setOnAction(e -> playPrevious());
        Button play = button("Play", "Play (Space toggles Play/Pause)");
        play.setOnAction(e -> play());
        Button pause = button("Pause", "Pause (Space toggles Play/Pause)");
        pause.setOnAction(e -> pause());
        Button stop = button("Stop", "Stop (S)");
        stop.setOnAction(e -> stopPlayback());
        Button next = button("Next", "Next item (N)");
        next.setOnAction(e -> playNext());
        muteButton.setFocusTraversable(false);
        muteButton.setTooltip(new Tooltip("Mute / Unmute (M)"));
        muteButton.setOnAction(e -> toggleMute());

        // volume
        volumeSlider.setPrefWidth(140);
        volumeSlider.setFocusTraversable(false);
        volumeSlider.setTooltip(new Tooltip("Volume (Up / Down arrows)"));
        volumeSlider.valueProperty().addListener((obs, oldV, newV) -> {
            volume = newV.doubleValue() / 100.0;
            volumeLabel.setText(Math.round(newV.doubleValue()) + "%");
            if (player != null) {
                player.setVolume(volume);
            }
        });
        volumeLabel.getStyleClass().add("time-label");
        volumeLabel.setMinWidth(40);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, prev, play, pause, stop, next, spacer,
                muteButton, volumeSlider, volumeLabel);
        buttons.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("status-label");
        Label help = new Label("Keys:  Space = Play/Pause   S = Stop   N = Next   P = Previous   "
                + "Up/Down = Volume   M = Mute   Left/Right = Seek 5s   Ctrl+O = Add   Delete = Remove");
        help.getStyleClass().add("help-label");

        VBox controls = new VBox(8, seekRow, buttons, statusLabel, help);
        controls.getStyleClass().add("controls");
        return controls;
    }

    private Button button(String text, String tip) {
        Button b = new Button(text);
        b.setFocusTraversable(false);   // keep keyboard focus off buttons so keys stay predictable
        b.setTooltip(new Tooltip(tip));
        b.setMinWidth(80);
        return b;
    }

    // =====================================================================
    //  Keyboard (Part B)
    // =====================================================================
    private void handleKey(KeyEvent e) {
        if (e.isShortcutDown()) {
            if (e.getCode() == KeyCode.O) {
                addFiles();
                e.consume();
            }
            return;
        }
        if (e.isAltDown()) {
            return;
        }
        boolean handled = true;
        switch (e.getCode()) {
            case SPACE -> togglePlayPause();
            case S -> stopPlayback();
            case N -> playNext();
            case P -> playPrevious();
            case UP -> adjustVolume(VOLUME_STEP);
            case DOWN -> adjustVolume(-VOLUME_STEP);
            case M -> toggleMute();
            case RIGHT -> seekBy(SEEK_STEP);
            case LEFT -> seekBy(-SEEK_STEP);
            case DELETE -> removeSelected();
            case ENTER -> {
                if (listView.isFocused()) {
                    playSelected();
                } else {
                    handled = false;
                }
            }
            default -> handled = false;
        }
        if (handled) {
            e.consume();
        }
    }

    // =====================================================================
    //  Playback (Part A)
    // =====================================================================
    private void playIndex(int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }
        disposePlayer();
        currentIndex = index;
        File file = playlist.get(index);
        listView.getSelectionModel().select(index);
        listView.scrollTo(index);
        listView.refresh();
        nowPlaying.setText(file.getName());

        try {
            Media media = new Media(file.toURI().toString());
            MediaPlayer mp = new MediaPlayer(media);
            mp.setVolume(volume);
            mp.setMute(muted);

            media.setOnError(() -> showError(file, media.getError()));
            mp.setOnError(() -> showError(file, mp.getError()));

            mp.setOnReady(() -> {
                Duration total = mp.getTotalDuration();
                seekSlider.setMax(total.isUnknown() ? 1 : Math.max(1, total.toSeconds()));
                boolean hasVideo = media.getWidth() > 0 && media.getHeight() > 0;
                mediaView.setVisible(hasVideo);
                placeholder.setVisible(!hasVideo);
                updateTime(mp.getCurrentTime());
            });
            mp.currentTimeProperty().addListener((obs, oldT, newT) -> {
                if (!seekSlider.isValueChanging()) {
                    seekSlider.setValue(newT.toSeconds());
                }
                updateTime(newT);
            });
            mp.statusProperty().addListener((obs, oldS, newS) -> updateStatus(file, newS));
            mp.setOnEndOfMedia(() -> {
                if (playlist.size() > 1) {
                    playNext();
                } else {
                    stopPlayback();
                }
            });

            mediaView.setMediaPlayer(mp);
            player = mp;
            mp.play();
        } catch (Exception ex) {
            showError(file, ex);
        }
    }

    private void play() {
        if (player == null) {
            playSelected();
        } else {
            player.play();
        }
    }

    private void pause() {
        if (player != null && player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        }
    }

    private void togglePlayPause() {
        if (player == null) {
            playSelected();
        } else if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else {
            player.play();
        }
    }

    private void stopPlayback() {
        if (player != null) {
            player.stop();
            seekSlider.setValue(0);
            updateTime(Duration.ZERO);
        }
    }

    private void playSelected() {
        if (playlist.isEmpty()) {
            statusLabel.setText("Playlist is empty - press Ctrl+O to add files.");
            return;
        }
        playIndex(Math.max(0, listView.getSelectionModel().getSelectedIndex()));
    }

    private void playNext() {
        if (playlist.isEmpty()) {
            return;
        }
        playIndex(currentIndex < 0 ? 0 : (currentIndex + 1) % playlist.size());
    }

    private void playPrevious() {
        if (playlist.isEmpty()) {
            return;
        }
        playIndex(currentIndex < 0 ? 0 : (currentIndex - 1 + playlist.size()) % playlist.size());
    }

    private void seekBy(double seconds) {
        if (player == null) {
            return;
        }
        Duration total = player.getTotalDuration();
        double target = player.getCurrentTime().toSeconds() + seconds;
        double max = total.isUnknown() ? Double.MAX_VALUE : total.toSeconds();
        player.seek(Duration.seconds(Math.max(0, Math.min(max, target))));
    }

    private void adjustVolume(double deltaPercent) {
        if (muted) {
            toggleMute();   // changing the volume un-mutes
        }
        volumeSlider.setValue(Math.max(0, Math.min(100, volumeSlider.getValue() + deltaPercent)));
    }

    private void toggleMute() {
        muted = !muted;
        muteButton.setText(muted ? "Unmute" : "Mute");
        if (player != null) {
            player.setMute(muted);
        }
        statusLabel.setText(muted ? "Muted" : "Unmuted");
    }

    private void disposePlayer() {
        if (player != null) {
            player.stop();
            player.dispose();
            player = null;
        }
        mediaView.setMediaPlayer(null);
        mediaView.setVisible(false);
        placeholder.setVisible(true);
        seekSlider.setValue(0);
        updateTime(Duration.ZERO);
    }

    // =====================================================================
    //  Playlist management (Part C)
    // =====================================================================
    private void addFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Add media files to the playlist");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media files",
                        "*.mp3", "*.mp4", "*.wav", "*.m4a", "*.m4v", "*.aac", "*.aif", "*.aiff", "*.flv"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File start = lastDir != null ? lastDir : mediaDir;
        if (start != null && start.isDirectory()) {
            chooser.setInitialDirectory(start);
        }
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) {
            return;
        }
        lastDir = files.get(0).getParentFile();

        int firstNew = playlist.size();
        for (File f : files) {
            if (!playlist.contains(f)) {
                playlist.add(f);
            }
        }
        int added = playlist.size() - firstNew;
        statusLabel.setText(added + " file(s) added to the playlist.");
        if (added > 0 && player == null) {
            playIndex(firstNew);    // nothing was loaded yet -> start with the first new file
        }
    }

    private void removeSelected() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            statusLabel.setText("Select an item in the playlist to remove it.");
            return;
        }
        File removed = playlist.remove(idx);
        statusLabel.setText("Removed: " + removed.getName());

        if (idx < currentIndex) {
            currentIndex--;
        } else if (idx == currentIndex) {
            disposePlayer();
            currentIndex = -1;
            nowPlaying.setText("Nothing playing");
            if (!playlist.isEmpty()) {
                playIndex(Math.min(idx, playlist.size() - 1));   // player updates to the next item
                return;
            }
        }
        listView.refresh();
    }

    private void loadDefaultPlaylist() {
        if (mediaDir == null) {
            statusLabel.setText("No 'media' folder found - use Ctrl+O to add files.");
            return;
        }
        File[] files = mediaDir.listFiles(f -> f.isFile() && isSupported(f));
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(f -> f.getName().toLowerCase()));
        playlist.addAll(files);
        listView.getSelectionModel().select(0);
        statusLabel.setText("Loaded " + files.length + " file(s) from the media folder. Press Space to play.");
    }

    // =====================================================================
    //  Helpers
    // =====================================================================
    private void updateTime(Duration current) {
        Duration total = (player != null) ? player.getTotalDuration() : Duration.ZERO;
        timeLabel.setText(format(current) + " / " + format(total));
    }

    private void updateStatus(File file, MediaPlayer.Status status) {
        switch (status) {
            case PLAYING -> statusLabel.setText("Playing: " + file.getName());
            case PAUSED -> statusLabel.setText("Paused: " + file.getName());
            case STOPPED -> statusLabel.setText("Stopped");
            case STALLED -> statusLabel.setText("Buffering...");
            default -> { }
        }
    }

    private void showError(File file, Throwable error) {
        String msg = (error == null || error.getMessage() == null) ? "unknown error" : error.getMessage();
        statusLabel.setText("Cannot play " + file.getName() + ": " + msg);
    }

    private static String format(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) {
            return "--:--";
        }
        int s = (int) Math.floor(d.toSeconds());
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private static boolean isSupported(File f) {
        String n = f.getName().toLowerCase();
        return SUPPORTED.stream().anyMatch(n::endsWith);
    }

    private File findMediaDir() {
        File[] candidates = {
                new File("media"),
                new File(System.getProperty("user.dir"), "media"),
                new File("..", "media")
        };
        for (File c : candidates) {
            if (c.isDirectory()) {
                return c.getAbsoluteFile();
            }
        }
        return null;
    }

    private Image loadLogo() {
        if (mediaDir == null) {
            return null;
        }
        File logo = new File(mediaDir, "logo.jpg");
        return logo.isFile() ? new Image(logo.toURI().toString()) : null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
