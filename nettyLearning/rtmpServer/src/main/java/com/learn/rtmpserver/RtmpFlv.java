package com.learn.rtmpserver;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

public class RtmpFlv {

    private static final String outputUrl = "rtmp://localhost:1935/flv-live/test";

    private static final String inputUrl = "D:\\CloudMusic\\MV\\01.mp4";

    public static void main(String[] args) throws FrameGrabber.Exception, FrameRecorder.Exception {
        //设置FFmpeg日志级别
        avutil.av_log_set_level(avutil.AV_LOG_INFO);
        FFmpegLogCallback.set();

        //以文件路径的方式传入视频，当然也支持以流的方式传入
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputUrl);
        //开始捕获视频流
        grabber.start();

        //用于将捕获到的视频流转换为输出URL的mp4格式。
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputUrl, grabber.getImageWidth(), grabber.getImageHeight());
        recorder.setFormat("flv");
        recorder.setVideoBitrate(grabber.getVideoBitrate()); // 设置视频比特率
        recorder.setFrameRate(grabber.getVideoFrameRate()); // 设置帧率
        recorder.setGopSize((int) grabber.getVideoFrameRate()); // 设置关键帧间隔
        // CRF 是一种用于控制视频/音频质量的参数，它允许在保持目标质量的同时动态地调整比特率。较低的CRF值表示更高的质量，但也可能导致较大的文件大小
        recorder.setAudioOption("crf", "23");

        Frame frame;
        //设置音频编码为AAC
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(grabber.getAudioBitrate());
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        }
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        //将解码后的帧记录到输出文件中
        //recorder.start通常用于处理已经解码成图像的视频数据
        recorder.start();
        while ((frame = grabber.grab()) != null) {
            recorder.record(frame);
        }
        recorder.close();
        grabber.close();
    }
}
