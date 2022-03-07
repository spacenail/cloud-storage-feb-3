package com.geekbrains.cloud.server;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;

public class CloudFileHandler implements Runnable {

    private static final int BUFFER_SIZE = 8192;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final byte[] buf;
    private File serverDirectory;

    public CloudFileHandler(Socket socket) throws IOException {
        System.out.println("Client connected!");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[BUFFER_SIZE];
        serverDirectory = new File("server");
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                if ("#file_message#".equals(command)) {
                    uploadFile();
                } else if ("#getDirectory_message#".equals(command)) {
                    sendDirectory();
                } else if("#getPath_message#".equals(command)){
                    sendPath();
                } else if ("#getFile_message#".equals(command)) {
                    String file_name = is.readUTF();
                    if (isFile(file_name)) {
                        os.writeUTF("#isFile#");
                        sendFile(file_name);
                    } else {
                        os.writeUTF("#isNotAFile#");
                    }
                } else {
                    System.err.println("Unknown command: " + command);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String file_name) throws IOException {
        File selected = serverDirectory.toPath().
                resolve(file_name).
                toFile();
        os.writeLong(selected.length());
        try (InputStream fis = new FileInputStream(selected)) {
            while (fis.available() > 0) {
                int readBytes = fis.read(buf);
                os.write(buf, 0, readBytes);
            }
        }
        os.flush();
        System.out.println("File: " + file_name + " sent to client!");
    }

    private void uploadFile() throws IOException {
        String name = is.readUTF();
        long size = is.readLong();
        File newFile = serverDirectory.toPath()
                .resolve(name)
                .toFile();
        try (OutputStream fos = new FileOutputStream(newFile)) {
            for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                int readCount = is.read(buf);
                fos.write(buf, 0, readCount);
            }
        }
        System.out.println("File: " + name + " is uploaded");
    }

    private void sendDirectory() throws IOException {
        os.writeUTF(Arrays.toString(serverDirectory.list()));
    }

    private void sendPath() throws IOException{
        os.writeUTF(serverDirectory.getPath());
    }

    private boolean isFile(String file_name) {
        return serverDirectory.toPath()
                .resolve(file_name).
                        toFile().
                        isFile();
    }
}
