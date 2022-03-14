package com.mini.rpc.consumer;

import com.mini.rpc.common.MiniRpcFuture;
import com.mini.rpc.common.MiniRpcRequest;
import com.mini.rpc.common.MiniRpcRequestHolder;
import com.mini.rpc.common.MiniRpcResponse;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.protocol.MsgHeader;
import com.mini.rpc.protocol.MsgType;
import com.mini.rpc.protocol.ProtocolConstants;
import com.mini.rpc.provider.registry.RegistryService;
import com.mini.rpc.serialization.SerializationTypeEnum;
import io.netty.channel.DefaultEventLoop;
import io.netty.util.concurrent.DefaultPromise;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

// 通过InvocationHandler生成代理类
public class RpcInvokerProxy implements InvocationHandler {

    private final String serviceVersion;
    private final long timeout;
    private final RegistryService registryService; // 服务注册器工具类

    public RpcInvokerProxy(String serviceVersion, long timeout, RegistryService registryService) {
        this.serviceVersion = serviceVersion;
        this.timeout = timeout;
        this.registryService = registryService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 实例化调用数据包与消息体
        MiniRpcProtocol<MiniRpcRequest> protocol = new MiniRpcProtocol<>();
        MsgHeader header = new MsgHeader();

        // 通过AtomicLong类生成请求的序列号
        long requestId = MiniRpcRequestHolder.REQUEST_ID_GEN.incrementAndGet();

        // 构造消息体
        header.setMagic(ProtocolConstants.MAGIC);
        header.setVersion(ProtocolConstants.VERSION);
        header.setRequestId(requestId);
        header.setSerialization((byte) SerializationTypeEnum.HESSIAN.getType());
        header.setMsgType((byte) MsgType.REQUEST.getType());
        header.setStatus((byte) 0x1);
        protocol.setHeader(header);

        // 将请求的方法的信息装入request类，作为数据包的body发送
        MiniRpcRequest request = new MiniRpcRequest();
        request.setServiceVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParams(args);
        protocol.setBody(request); // 将方法请求体放入body

        // 开启consumer实际调用过程，包括启动netty，从注册中心获取服务提供者信息，建立连接，发送请求
        RpcConsumer rpcConsumer = new RpcConsumer();

        MiniRpcFuture<MiniRpcResponse> future = new MiniRpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()), timeout);
        MiniRpcRequestHolder.REQUEST_MAP.put(requestId, future);
        rpcConsumer.sendRequest(protocol, this.registryService);

        // TODO hold request by ThreadLocal


        return future.getPromise().get(future.getTimeout(), TimeUnit.MILLISECONDS).getData();
    }
}
