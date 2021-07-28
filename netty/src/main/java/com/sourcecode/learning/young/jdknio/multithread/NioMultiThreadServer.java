package com.sourcecode.learning.young.jdknio.multithread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author yangjw
 * @date 2021/7/2510:36
 */
public class NioMultiThreadServer {

    private static Selector selector = null;
    private static Queue<Task> taskQueen = new ConcurrentLinkedQueue<>();

    public static void addQueen(Task key) {
        taskQueen.add(key);
        selector.wakeup();
    }

    public void start() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress("localhost", 9041));
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.currentThread().isInterrupted()) {
            int num = selector.select();
            if (num > 0) {
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
                        key.cancel();
                        Processor.read(key);
                    } else if (key.isWritable()) {
                        key.cancel();
                        Processor.write(key);
                    }
                }
            } else {
                Task task;
                while ((task = taskQueen.poll()) != null) {
                    SelectionKey key = task.getKey();
                    //注册写事件
                    SocketChannel channel = (SocketChannel) key.channel();
                    Object attachment = key.attachment();
                    channel.register(selector, task.operator, attachment);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("a new client connected " + clientChannel.getRemoteAddress());
    }

    public static class Task {
        private SelectionKey key;
        private int operator;

        public Task(SelectionKey key, int operator) {
            this.key = key;
            this.operator = operator;
        }

        public int getOperator() {
            return operator;
        }

        public SelectionKey getKey() {
            return key;
        }
    }

    public static class Processor {
        //构造线程池
        private static ExecutorService executorService = Executors.newFixedThreadPool(10);

        public static void write(final SelectionKey key) {
            //拿到线程并执行
            executorService.submit(() -> {
                try {
                    // 写操作
                    SocketChannel writeChannel = (SocketChannel) key.channel();
                    //拿到客户端传递的数据
                    String message = (String) key.attachment();
                    System.out.println("write back to client message:" + message);
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    buffer.put(message.getBytes());
                    buffer.flip();
                    writeChannel.write(buffer);
                    NioMultiThreadServer.addQueen(new Task(key, SelectionKey.OP_READ));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        public static void read(final SelectionKey key) {
            //获得线程并执行
            executorService.submit(() -> {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                SocketChannel readChannel = null;
                try {
                    readChannel = (SocketChannel) key.channel();
                    readBuffer.clear();
                    int numRead;
                    numRead = readChannel.read(readBuffer);
                    String message = new String(readBuffer.array(), 0, numRead);
                    System.out.println("accept " + readChannel.getRemoteAddress().toString() + " client message:" + message);
                    //将数据添加到key中
                    key.attach(message);
                    //将注册写操作添加到队列中
                    NioMultiThreadServer.addQueen(new Task(key, SelectionKey.OP_WRITE));
                } /*catch (NotYetConnectedException nce) {

                }*/ catch (IOException e) {
                    // 简单弄，就不分步去固定捕获read的异常了。
                    try {
                        System.out.println(readChannel.getRemoteAddress().toString() + " is disconnected");
                        readChannel.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });
        }

    }

    public static void main(String[] args) throws IOException {
        new NioMultiThreadServer().start();
    }
}
