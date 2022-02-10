package com.geekbrains.cloud.client;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController implements Initializable {

    private static final int BUFFER_SIZE = 8192;

    public TextField clientPath;
    public TextField serverPath;
    public ListView<String> clientView;
    public ListView<String> serverView;
    private File currentDirectory;

    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;
    
    private String incomingServerPath;

    // Platform.runLater(() -> {})
    private void updateClientView() {
        Platform.runLater(() -> {
            clientPath.setText(currentDirectory.getAbsolutePath());
            clientView.getItems().clear();
            clientView.getItems().add("...");
            clientView.getItems()
                    .addAll(currentDirectory.list());
        });
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String item = serverView.getSelectionModel().getSelectedItem();
            os.writeUTF("#getFile_message#");
            os.writeUTF(item);
            if ("#isFile#".equals(is.readUTF())) {
                long size = is.readLong();
                File newFileFromServer = currentDirectory.toPath().
                        resolve(item).
                        toFile();
                try (OutputStream fos = new FileOutputStream(newFileFromServer)) {
                    for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                        int readCount = is.read(buf);
                        fos.write(buf, 0, readCount);
                    }
                }
            }
            System.out.println("File: " + item + " is downloaded!");
            updateClientView();
    }

    // upload file to server
    public void upload(ActionEvent actionEvent) throws IOException {
        String item = clientView.getSelectionModel().getSelectedItem();
        File selected = currentDirectory.toPath().resolve(item).toFile();
        if (selected.isFile()) {
            os.writeUTF("#file_message#");
            os.writeUTF(selected.getName());
            os.writeLong(selected.length());
            try (InputStream fis = new FileInputStream(selected)) {
                while (fis.available() > 0) {
                    int readBytes = fis.read(buf);
                    os.write(buf, 0, readBytes);
                }
            }
            os.flush();
        }
        //updateServerView();
    }

    private void initNetwork() {
        try {
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentDirectory = new File(System.getProperty("user.home"));


        // run in FX Thread
        // :: - method reference
        updateClientView();
        initNetwork();
        updateServerView();
        clientViewAction();
    }

    private void updateServerView() {
        Platform.runLater(() -> {
            serverView.getItems().clear();
            serverView.getItems().addAll(getServerDirectory());
            serverPath.setText(incomingServerPath);
        });
    }
    
    private ObservableList<String> getServerDirectory(){
        ObservableList<String> serverList = null;
        try {
            os.writeUTF("#getDirectory_message#");
            incomingServerPath = is.readUTF();
            String input = is.readUTF();
            serverList = FXCollections.
                    observableArrayList(input.
                            substring(1, input.length() - 1).
                            split(", "));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverList;
    }

    private void clientViewAction(){
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = clientView.getSelectionModel().getSelectedItem();
                if (item.equals("...")) {
                    currentDirectory = currentDirectory.getParentFile();
                    updateClientView();
                } else {
                    File selected = currentDirectory.toPath().resolve(item).toFile();
                    if (selected.isDirectory()) {
                        currentDirectory = selected;
                        updateClientView();
                    }
                }
            }
        });
    }
}
