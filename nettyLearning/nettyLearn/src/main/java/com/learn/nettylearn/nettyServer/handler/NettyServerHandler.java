/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettylearn.nettyServer.handler;

import com.learn.nettylearn.decoder.MyTCPMessageDecoder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class NettyServerHandler extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
            // .addLast(new HttpServerCodec())
            // .addLast(new MyHttpNettyHandler())
            .addLast("decoder",new MyTCPMessageDecoder())
            .addLast(new MyTCPNettyHandler())
            .addLast(new MyTCPNettyHandler());
    }
}
