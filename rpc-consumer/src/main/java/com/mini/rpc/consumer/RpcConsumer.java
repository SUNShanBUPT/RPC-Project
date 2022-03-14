package com.mini.rpc.consumer;

import com.mini.rpc.codec.MiniRpcDecoder;
import com.mini.rpc.codec.MiniRpcEncoder;
import com.mini.rpc.common.MiniRpcRequest;
import com.mini.rpc.common.RpcServiceHelper;
import com.mini.rpc.common.ServiceMeta;
import com.mini.rpc.handler.RpcResponseHandler;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.provider.registry.RegistryService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;


// 开启consumer实际调用过程，包括配置启动netty->从注册中心获取服务提供者信息->建立连接->发送请求
@Slf4j
public class RpcConsumer {
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    // 创建Netty
    public RpcConsumer() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() { // handler发生在初始化时，childHandler发生在客户端连接后
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new MiniRpcEncoder())
                                .addLast(new MiniRpcDecoder())
                                .addLast(new RpcResponseHandler());// 处理解码后的结果消息体
                    }
                });
    }


    // 发送请求，参数为数据包，服务注册工具类
    public void sendRequest(MiniRpcProtocol<MiniRpcRequest> protocol, RegistryService registryService) throws Exception {
        MiniRpcRequest request = protocol.getBody();
        Object[] params = request.getParams();
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(), request.getServiceVersion());

        // 通过服务注册工具类discovery()方法，获取服务提供者的信息，包括地址端口服务名称等，包装在ServiceMeta中
        int invokerHashCode = params.length > 0 ? params[0].hashCode() : serviceKey.hashCode();
        ServiceMeta serviceMetadata = registryService.discovery(serviceKey, invokerHashCode);

        // 成功从注册中心获得服务提供者信息，建立Netty连接
        if (serviceMetadata != null) {
            // 客服端bootstrap.connect()方法标志着连接开始，服务端bootstrap.bind()方法标志连接开始
            ChannelFuture future = bootstrap.connect(serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort()).sync();

            // 判断是否连接成功
            future.addListener((ChannelFutureListener) arg0 -> { // 判断是否连接成功
                if (future.isSuccess()) {
                    log.info("connect rpc server {} on port {} success.", serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort());
                } else {
                    log.error("connect rpc server {} on port {} failed.", serviceMetadata.getServiceAddr(), serviceMetadata.getServicePort());
                    future.cause().printStackTrace();
                    eventLoopGroup.shutdownGracefully();
                }
            });

            // 发送服务请求
            future.channel().writeAndFlush(protocol);
        }
    }
}
