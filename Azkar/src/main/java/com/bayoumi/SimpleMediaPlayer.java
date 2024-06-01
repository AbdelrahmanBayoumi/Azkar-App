package com.bayoumi;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.File;

public class SimpleMediaPlayer extends Application {

    @Override
    public void start(Stage primaryStage) {
        String audioUrl = "http://www.hisnmuslim.com/audio/ar/101.mp3";
        File audioFile = new File("downloaded_audio.mp3");
        if (audioFile.delete()) {
            System.out.println("File deleted successfully");
        }

        // Download the audio file
        if (downloadFile(audioUrl, audioFile)) {
            // Create a Media object
            Media media = new Media(audioFile.toURI().toString());

            // Create a MediaPlayer object
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            // Error handling
            mediaPlayer.setOnError(() -> {
                System.out.println("Error: " + mediaPlayer.getError().getMessage());
                showAlert("Media Player Error", "Could not play media", mediaPlayer.getError().getMessage());
            });

            // MediaPlayer status handling
            mediaPlayer.setOnReady(() -> System.out.println("Media is ready to play."));
            mediaPlayer.setOnPlaying(() -> System.out.println("Media is playing."));
            mediaPlayer.setOnEndOfMedia(() -> System.out.println("End of media."));
            mediaPlayer.setOnStopped(() -> System.out.println("Media stopped."));
            mediaPlayer.setOnPaused(() -> System.out.println("Media paused."));

            // Create Play button
            Button playButton = new Button("Play");
            playButton.setOnAction(e -> mediaPlayer.play());

            // Create Pause button
            Button pauseButton = new Button("Pause");
            pauseButton.setOnAction(e -> mediaPlayer.pause());

            // Create Stop button
            Button stopButton = new Button("Stop");
            stopButton.setOnAction(e -> mediaPlayer.stop());

            // Add buttons to a layout container
            VBox root = new VBox(10, playButton, pauseButton, stopButton);

            // Create a scene with the layout
            Scene scene = new Scene(root, 300, 200);

            // Set up the stage
            primaryStage.setTitle("Simple Media Player");
            primaryStage.setScene(scene);
            primaryStage.show();
        } else {
            showAlert("Download Error", "Could not download media file", "Check your internet connection and try again.");
        }
    }

    private boolean downloadFile(String fileURL, File destinationFile) {
        try {
            HttpResponse<File> response = Unirest.get(fileURL).asFile(destinationFile.getPath());
            if (response.getStatus() == 200) {
                return true;
            } else {
                System.out.println("Server returned non-OK status: " + response.getStatus());
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error downloading file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
