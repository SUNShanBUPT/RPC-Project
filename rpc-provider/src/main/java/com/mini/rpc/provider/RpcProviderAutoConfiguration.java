package com.mini.rpc.provider;

import com.mini.rpc.common.RpcProperties;
import com.mini.rpc.provider.registry.RegistryFactory;
import com.mini.rpc.provider.registry.RegistryService;
import com.mini.rpc.provider.registry.RegistryType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(RpcProperties.class)
public class RpcProviderAutoConfiguration {

    // 读取rpc配置信息，封装在RpcProperties对象中
    @Resource
    private RpcProperties rpcProperties;


    // 生成RpcProvider的单例模式Bean
    @Bean
    public RpcProvider init() throws Exception {
        RegistryType type = RegistryType.valueOf(rpcProperties.getRegistryType());

        // 调用RegistryFactory，采用单例模式实例化服务注册器
        RegistryService serviceRegistry = RegistryFactory.getInstance(rpcProperties.getRegistryAddr(), type);

        // 传入服务注册器实例及rpc提供端口号
        return new RpcProvider(rpcProperties.getServicePort(), serviceRegistry);
    }
}
