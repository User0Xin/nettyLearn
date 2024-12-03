/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettyRtmpServer.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MediaServer implements CommandLineRunner {

    @Autowired
    private LiveHandler liveHandler;
    public static ConcurrentHashMap<String, TransferToFlv> deviceContext = new ConcurrentHashMap<>();
    public final static String  YOUR_VIDEO_PATH = "rtmp://localhost:1935/flv-live/test";
    public final static int PORT = 8234;

    public void start() {
        InetSocketAddress socketAddress = new InetSocketAddress("0.0.0.0", PORT);
        //主线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        //工作线程组
        EventLoopGroup workGroup = new NioEventLoopGroup(200);
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(bossGroup, workGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();

                    socketChannel.pipeline()
                        .addLast(new HttpResponseEncoder())
                        .addLast(new HttpRequestDecoder())
                        .addLast(new ChunkedWriteHandler())
                        .addLast(new HttpObjectAggregator(64 * 1024))
                        .addLast(new CorsHandler(corsConfig))
                        .addLast(liveHandler);
                }
            })
            .localAddress(socketAddress)
            .option(ChannelOption.SO_BACKLOG, 128)
            //选择直接内存
            .option(ChannelOption.ALLOCATOR, PreferredDirectByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)
            .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024));
        //绑定端口,开始接收进来的连接
        try {
            ChannelFuture future = bootstrap.bind(socketAddress).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //关闭主线程组
            bossGroup.shutdownGracefully();
            //关闭工作线程组
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void run(String... args) {
        log.info("server started");
        this.start();
    }
}

