package com.mini.rpc.common;

import lombok.Data;


// 用于保存服务的元信息，并作为信息载体传递给服务注册器
@Data
public class ServiceMeta {

    private String serviceName;

    private String serviceVersion;

    private String serviceAddr;

    private int servicePort;

}
