package com.geekbrains.cloud.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EchoServerNio {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer byteBuffer;

    public EchoServerNio() {
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
    }

    private void handleRead(SelectionKey currentKey) throws IOException {
        SocketChannel channel = (SocketChannel) currentKey.channel();
        StringBuilder reader = new StringBuilder();


        while (true) {
            int count = channel.read(byteBuffer);

            if (count == 0) { // если данных нет, выходим из цикла
                break;
            }

            if (count == -1) { // если EOS(прервано соединение) закрываем канал
                channel.close();
                return;
            }

            byteBuffer.flip(); // после записи из потока в буфер меняем его режим на чтение

            while (byteBuffer.hasRemaining()) { // пока есть элементы в буфере
                reader.append((char) byteBuffer.get()); // добавляем прочтенные символы в StringBuilder
            }

            byteBuffer.clear();
        }

        String msg = "From server: " + reader.toString();
        System.out.println("Received: " + msg);
        channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
    }


    public static void main(String[] args) {
        new EchoServerNio();
    }
}
