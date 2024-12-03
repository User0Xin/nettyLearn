/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettyRtmpServer.service;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import org.bytedeco.javacv.FFmpegFrameRecorder;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ChannelHandler.Sharable
public class LiveHandler extends SimpleChannelInboundHandler<Object> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        FullHttpRequest req = (FullHttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        // 判断请求uri
        if (!"/live".equals(decoder.path())) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
        List<String> parameters = queryStringDecoder.parameters().get("deviceId");
        if(parameters == null || parameters.isEmpty()){
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        String deviceId = parameters.get(0);
        sendFlvResHeader(ctx);
        Device device = new Device(deviceId, MediaServer.YOUR_VIDEO_PATH);
        playForHttp(device, ctx);
    }

    public void playForHttp(Device device, ChannelHandlerContext ctx) {
        try {
            TransferToFlv mediaConvert = new TransferToFlv();
            if (MediaServer.deviceContext.containsKey(device.getDeviceId())) {
                mediaConvert = MediaServer.deviceContext.get(device.getDeviceId());
                mediaConvert.getMediaChannel().addChannel(ctx, true);
                return;
            }
            mediaConvert.setCurrentDevice(device);
            MediaChannel mediaChannel = new MediaChannel(device);
            mediaConvert.setMediaChannel(mediaChannel);
            MediaServer.deviceContext.put(device.getDeviceId(), mediaConvert);
            //注册事件
            // mediaChannel.getEventBus().register(mediaConvert);
            new Thread(mediaConvert).start();
            mediaConvert.getMediaChannel().addChannel(ctx, false);
        } catch (InterruptedException | FFmpegFrameRecorder.Exception e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * 错误请求响应
     *
     * @param ctx
     * @param status
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
            Unpooled.copiedBuffer("请求地址有误: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送req header，告知浏览器是flv格式
     *
     * @param ctx
     */
    private void sendFlvResHeader(ChannelHandlerContext ctx) {
        HttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        rsp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
            .set(HttpHeaderNames.CONTENT_TYPE, "video/x-flv").set(HttpHeaderNames.ACCEPT_RANGES, "bytes")
            .set(HttpHeaderNames.PRAGMA, "no-cache").set(HttpHeaderNames.CACHE_CONTROL, "no-cache")
            .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED).set(HttpHeaderNames.SERVER, "测试");
        ctx.writeAndFlush(rsp);
    }
}
