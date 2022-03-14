package com.mini.rpc.codec;

import com.mini.rpc.protocol.MiniRpcProtocol;
import com.mini.rpc.protocol.MsgHeader;
import com.mini.rpc.serialization.RpcSerialization;
import com.mini.rpc.serialization.SerializationFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


// 继承Netty的MessageToByteEncoder，重写抽象方法encode
public class MiniRpcEncoder extends MessageToByteEncoder<MiniRpcProtocol<Object>> {

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
    protected void encode(ChannelHandlerContext ctx, MiniRpcProtocol<Object> msg, ByteBuf byteBuf) throws Exception {
        MsgHeader header = msg.getHeader();
        byteBuf.writeShort(header.getMagic());// 将信息写入ByteBuf中
        byteBuf.writeByte(header.getVersion());
        byteBuf.writeByte(header.getSerialization());
        byteBuf.writeByte(header.getMsgType());
        byteBuf.writeByte(header.getStatus());
        byteBuf.writeLong(header.getRequestId());

        // 根据序列化位置的byte，从序列化方法工厂获得对应的序列化方法
        RpcSerialization rpcSerialization = SerializationFactory.getRpcSerialization(header.getSerialization());

        // 通过序列化方法将消息体body序列化
        byte[] data = rpcSerialization.serialize(msg.getBody());

        // 获得body序列化后的长度，填入byteBuf中
        byteBuf.writeInt(data.length);

        // 写入body
        byteBuf.writeBytes(data);
    }
}
