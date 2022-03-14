package com.mini.rpc.provider;

import com.mini.rpc.codec.MiniRpcDecoder;
import com.mini.rpc.codec.MiniRpcEncoder;
import com.mini.rpc.common.RpcServiceHelper;
import com.mini.rpc.common.ServiceMeta;
import com.mini.rpc.handler.RpcRequestHandler;
import com.mini.rpc.provider.annotation.RpcService;
import com.mini.rpc.provider.registry.RegistryService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RpcProvider implements InitializingBean, BeanPostProcessor {

    private String serverAddress;
    private final int serverPort;
    private final RegistryService serviceRegistry;

    // Map中保存了当前rpc服务提供者能提供的所有服务，以服务名称为key，服务提供Bean为value
    private final Map<String, Object> rpcServiceMap = new HashMap<>();

    public RpcProvider(int serverPort, RegistryService serviceRegistry) {
        this.serverPort = serverPort;
        this.serviceRegistry = serviceRegistry; // 将单例模式服务注册器传递进来
    }


    // 实例化之前执行，用新线程开启Netty服务，afterPropertiesSet()方法只有当前Bean会执行
    @Override
    public void afterPropertiesSet() {
        new Thread(() -> {
            try {
                startRpcServer();
            } catch (Exception e) {
                log.error("start rpc server error.", e);
            }
        }).start();
    }


    // 新线程，开启Netty服务
    private void startRpcServer() throws Exception {
        this.serverAddress = InetAddress.getLocalHost().getHostAddress(); // 获取当前机器的IP地址

        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // handler发生在初始化时，childHandler发生在客户端连接后
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new MiniRpcEncoder())
                                    .addLast(new MiniRpcDecoder())
                                    .addLast(new RpcRequestHandler(rpcServiceMap)); // 将全部服务列表map传入RequestHandler
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);


            // ServerBootstrap的bind方法是入口，主要做了个校验，然后再调用核心的doBind方法
            ChannelFuture channelFuture = bootstrap.bind(this.serverAddress, this.serverPort).sync();
            log.info("server addr {} started on port {}", this.serverAddress, this.serverPort);
            channelFuture.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }


    // Bean实例化后执行postProcessAfterInitialization方法，所有的Bean都会执行
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class); // 判断当前执行的Bean是否是提供rpcService的Bean
        if (rpcService != null) {
            String serviceName = rpcService.serviceInterface().getName(); // 获取当前rpcService Bean的名字及version
            String serviceVersion = rpcService.serviceVersion();

            try {
                ServiceMeta serviceMeta = new ServiceMeta(); // 将当前rpcService Bean的元信息封装到serviceMeta中
                serviceMeta.setServiceAddr(serverAddress);
                serviceMeta.setServicePort(serverPort);
                serviceMeta.setServiceName(serviceName);
                serviceMeta.setServiceVersion(serviceVersion);

                serviceRegistry.register(serviceMeta); // 将serviceMeta传入服务注册类中，在注册中心处实现服务注册

                // 在当前rpc服务提供者的服务列表中加入此rpcService
                rpcServiceMap.put(RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName(), serviceMeta.getServiceVersion()), bean);
            } catch (Exception e) {
                log.error("failed to register service {}#{}", serviceName, serviceVersion, e);
            }
        }
        return bean;
    }

}
