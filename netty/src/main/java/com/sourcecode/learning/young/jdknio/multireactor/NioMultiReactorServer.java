package com.sourcecode.learning.young.jdknio.multireactor;

import com.sourcecode.learning.young.jdknio.multithread.NioMultiThreadServer.*;

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
 * @date 2021/7/2815:33
 */
public class NioMultiReactorServer {

    public static void main(String[] args) throws IOException {
        new MainReactor().start();
    }

    static class MainReactor {
        private static int subSize = 5;
        private static ExecutorService subPool = Executors.newFixedThreadPool(subSize);
        private static SubReactor[] subReactors = new SubReactor[subSize];
        private static Selector selector = null;

        public MainReactor() throws IOException {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.bind(new InetSocketAddress("localhost", 9041));
            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        }

        public void start() throws IOException {
            for (int i = 0; i < subSize; i++) {
                SubReactor subReactor = new SubReactor();
                subReactors[i] = subReactor;
                subPool.execute(subReactor);
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = keys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();
                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void accept(SelectionKey key) throws IOException {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = ssc.accept();
            subReactors[new Random().nextInt(subSize)].accpetChannel(clientChannel);
        }
    }

    static class SubReactor implements Runnable {

        private static int workSize = 10;

        private Selector selector;
        private ExecutorService workPool = Executors.newFixedThreadPool(workSize);
        /**
         * 基于链接节点的、无界的、线程安全 不允许为null
         */
        private Queue<Object> dataQueue = new ConcurrentLinkedQueue();

        public SubReactor() throws IOException {
            selector = Selector.open();
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                int num;
                try {
                    num = selector.select();
                } catch (IOException e) {
                    System.err.println("select error ,thread run over");
                    return;
                }
                if (num > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = keys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();
                        if (!key.isValid()) {
                            continue;
                        }
                        if (key.isReadable()) {
                            key.cancel();
                            workPool.execute(() -> {
                                SocketChannel readChannel = null;
                                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                                try {
                                    readChannel = (SocketChannel) key.channel();
                                    int numRead;
                                    numRead = readChannel.read(readBuffer);
                                    String message = new String(readBuffer.array(), 0, numRead);
                                    System.out.println("accept " + readChannel.getRemoteAddress().toString() + " client message:" + message);
                                    //将数据添加到key中
                                    key.attach(message);
                                    //将注册写操作添加到队列中
                                    dataQueue.add(new Task(key, SelectionKey.OP_WRITE));
                                    selector.wakeup();
                                } catch (IOException e) {
                                    // 简单弄，就不分步去固定捕获read的异常了。
                                    try {
                                        System.out.println(readChannel.getRemoteAddress().toString() + " is disconnected");
                                        readChannel.close();
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                }
                            });
                        } else if (key.isWritable()) {
                            key.cancel();
                            workPool.execute(() -> {
                                try {
                                    SocketChannel writeChannel = (SocketChannel) key.channel();
                                    String message = (String) key.attachment();
                                    System.out.println("write back to client message:" + message);
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    buffer.put(message.getBytes());
                                    buffer.flip();
                                    writeChannel.write(buffer);
                                    dataQueue.add(new Task(key, SelectionKey.OP_READ));
                                    selector.wakeup();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                } else {
                    Object obj;
                    while ((obj = dataQueue.poll()) != null) {
                        if (obj instanceof SocketChannel) {
                            registerChannel((SocketChannel) obj);
                        } else if (obj instanceof Task) {
                            processEvent((Task) obj);
                        } else {
                            throw new RuntimeException("not match queue data");
                        }
                    }
                }
            }
        }

        private void registerChannel(SocketChannel socketChannel) {
            try {
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_READ);
                System.out.println("a new client connected " + socketChannel.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processEvent(Task task) {
            SelectionKey key = task.getKey();
            //注册写事件
            SocketChannel channel = (SocketChannel) key.channel();
            Object attachment = key.attachment();
            try {
                channel.register(selector, task.getOperator(), attachment);
            } catch (ClosedChannelException e) {
                try {
                    System.err.println(channel.getRemoteAddress() + " register to selector fail");
                    key.channel();
                    channel.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }


        public void accpetChannel(SocketChannel socketChannel) throws IOException {
            if (Objects.isNull(socketChannel)) {
                throw new IllegalArgumentException("socket channel can not be null");
            }
            dataQueue.add(socketChannel);
            System.out.println("add accpet channel to queue");
            // 一定要唤醒 否则一直阻塞
            selector.wakeup();
        }

    }
}
