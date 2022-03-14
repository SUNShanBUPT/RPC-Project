package com.mini.rpc.provider.registry;

import com.mini.rpc.common.RpcServiceHelper;
import com.mini.rpc.common.ServiceMeta;
import com.mini.rpc.provider.registry.loadbalancer.ZKConsistentHashLoadBalancer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.List;


// ZooKeeper的服务注册器，单例模式生成，是一个工具类，在开发中其实只需要存在一份即可，因此采用单例模式
// 单例模式的优势：1.少创建实例  2.减少JVM垃圾回收  3.缓存快速获取
public class ZookeeperRegistryService implements RegistryService {
    public static final int BASE_SLEEP_TIME_MS = 1000;
    public static final int MAX_RETRIES = 3;
    public static final String ZK_BASE_PATH = "/mini_rpc";

    // ServiceDiscovery 为 Curator 提供的服务发现接口
    private final ServiceDiscovery<ServiceMeta> serviceDiscovery;


    // 构造器，以注册中心地址为参数
    public ZookeeperRegistryService(String registryAddr) throws Exception {
        // 使用Curator建立与Zookeeper的连接
        CuratorFramework client = CuratorFrameworkFactory.newClient(registryAddr, new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS, MAX_RETRIES));
        client.start();

        // 使用Curator自带JsonInstanceSerializer将服务元数据ServiceMeta序列化
        JsonInstanceSerializer<ServiceMeta> serializer = new JsonInstanceSerializer<>(ServiceMeta.class);

        // 实例化 服务发现类 ServiceDiscoveryBuilder的build（）方法返回一个ServiceDiscoveryImpl类，其中有与Zookeeper相关的操作
        // 实例化后serviceDiscovery借口实际指向了ServiceDiscoveryImpl类
        this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceMeta.class)
                .client(client)
                .serializer(serializer)
                .basePath(ZK_BASE_PATH)
                .build();
        this.serviceDiscovery.start();
    }


    // 先用serviceMeta实例化Curator的ServiceInstance
    // 然后传入ServiceDiscoveryImpl类，调用registerService方法
    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {
        ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
                .<ServiceMeta>builder()
                .name(RpcServiceHelper.buildServiceKey(serviceMeta.getServiceName(), serviceMeta.getServiceVersion()))
                .address(serviceMeta.getServiceAddr())
                .port(serviceMeta.getServicePort())
                .payload(serviceMeta)
                .build();
        serviceDiscovery.registerService(serviceInstance);
    }


    // 先用serviceMeta实例化Curator的ServiceInstance
    // 然后传入ServiceDiscoveryImpl类，调用unregisterService方法
    @Override
    public void unRegister(ServiceMeta serviceMeta) throws Exception {
        ServiceInstance<ServiceMeta> serviceInstance = ServiceInstance
                .<ServiceMeta>builder()
                .name(serviceMeta.getServiceName())
                .address(serviceMeta.getServiceAddr())
                .port(serviceMeta.getServicePort())
                .payload(serviceMeta)
                .build();
        serviceDiscovery.unregisterService(serviceInstance);
    }


    // 以服务名为key，调用Curator的queryForInstances方法去Zookeeper中查找服务信息
    // queryForInstances方法返回一个ServiceInstance列表
    // 调用负载均衡算法，返回instance中的payload属性，其中保存的是serviceMeta
    @Override
    public ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception {
        Collection<ServiceInstance<ServiceMeta>> serviceInstances = serviceDiscovery.queryForInstances(serviceName);
        ServiceInstance<ServiceMeta> instance = new ZKConsistentHashLoadBalancer().select((List<ServiceInstance<ServiceMeta>>) serviceInstances, invokerHashCode);
        if (instance != null) {
            return instance.getPayload();
        }
        return null;
    }

    @Override
    public void destroy() throws IOException {
        serviceDiscovery.close();
    }
}
