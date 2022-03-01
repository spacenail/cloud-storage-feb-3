package com.geekbrains.cloud.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TerminalServer {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer byteBuffer;
    private String welcomeMsg;
    private final String START_SYMBOL = "-> ";
    private String HELP_MESSAGE;
    private String UNKNOWN_COMMAND;
    private Path serverDirectory;

    public TerminalServer() {
        serverDirectory = Paths.get(".","server");
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome in Mike terminal").append("\r\n\r\n")
                .append("input --help to show command list")
                .append("\r\n\r\n")
                .append(START_SYMBOL);
        welcomeMsg = sb.toString();

        sb.delete(0,sb.capacity());
        sb.append("Commands:\r\n").append("1. ls - выводит список файлов на экран\r\n")
                .append("2. cd path - перемещается из текущей папки в папку из аргумента\r\n")
                .append("3. cat file - печатает содержание текстового файла на экран\r\n")
                .append("4. mkdir dir - создает папку в текущей директории\r\n")
                .append("5. touch file - создает пустой файл в текущей директории\r\n")
                .append(START_SYMBOL);
        HELP_MESSAGE = sb.toString();

        sb.delete(0,sb.capacity());
        sb.append("Unknown command\r\n").append(START_SYMBOL);
        UNKNOWN_COMMAND = sb.toString();


        try {
            byteBuffer = ByteBuffer.allocate(10);
            selector = Selector.open();

            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8189));
            serverSocketChannel.configureBlocking(false); // для использования преимуществ селектора
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); //селект будет отслеживать события, когда сервер принимает подключение

            while (serverSocketChannel.isOpen()){
                selector.select(); // отслеживаем события

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()){
                    SelectionKey currentKey = iterator.next();
                    if(currentKey.isAcceptable()) {
                        handleAccept();
                    }
                    if (currentKey.isReadable()){
                        handleRead(currentKey);
                    }
                    iterator.remove();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client accepted...");
        sendWelcomeMessage(socketChannel);
    }

    private void sendWelcomeMessage(SocketChannel socketChannel) throws IOException {
        socketChannel.write(ByteBuffer.wrap(welcomeMsg.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey currentKey) throws IOException {
        SocketChannel channel = (SocketChannel) currentKey.channel();
        List<Byte> byteList = new ArrayList<>();

        while (true) {
            int count = channel.read(byteBuffer); // читаем из сокета количество байт равное buffer.remaining()

            if (count == 0) { // если данных нет, выходим из цикла
                break;
            }

            if (count == -1) { // если EOS(прервано соединение) закрываем канал
                channel.close();
                return;
            }

            byteBuffer.flip(); // после записи из потока в буфер меняем его режим на чтение

            while (byteBuffer.hasRemaining()) { // пока есть элементы в буфере считываем по одному в список
                byteList.add(byteBuffer.get());
            }

            byteBuffer.clear(); //очищаем буфер после каждого цикла чтения-записи
        }

        byte[] byteArray = new byte[byteList.size()];
        int i = 0;
        for(Byte b:byteList){ // Byte[] -> byte[]
            byteArray[i++] = b.byteValue();
        }

        String clearMsg = new String(byteArray, StandardCharsets.UTF_8).replaceAll("\\r\\n", "");
        String[] splitArrays = clearMsg.split("\\s",2);

        switch (splitArrays[0]){
            case ("--help"):
                channel.write(ByteBuffer.wrap(HELP_MESSAGE.getBytes(StandardCharsets.UTF_8)));
                break;
            case ("ls"):
                channel.write(ByteBuffer.wrap(getList(serverDirectory).toString().getBytes(StandardCharsets.UTF_8)));
                break;
            case ("cd"):
                if(splitArrays.length > 1) {
                    File file = new File(splitArrays[1]);
                    if (file.exists()) {
                        serverDirectory = file.toPath();
                    }
                }
                channel.write(ByteBuffer.wrap(START_SYMBOL.getBytes(StandardCharsets.UTF_8)));
                break;
            case ("cat"):
                if(splitArrays.length > 1) {
                    File file = new File(splitArrays[1]);
                    if (file.exists()) {
                        channel.write(ByteBuffer.wrap(getList(file.toPath()).toString().getBytes(StandardCharsets.UTF_8)));
                    } else channel.write(ByteBuffer.wrap(START_SYMBOL.getBytes(StandardCharsets.UTF_8)));
                }else channel.write(ByteBuffer.wrap(START_SYMBOL.getBytes(StandardCharsets.UTF_8)));
                break;

            case (""):
                channel.write(ByteBuffer.wrap(START_SYMBOL.getBytes(StandardCharsets.UTF_8)));
                break;
            default:
                channel.write(ByteBuffer.wrap(UNKNOWN_COMMAND.getBytes(StandardCharsets.UTF_8)));
        }

    }

    private StringBuilder getList(Path path){
        String[] catalog = path.toFile().list();
        StringBuilder stringBuilder = new StringBuilder();
        for (String file:catalog){
            stringBuilder.append(file).append("\r\n");
        }
        stringBuilder.append(START_SYMBOL);
        return stringBuilder;
    }

    public static void main(String[] args) {
        new TerminalServer();
    }
}
