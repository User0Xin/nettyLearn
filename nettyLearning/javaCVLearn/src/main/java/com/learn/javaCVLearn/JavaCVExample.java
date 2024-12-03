/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.javaCVLearn;

import static org.bytedeco.ffmpeg.avcodec.AVCodecContext.FF_THREAD_FRAME;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_parser_close;
import static org.bytedeco.ffmpeg.global.avcodec.av_parser_init;
import static org.bytedeco.ffmpeg.global.avcodec.av_parser_parse2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_close;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParserContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;

public class JavaCVExample {
    public static void main(String[] args) throws Exception {
        // 初始化解码器上下文和解析器上下文
        AVCodec codec = avcodec_find_decoder(AV_CODEC_ID_H264);
        AVCodecContext codecCtx = avcodec_alloc_context3(codec);
        AVCodecParserContext parserCtx = av_parser_init(codec.id());

        // 设置解码器上下文参数
        codecCtx = codecCtx.thread_count(4);
        codecCtx = codecCtx.thread_type(FF_THREAD_FRAME);

        // 打开解码器
        avcodec_open2(codecCtx, codec, (AVDictionary) null);

        // 创建帧
        AVFrame frame = av_frame_alloc();

        // 创建 AVPacket
        AVPacket packet = av_packet_alloc();

        av_init_packet(packet);
        // 输入数据缓冲区
        byte[] inputData = new byte[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,1,1,0,1,0,1,0,0,1};
        BytePointer bytePointer = new BytePointer(inputData);

        // 解析数据
        int gotFrame = 0;
        IntPointer dataLen = new IntPointer(1);
        int consumedBytes = av_parser_parse2(parserCtx, codecCtx, frame.data(),
            dataLen,bytePointer, inputData.length, AV_NOPTS_VALUE, AV_NOPTS_VALUE, AV_NOPTS_VALUE);
        System.out.println(consumedBytes);
        System.out.println(dataLen.get());
        System.out.println(frame.data());
        System.out.println(parserCtx.frame_offset());
        System.out.println(parserCtx.key_frame());
        System.out.println(parserCtx.width());
        System.out.println(parserCtx.height());
        if ((dataLen.get() & 1) != 0) {
            // 成功解析到一帧数据，进行后续处理

            System.out.println("deal");
        }

        // 释放资源
        av_frame_free(frame);
        av_packet_free(packet);
        avcodec_close(codecCtx);
        av_parser_close(parserCtx);
        avcodec_free_context(codecCtx);
    }
}
