/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettylearn.decoder;

import com.learn.nettylearn.model.MyTCPMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * 解决TCP粘包拆包问题
 */
public class MyTCPMessageDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list)
        throws Exception {
        System.out.println("MyTCPMessageDecoder decode 被调用");
        // 需要将得到二进制字节码-> MyTCPMessage 数据包(对象)
        int length = byteBuf.readInt();
        byte[] data = new byte[length];
        byteBuf.readBytes(data);
        MyTCPMessage myTCPMessage = new MyTCPMessage();
        myTCPMessage.setLen(length);
        myTCPMessage.setData(data);
        // 将处理好的数据放到list当中给下一个handler使用
        list.add(myTCPMessage);
    }
}
