/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.niolearn.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioClient {
    public void start() throws IOException, InterruptedException {
        Thread thread = Thread.startVirtualThread(()->{
          while(true){
              System.out.println("haha");
              try {
                  Thread.sleep(1000);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
          }
        });
        SocketChannel socketChannel = SocketChannel.open();
        // 设置为非阻塞
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));
        // 等待连接成功
        while (!socketChannel.finishConnect()) {
        }
        socketChannel.close();
        socketChannel = SocketChannel.open();
        // 设置为非阻塞
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));
        // 等待连接成功
        while (!socketChannel.finishConnect()) {
        }
        System.out.println("连接成功");
        // 分配Buffer空间
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 写入Buffer
        byteBuffer.put("hello world".getBytes());
        // 切换成读模式
        byteBuffer.flip();
        // 把Buffer 写入Socket Channel
        socketChannel.write(byteBuffer);
        // 关闭写连接
        socketChannel.shutdownOutput();
        // 关闭socket Channel
        socketChannel.close();
        Thread.sleep(10000);

    }
    public static void main(String[] args) throws IOException, InterruptedException {
        new NioClient().start();
    }
}
