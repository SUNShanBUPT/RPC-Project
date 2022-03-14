package com.mini.rpc.common;

import lombok.Data;

import java.io.Serializable;

// rpc服务发起方的信息，包括需要调用的方法的名称，参数等
@Data
public class MiniRpcRequest implements Serializable {
    private String serviceVersion;
    private String className;
    private String methodName;
    private Object[] params;
    private Class<?>[] parameterTypes;
}
