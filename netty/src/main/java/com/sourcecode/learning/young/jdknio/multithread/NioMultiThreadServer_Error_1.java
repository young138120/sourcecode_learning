package com.sourcecode.learning.young.jdknio.multithread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yangjw
 * @date 2021/7/2510:36
 */
public class NioMultiThreadServer_Error_1 {
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);//调整缓存的大小可以看到打印输出的变化
    private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);//调整缓存的大小可以看到打印输出的变化
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    String str;

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress("localhost", 8001));
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    private void write(SelectionKey key) {
        executorService.execute(() -> {
            SocketChannel channel = (SocketChannel) key.channel();
            System.out.println("write back to client message:" + str);
            sendBuffer.clear();
            sendBuffer.put(str.getBytes());
            sendBuffer.flip();
            try {
                channel.write(sendBuffer);
                channel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void read(SelectionKey key) {
        executorService.execute(() -> {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            this.readBuffer.clear();
            int numRead;
            try {
                numRead = socketChannel.read(this.readBuffer);
                str = new String(readBuffer.array(), 0, numRead);
                System.out.println("accept " + socketChannel.getRemoteAddress().toString() + " client message:" + str);
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            } catch (IOException e) {
                e.printStackTrace();
                key.cancel();
                try {
                    socketChannel.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                return;
            }
        });
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("a new client connected " + clientChannel.getRemoteAddress());
    }

    public static void main(String[] args) throws IOException {
        new NioMultiThreadServer_Error_1().start();
    }
}
