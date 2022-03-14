package com.mini.rpc.codec;

import com.mini.rpc.common.MiniRpcRequest;
import com.mini.rpc.common.MiniRpcResponse;
import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.protocol.MsgHeader;
import com.mini.rpc.protocol.MsgType;
import com.mini.rpc.protocol.ProtocolConstants;
import com.mini.rpc.serialization.RpcSerialization;
import com.mini.rpc.serialization.SerializationFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;


// 自定义解码器
public class MiniRpcDecoder extends ByteToMessageDecoder {

    /*
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 8byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    |                   数据内容 （长度不定）                          |
    +---------------------------------------------------------------+
    */
    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 若数据比数据包Header长度还短，则什么都不干，直接return
        if (in.readableBytes() < ProtocolConstants.HEADER_TOTAL_LEN) {
            return;
        }
        in.markReaderIndex();

        // 读取魔数，判断是否合法
        short magic = in.readShort();
        if (magic != ProtocolConstants.MAGIC) {
            throw new IllegalArgumentException("magic number is illegal, " + magic);
        }

        // 读取Header中其他字段
        byte version = in.readByte();
        byte serializeType = in.readByte();
        byte msgType = in.readByte();
        byte status = in.readByte();
        long requestId = in.readLong();

        // 读取数据长度，如果ByteBuf可读长度小于数据长度，重置读索引，返回
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data); // 读取数据

        // 获取报文类型，request or response or heartbeat，不合法则返回
        MsgType msgTypeEnum = MsgType.findByType(msgType);
        if (msgTypeEnum == null) {
            return;
        }

        // 此时一个Msg已经解析完成
        MsgHeader header = new MsgHeader();
        header.setMagic(magic);
        header.setVersion(version);
        header.setSerialization(serializeType);
        header.setStatus(status);
        header.setRequestId(requestId);
        header.setMsgType(msgType);
        header.setMsgLen(dataLength);

        // 读取序列化方法，从序列化方法工厂获得相应的序列化方法实例
        RpcSerialization rpcSerialization = SerializationFactory.getRpcSerialization(serializeType);
        switch (msgTypeEnum) {
            case REQUEST:
                // 获取反序列化后的对象，即MiniRpcRequest，是数据包中的body，即请求内容
                MiniRpcRequest request = rpcSerialization.deserialize(data, MiniRpcRequest.class);
                if (request != null) {
                    // 将body和header组装起来，返回
                    MiniRpcProtocol<MiniRpcRequest> protocol = new MiniRpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(request);
                    out.add(protocol);
                }
                break;
            case RESPONSE:
                // 返回rpc提供者的回应数据包
                MiniRpcResponse response = rpcSerialization.deserialize(data, MiniRpcResponse.class);
                if (response != null) {
                    MiniRpcProtocol<MiniRpcResponse> protocol = new MiniRpcProtocol<>();
                    protocol.setHeader(header);
                    protocol.setBody(response);
                    out.add(protocol);
                }
                break;
            case HEARTBEAT:
                // TODO
                break;
        }
    }
}
