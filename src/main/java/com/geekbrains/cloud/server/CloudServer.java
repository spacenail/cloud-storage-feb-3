package com.geekbrains.cloud.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CloudServer {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8189);
        System.out.println("Started!");
        while (true) {
            Socket socket = server.accept();
            new Thread(new CloudFileHandler(socket)).start();
        }
    }

}
