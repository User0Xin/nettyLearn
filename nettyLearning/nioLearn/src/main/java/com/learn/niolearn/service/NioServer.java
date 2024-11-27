/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.niolearn.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer {
    public void start() throws IOException {
        // 创建ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 创建选择器
        Selector selector = Selector.open();
        // 给ServerSocketChannel绑定要监听的host和端口
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 8080));
        // 设置serverSocketChannel为非阻塞
        serverSocketChannel.configureBlocking(false);
        // 将serverSocketChannel注册到选择器中，IO事件为Accept
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 阻塞等待有IO事件完成
        System.out.println("服务器已启动，监听端口8080");
        while(selector.select()>0){
            // 获取全部就绪的selectedKeys
            Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
            while (selectionKeys.hasNext()) {
                SelectionKey selectionKey = selectionKeys.next();
                // 如果是Accept事件
                if(selectionKey.isAcceptable()){
                    System.out.println("收到连接事件");
                    // 接受一个链接并将其注册到selector中
                    SocketChannel accept = serverSocketChannel.accept();
                    accept.configureBlocking(false);
                    accept.register(selector, SelectionKey.OP_READ);
                }else if (selectionKey.isReadable()) {
                    System.out.println("收到消息");
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    // 开辟一个字节缓冲区
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    // 从channel中读取数据到buffer中
                    long len = 0;
                    while((len=socketChannel.read(buffer))>0){
                        buffer.flip();
                        System.out.println("arr len:" + len);
                        System.out.println("Data: "+new String(buffer.array(), 0, buffer.limit()));
                        buffer.clear();
                    }
                    socketChannel.close();
                }
                selectionKeys.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer().start();
    }
}
