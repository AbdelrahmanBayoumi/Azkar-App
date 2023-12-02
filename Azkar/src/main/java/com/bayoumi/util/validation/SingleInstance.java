package com.bayoumi.util.validation;

import com.bayoumi.util.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author Bayoumi
 */
public class SingleInstance {

    private static SingleInstance singleInstance = null;
    private final int PORT = 12347;
    private ServerSocket server;
    private Stage currentStage;

    private SingleInstance() {
        singleInstanceApplicationCheck();
    }

    public static SingleInstance getInstance() {
        if (singleInstance == null) {
            singleInstance = new SingleInstance();
        }
        return singleInstance;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    private void singleInstanceApplicationCheck() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            Logger.debug("InetAddress.getLocalHost(): " + localAddress + ":" + PORT);
            server = new ServerSocket(PORT, 1, localAddress);
            Logger.debug("Server Online ...");
            // listen to other Instances
            new Thread(() -> {
                try {
                    while (true) {
                        Socket serverClient = server.accept();  //server accept the client connection request
                        listenToInstance(serverClient);
                    }
                } catch (Exception ex) {
                    Logger.error("Error in => listen to other Instances", ex, getClass().getName() + ".singleInstanceApplicationCheck()");
                }
            }).start();
        } catch (IOException iOException) {
            // port is taken -> there is a running instance
            // notify the running instance to be on Top
            sendToServer();
        } catch (Exception ex) {
            Platform.runLater(this::showAlreadyRunningError);
        }
    }

    private void listenToInstance(Socket serverClient) {
        try {
            String clientMessage;
            Scanner scan = new Scanner(serverClient.getInputStream());
            while (scan.hasNext()) {
                clientMessage = scan.next();
                Logger.debug("Other Instance: " + clientMessage);
                Platform.runLater(this::openCurrentStage);
            }
        } catch (IOException ex) {
            Logger.error("Error in => listen to other Instances => ", ex, getClass().getName() + ".listenToInstance()");
        }
    }

    private void sendToServer() {
        try {
            final Socket socket = new Socket(InetAddress.getLocalHost(), PORT);
            Logger.debug("Connection Success ...");
            try {
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                String clientMessage = "1";
                pw.println(clientMessage);
                System.exit(0);
            } catch (Exception e) {
                Logger.error("Exception in sending to server", e, getClass().getName() + "sendToServer()");
            }
        } catch (Exception e) {
            Logger.error("Exception in Connecting to server : ", e, getClass().getName() + "sendToServer()");
            Platform.runLater(this::showAlreadyRunningError);
        }
    }

    private void showAlreadyRunningError() {
        // catch anything unexpected !
        Logger.debug("Program already running, exiting");
        Alert warning = new Alert(Alert.AlertType.WARNING);
        warning.setHeaderText("Program already running, exiting");
        warning.showAndWait();
        System.exit(0);
    }

    public void openCurrentStage() {
        Platform.runLater(() -> {
            if (currentStage == null) {
                Logger.debug("null");
                System.exit(0);
            } else if (currentStage.isIconified()) {
                Logger.debug("isIconified");
                currentStage.setIconified(false);
            } else if (!currentStage.isShowing()) {
                Logger.debug("not isShowing");
                currentStage.show();
                currentStage.toFront();
            } else {
                Logger.debug("setAlwaysOnTop");
                currentStage.setAlwaysOnTop(true);
                currentStage.setAlwaysOnTop(false);
            }
        });
    }
}
