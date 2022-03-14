package com.mini.rpc.common;


// 用以将服务名称和版本号组合为ServiceMap中的key
public class RpcServiceHelper {
    public static String buildServiceKey(String serviceName, String serviceVersion) {
        return String.join("#", serviceName, serviceVersion);
    }
}
