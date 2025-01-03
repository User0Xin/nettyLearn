/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettyRtmpServer.service;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.util.StopWatch;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

@Slf4j
public class VideoHandler implements Runnable {
    // 拉流器
    private FFmpegFrameGrabber grabber;

    // 推流器
    private FFmpegFrameRecorder recorder;

    // 当前处理器处理的视频源（无人机设备)
    private Device device;

    // 获取当前视频的前端客户端（netty的Channel）
    private final ConcurrentHashMap<String, ChannelHandlerContext> httpClients = new ConcurrentHashMap<>();

    // 运行标志位
    private volatile boolean running;

    ScheduledExecutorService scheduler;

    public VideoHandler(Device device) {
        this.device = device;
        this.running = true;
        scheduler = Executors.newScheduledThreadPool(1);
    }

    // 添加客户端
    public void addClient(ChannelHandlerContext ctx) {
        scheduler.scheduleAtFixedRate(this::checkChannel, 10, 30, TimeUnit.SECONDS);
        httpClients.put(ctx.channel().id().toString(), ctx);
    }

    private void checkChannel() {
        if (httpClients.isEmpty()) {
            System.out.println("客户端全部关闭，停止拉流");
            this.running = false;
            scheduler.shutdown();
        }
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void execute() throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("创建grabber");
        log.info("创建grabber");
        // 创建拉流器
        grabber = new FFmpegFrameGrabber(device.getRtmpUrl());
        stopWatch.stop();
        setGrabber();
        stopWatch.start("启动grabber");
        log.info("启动grabber");
        grabber.start();
        stopWatch.stop();
        stopWatch.start("创建recorder");
        log.info("创建recorder");
        // 用字节流存放每个frame的数据，返回给前端
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 创建推流器
        recorder = new FFmpegFrameRecorder(bos, grabber.getImageWidth(), grabber.getImageHeight(),
            grabber.getAudioChannels());
        stopWatch.stop();
        setRecorder();
        stopWatch.start("启动recorder");
        System.out.println("启动recorder");
        recorder.start();
        stopWatch.stop();
        stopWatch.start("flush grabber");
        // 清空缓冲区
        grabber.flush();
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        long startTime = 0;
        long lastTime = System.currentTimeMillis(); // 上次获取到数据的时间
        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
        while (running) {
            // 拉取一帧
            Frame frame = grabber.grab();
            if (frame != null && frame.image != null) {
                lastTime = System.currentTimeMillis();
                // recorder.setTimestamp((1000 * (System.currentTimeMillis() - startTime)));
                // 处理图片数据
                // BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
                // byte[] jpgData = Image2Bytes(bufferedImage, "jpg");// 假设 frame.image[0] 是一个 ByteBuffer 或其子类
                byte[] jpgData = null;
                if (frame.image[0] instanceof ByteBuffer) {
                    ByteBuffer byteBuffer = (ByteBuffer) frame.image[0];

                    // 确保缓冲区中有足够的数据
                    if (byteBuffer.remaining() > 0) {
                        // 创建字节数组，大小等于缓冲区的剩余数据量
                        jpgData = new byte[byteBuffer.remaining()];

                        // 翻转缓冲区并读取数据
                        // byteBuffer.flip();
                        byteBuffer.get(jpgData);

                        // 现在 jpgData 包含了缓冲区中的所有数据
                    } else {
                        // 处理没有数据可读的情况
                        System.out.println("Buffer is empty.");
                    }
                } else {
                    // 处理 frame.image[0] 不是 ByteBuffer 的情况
                    System.out.println("frame.image[0] is not a ByteBuffer.");
                }
                // 通过websocket发送给大模型

                // 接受数据
                byte[] receivedData = jpgData;
                frame.image[0].clear();
                ((ByteBuffer) frame.image[0]).put(receivedData);
                // BufferedImage handledImage= Bytes2Image(receivedData);
                //handledImage = printRect(handledImage);

                // Frame handledFrame = Java2DFrameUtils.toFrame(handledImage);
                recorder.record(frame);
                // recorder.record(frame);
                if (bos.size() > 0) {
                    byte[] b = bos.toByteArray();
                    bos.reset();
                    // 将数据发送给前端
                    sendFrame(b);
                    continue;
                }
            }
            // 10秒内读不到视频帧，则关闭连接
            if ((System.currentTimeMillis() / 1000 - lastTime / 1000) > 10) {
                log.info("{} ：10秒内读不到视频帧", device.getDeviceId());
                break;
            }
        }
        recorder.close();
        grabber.close();
        bos.close();
        closeClient();
    }

    // 配置拉流器参数
    private void setGrabber() {
        // 拉流超时时间(10秒)
        grabber.setOption("stimeout", "10000000");
        // 拉流线程
        grabber.setOption("threads", "1");
        // grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        // 设置缓存大小，提高画质、减少卡顿花屏
        grabber.setOption("buffer_size", "1024000");
        // 读写超时，适用于所有协议的通用读写超时
        grabber.setOption("rw_timeout", "15000000");
        // 探测视频流信息，为空默认5000000微秒
        // grabber.setOption("probesize", "5000000");
        // 解析视频流信息，为空默认5000000微秒
        //grabber.setOption("analyzeduration", "5000000");
    }

    // 配置推流器参数
    private void setRecorder() {
        recorder.setFormat("flv");
        // 转码
        // 关闭交错模式。音频和视频数据分开处理。
        recorder.setInterleaved(false);
        // 设置解析效率，优化编码器以减少延迟
        recorder.setVideoOption("tune", "zerolatency");
        // 设置编码速度为最快，牺牲一定的压缩效率。
        recorder.setVideoOption("preset", "ultrafast");
        // 设置恒定质量因子，平衡视频质量和文件大小。
        recorder.setVideoOption("crf", "23");
        // 设置工作线程为1
        recorder.setVideoOption("threads", "1");
        // 设置帧率
        recorder.setFrameRate(25);
        // 设置gop,与帧率相同
        recorder.setGopSize(25);
        // 设置输出码率为1Mbps保证480p画面清晰
        recorder.setVideoBitrate(1000 * 1000);
        // 设置视频编码格式为H.264
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 设置像素格式为 YUV420P
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        // 设置音频编码器为 AAC
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        // 设置关键帧的最小间隔为 25 帧
        recorder.setOption("keyint_min", "25");
        // 启用 Trellis 量化，有助于提高视频压缩效率和质量。
        recorder.setTrellis(1);
        // 设置最大延迟为 0，进一步减少编码过程中的延迟。
        recorder.setMaxDelay(0);
    }

    /**
     * 关闭所有的客户端连接
     */
    private void closeClient() {
        // 将当前设备从设备列表中移除
        LiveServer.deviceContext.remove(device.getDeviceId());
        // 关闭所有客户端连接
        for (ChannelHandlerContext client : httpClients.values()) {
            client.close();
        }
    }

    private void sendFrame(byte[] data) {
        for (Map.Entry<String, ChannelHandlerContext> entry : httpClients.entrySet()) {
            if (entry.getValue().channel().isWritable()) {
                entry.getValue().writeAndFlush(Unpooled.copiedBuffer(data));
            } else {
                httpClients.remove(entry.getKey());
                log.info("设备：" + device.getDeviceId() + "的：channel：" + entry.getKey() + "已被去除");
            }
        }
    }

    public byte[] Image2Bytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos); // format可以是"png", "jpg", "jpeg"等
        return baos.toByteArray();
    }

    public BufferedImage Bytes2Image(byte[] data) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        return ImageIO.read(byteArrayInputStream);
    }

    public BufferedImage printRect(BufferedImage bufferedImage) throws IOException {
        // 2. 将BufferedImage转换为Mat
        Mat mat = Java2DFrameUtils.toMat(bufferedImage);
        // 3. 定义矩形的参数
        int x = 10; // 矩形左上角的x坐标
        int y = 10; // 矩形左上角的y坐标
        int width = 100; // 矩形的宽度
        int height = 100; // 矩形的高度
        Scalar color = new Scalar(0, 0, 255, 0); // 矩形的颜色（BGR格式），这里是红色，不透明
        // 4. 绘制矩形
        opencv_imgproc.rectangle(mat, new Point(x, y), new Point(x + width, y + height), color);
        // 5. 将Mat转换回BufferedImage
        return Java2DFrameUtils.toBufferedImage(mat);
    }
}
