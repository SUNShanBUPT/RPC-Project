package com.mini.rpc.common;

import lombok.Data;

import java.io.Serializable;
// rpc服务提供者返回的结果消息体
@Data
public class MiniRpcResponse implements Serializable {
    private Object data;
    private String message;
}
