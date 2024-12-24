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

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        List<String> parameters = decoder.parameters().get("deviceId");
        if(parameters == null || parameters.isEmpty()){
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        String deviceId = parameters.get(0);
        sendFlvResHeader(ctx);
        Device device = new Device(deviceId, LiveServer.YOUR_VIDEO_PATH);
        playForHttp(device, ctx);
    }


    public void playForHttp(Device device, ChannelHandlerContext ctx) {
        CompletableFuture.runAsync(() -> {
            try {
                play(device, ctx);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private void play(Device device, ChannelHandlerContext ctx) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("创建grabber");
        System.out.println("创建grabber");
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(device.getRtmpUrl());
        stopWatch.stop();
        //拉流超时时间(10秒)
        grabber.setOption("stimeout", "10000000");
        grabber.setOption("threads", "1");
        grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        // 设置缓存大小，提高画质、减少卡顿花屏
        grabber.setOption("buffer_size", "1024000");
        // 读写超时，适用于所有协议的通用读写超时
        grabber.setOption("rw_timeout", "15000000");
        // 探测视频流信息，为空默认5000000微秒
        // grabber.setOption("probesize", "5000000");
        // 解析视频流信息，为空默认5000000微秒
        //grabber.setOption("analyzeduration", "5000000");
        stopWatch.start("启动grabber");
        System.out.println("启动grabber");
        grabber.start();
        stopWatch.stop();
        stopWatch.start("创建recorder");
        System.out.println("创建recorder");
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight(),
            grabber.getAudioChannels());
        stopWatch.stop();
        recorder.setFormat("flv");
        // 转码
        recorder.setInterleaved(false);
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("crf", "23");
        recorder.setVideoOption("threads", "1");
        recorder.setFrameRate(25);// 设置帧率
        recorder.setGopSize(25);// 设置gop,与帧率相同
        recorder.setVideoBitrate(1000 * 1000);// 码率1Mbps保证480p画面
        // recorder.setVideoBitrate(grabber.getVideoBitrate()); // 设置视频比特率
        System.out.println("码率"+grabber.getVideoBitrate());
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        recorder.setOption("keyint_min", "25");  //gop最小间隔
        recorder.setTrellis(1);
        recorder.setMaxDelay(0);// 设置延迟
        stopWatch.start("启动recorder");
        System.out.println("启动recorder");
        recorder.start();
        stopWatch.stop();
        stopWatch.start("flush grabber");
        grabber.flush();
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
        Frame frame;
        long startTime = 0;
        long lastTime = System.currentTimeMillis();
        while ((frame = grabber.grab()) != null) {
            lastTime = System.currentTimeMillis();
            // recorder.setTimestamp((1000 * (System.currentTimeMillis() - startTime)));
            recorder.record(frame);
            if (bos.size() > 0) {
                byte[] b = bos.toByteArray();
                bos.reset();
                ctx.writeAndFlush(Unpooled.copiedBuffer(b));
            }
        }
        recorder.close();
        grabber.close();
        bos.close();
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
            .set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED).set(HttpHeaderNames.SERVER, "LiveServer");
        ctx.writeAndFlush(rsp);
    }
}
