package com.qz.basic.netty.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * nio简单群聊
 * 接收客户端发来的消息，并转发给其他客户端
 * @author yangweiqiang
 */
public class NioServer {

    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        //java.nio.channels.IllegalBlockingModeException
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.bind(new InetSocketAddress(6666));

        Selector selector = Selector.open();

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            if (selector.select() == 0) {
                continue;
            }

            /*if (selector.select(2000) == 0) {
                System.out.println("等待2s没有客户端连接...");
                continue;
            }*/

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                try {
                    //客户端连接
                    if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        String clientAddress = socketChannel.getRemoteAddress().toString();
                        System.out.println(clientAddress + "已连接...");

                        //java.nio.channels.IllegalBlockingModeException
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(10);
                        StringBuilder tmp = new StringBuilder();
                        int read;
                        while ((read = socketChannel.read(buffer)) > 0) {
                            tmp.append(new String(buffer.array(), 0, read));
                            buffer.clear();
                        }

                        String content = socketChannel.getRemoteAddress().toString() + ": " + tmp;
                        System.out.println(content);

                        notifyOtherChannel(content, socketChannel, selector);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                //处理完后，移除当前SelectionKey
                keyIterator.remove();
            }
        }

    }

    private static void notifyOtherChannel(String msg, SocketChannel selfChannel, Selector selector) throws IOException {
        Set<SelectionKey> registerKeys = selector.keys();
        for (SelectionKey registerKey : registerKeys) {
            SelectableChannel channel = registerKey.channel();
            //排除自己, 发送消息给其他channel
            if (channel instanceof SocketChannel && channel != selfChannel) {
                ((SocketChannel) channel).write(ByteBuffer.wrap(msg.getBytes()));
                System.out.println("将消息[" + msg + "]转发给: " + ((SocketChannel) channel).getRemoteAddress());
            }

        }
    }

}
