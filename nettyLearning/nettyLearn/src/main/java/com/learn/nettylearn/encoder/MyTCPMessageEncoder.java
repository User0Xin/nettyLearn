/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettylearn.encoder;

import com.learn.nettylearn.model.MyTCPMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MyTCPMessageEncoder  extends MessageToByteEncoder<MyTCPMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyTCPMessage myTCPMessage, ByteBuf byteBuf)
        throws Exception {
        System.out.println("MyTCPMessageEncoder encode 方法被调用");
        byteBuf.writeInt(myTCPMessage.getLen());
        byteBuf.writeBytes(myTCPMessage.getData());
    }
}
