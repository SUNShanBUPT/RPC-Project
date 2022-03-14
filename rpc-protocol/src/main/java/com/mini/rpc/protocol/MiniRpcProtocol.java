package com.mini.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

// 自定义数据包的整体格式类，包括Header和body，Header为自定义的MsgHeader类
@Data
public class MiniRpcProtocol<T> implements Serializable {

    // 报文Header具体细节保存在自定义的MsgHeader类中
    private MsgHeader header;

    private T body;

}
