/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettylearn.nettyServer.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NettyServerBoot {
    private static final Logger log = LoggerFactory.getLogger(NettyServerBoot.class);
    @Resource(name = "bossGroup")
    NioEventLoopGroup boosGroup;
    @Resource(name = "workerGroup")
    NioEventLoopGroup workerGroup;
    final ServerBootstrap serverBootstrap;
    final NettyProperties nettyProperties;


    public NettyServerBoot(ServerBootstrap serverBootstrap, NettyProperties nettyProperties) {
        this.serverBootstrap = serverBootstrap;
        this.nettyProperties = nettyProperties;
    }


    /**
     * 启动netty
     *
     * @throws InterruptedException
     */
    @PostConstruct
    public void start() throws InterruptedException {
        // 绑定端口启动
        serverBootstrap.bind(nettyProperties.getPort()).sync();
        // 备用端口
        serverBootstrap.bind(nettyProperties.getPortSalve()).sync();
        log.info("启动Netty: {},{}", nettyProperties.getPort(), nettyProperties.getPortSalve());
    }

    /**
     * 关闭netty
     */
    @PreDestroy
    public void close() {
        log.info("关闭Netty");
        boosGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}

