/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettylearn.model;

import lombok.Data;

@Data
public class MyTCPMessage {
    private int len;
    private byte[] data;
}
