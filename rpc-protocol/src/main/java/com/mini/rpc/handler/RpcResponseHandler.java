package com.mini.rpc.handler;


import com.mini.rpc.common.MiniRpcFuture;
import com.mini.rpc.common.MiniRpcRequestHolder;
import com.mini.rpc.common.MiniRpcResponse;
import com.mini.rpc.protocol.MiniRpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

// 处理服务端返回的结果
public class RpcResponseHandler extends SimpleChannelInboundHandler<MiniRpcProtocol<MiniRpcResponse>> {

    // 从channel读取解码后的返回信息包装类
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MiniRpcProtocol<MiniRpcResponse> msg) {
        long requestId = msg.getHeader().getRequestId();

        // 从请求map中删除已处理的数据，并获取future对象，从而写入返回消息体
        MiniRpcFuture<MiniRpcResponse> future = MiniRpcRequestHolder.REQUEST_MAP.remove(requestId);
        future.getPromise().setSuccess(msg.getBody());

    }
}

