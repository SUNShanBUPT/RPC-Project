package com.mini.rpc.provider.registry;


// 用于生成单例模式服务注册器
public class RegistryFactory {

    private static volatile RegistryService registryService;

    public static RegistryService getInstance(String registryAddr, RegistryType type) throws Exception {

        if (null == registryService) {
            synchronized (RegistryFactory.class) {
                if (null == registryService) {
                    switch (type) {
                        case ZOOKEEPER:
                            registryService = new ZookeeperRegistryService(registryAddr); // 生成Zookeeper服务注册器
                            break;
                        case EUREKA:
                            registryService = new EurekaRegistryService(registryAddr);
                            break;
                    }
                }
            }
        }
        return registryService;
    }
}
