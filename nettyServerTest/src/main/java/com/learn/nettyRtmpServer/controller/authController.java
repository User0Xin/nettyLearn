/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2024-2024. All rights reserved.
 */

package com.learn.nettyRtmpServer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class authController {
    @PostMapping("/auth")
    @ResponseBody
    public String auth(String input, HttpServletRequest request, HttpServletResponse response){
        System.out.println("input="+ input);
        if(input.equals("1234567890")){
            response.setStatus(200);
            System.out.println("返回编号为200");
            return "{\"code\":\"200\",\"detail\":\"SUCCESS\"}";
        }else{
            response.setStatus(500);
            System.out.println("返回编号为500");
            return "{\"code\":\"500\",\"detail\":\"auth error\"}";
        }

    }
}
