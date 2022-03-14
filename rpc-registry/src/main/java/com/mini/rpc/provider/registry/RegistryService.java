package com.mini.rpc.provider.registry;

import com.mini.rpc.common.ServiceMeta;

import java.io.IOException;


// 服务注册器需要实现的接口，实现向注册中心注册、下线、服务发现等功能
public interface RegistryService {

    void register(ServiceMeta serviceMeta) throws Exception;

    void unRegister(ServiceMeta serviceMeta) throws Exception;

    ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception;

    void destroy() throws IOException;
}
