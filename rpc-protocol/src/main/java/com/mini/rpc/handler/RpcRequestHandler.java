package com.mini.rpc.handler;

import com.mini.rpc.common.MiniRpcRequest;
import com.mini.rpc.common.MiniRpcResponse;
import com.mini.rpc.common.RpcServiceHelper;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.protocol.MsgHeader;
import com.mini.rpc.protocol.MsgStatus;
import com.mini.rpc.protocol.MsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;

import java.util.Map;

// 入站处理器
@Slf4j
public class RpcRequestHandler extends SimpleChannelInboundHandler<MiniRpcProtocol<MiniRpcRequest>> {

    // 服务提供者的服务列表Map
    private final Map<String, Object> rpcServiceMap;

    public RpcRequestHandler(Map<String, Object> rpcServiceMap) {
        this.rpcServiceMap = rpcServiceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MiniRpcProtocol<MiniRpcRequest> protocol) {
        // 向rpc请求处理类中传入Runnable匿名内部类
        RpcRequestProcessor.submitRequest(() -> {
            MiniRpcProtocol<MiniRpcResponse> resProtocol = new MiniRpcProtocol<>();
            MiniRpcResponse response = new MiniRpcResponse();
            MsgHeader header = protocol.getHeader();
            header.setMsgType((byte) MsgType.RESPONSE.getType());
            try {
                Object result = handle(protocol.getBody()); // handle方法调用具体方法，并返回结果
                response.setData(result); //将方法执行结果放入response消息体

                header.setStatus((byte) MsgStatus.SUCCESS.getCode());
                resProtocol.setHeader(header); // 封装为response数据包
                resProtocol.setBody(response);
            } catch (Throwable throwable) {
                header.setStatus((byte) MsgStatus.FAIL.getCode());// 执行异常，Header中status设为FAIL
                response.setMessage(throwable.toString());
                log.error("process request {} error", header.getRequestId(), throwable);
            }
            ctx.writeAndFlush(resProtocol);
        });
    }

    private Object handle(MiniRpcRequest request) throws Throwable {
        // 根据服务名从服务列表中获得具体的方法Bean
        String serviceKey = RpcServiceHelper.buildServiceKey(request.getClassName(), request.getServiceVersion());
        Object serviceBean = rpcServiceMap.get(serviceKey);

        // 方法不存在则报错
        if (serviceBean == null) {
            throw new RuntimeException(String.format("service not exist: %s:%s", request.getClassName(), request.getMethodName()));
        }

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParams();

        // 通过cglib FastClass生成一个新的类
        // 使用FastClass创建被代理类
        // FastClass不使用反射类（Constructor或Method）来调用委托类方法，而是动态生成一个新的类（继承FastClass），
        FastClass fastClass = FastClass.create(serviceClass);

        // 根据方法名和参数列表，获取被调用的方法，避免重载找不到调用哪个方法
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);

        // 调用FastClass的invoke方法，执行Bean类的特定参数列表的代理方法
        return fastClass.invoke(methodIndex, serviceBean, parameters);
    }
}
