package com.qz.basic.netty.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * nio群聊
 * 给服务器端发送消息，并接收其他客户端的信息
 *
 * @author yangweiqiang
 */
public class NioClient {

    private static Selector selector;
    private static final int PORT = 6666;
    private static final String HOST_NAME = "127.0.0.1";

    public void start() throws IOException {
        selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(HOST_NAME, PORT));
        socketChannel.configureBlocking(false);

        socketChannel.register(selector, SelectionKey.OP_READ);

        while (!socketChannel.finishConnect()) {
            System.out.println("连接中....");
        }

        System.out.println("连接完成,开始发送.");
        receiveMsg();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            ByteBuffer byteBuffer = ByteBuffer.wrap(s.getBytes());
            socketChannel.write(byteBuffer);
        }
    }

    public void receiveMsg() {
        //接受信息
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        //阻塞操作
                        if (selector.select() == 0) {
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        System.out.println("----" + selectionKeys.size());
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            if (selectionKey.isReadable()) {
                                System.out.println("有内容可读了...");
                                ByteBuffer byteBuffer = ByteBuffer.allocate(512);
                                SocketChannel channel = (SocketChannel) selectionKey.channel();
                                channel.read(byteBuffer);

                                System.out.println(new String(byteBuffer.array(), 0, byteBuffer.position()));
                            }

                            iterator.remove();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                System.out.println("子线程close....");
            }
        });
        t.start();
    }

    public static void main(String[] args) throws Exception {
        new NioClient().start();
    }

}
