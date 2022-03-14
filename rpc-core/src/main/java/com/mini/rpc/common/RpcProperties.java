package com.mini.rpc.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 读取配置文件，获取rpc服务基本配置
@Data
@ConfigurationProperties(prefix = "rpc") // 读取以rpc开头的配置信息  @ConfigurationProperties
public class RpcProperties {

    private int servicePort; // rpc提供者机器的rpc服务端口

    private String registryAddr; // 注册中心的ip+端口

    private String registryType; // 注册中心的类型

}
