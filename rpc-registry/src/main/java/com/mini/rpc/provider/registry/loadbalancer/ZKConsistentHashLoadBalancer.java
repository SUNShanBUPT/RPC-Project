package com.mini.rpc.provider.registry.loadbalancer;

import com.mini.rpc.common.ServiceMeta;
import org.apache.curator.x.discovery.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ZKConsistentHashLoadBalancer implements ServiceLoadBalancer<ServiceInstance<ServiceMeta>> {
    private final static int VIRTUAL_NODE_SIZE = 10;
    private final static String VIRTUAL_NODE_SPLIT = "#";

    @Override
    public ServiceInstance<ServiceMeta> select(List<ServiceInstance<ServiceMeta>> servers, int hashCode) {
        // 将节点和虚拟节点放入有TreeMap实现的hash环中
        TreeMap<Integer, ServiceInstance<ServiceMeta>> ring = makeConsistentHashRing(servers);
        return allocateNode(ring, hashCode);
    }

    private ServiceInstance<ServiceMeta> allocateNode(TreeMap<Integer, ServiceInstance<ServiceMeta>> ring, int hashCode) {
        Map.Entry<Integer, ServiceInstance<ServiceMeta>> entry = ring.ceilingEntry(hashCode);

        //hash的结果在尾部，返回头部
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    // 将节点和虚拟节点放入有TreeMap实现的hash环中
    private TreeMap<Integer, ServiceInstance<ServiceMeta>> makeConsistentHashRing(List<ServiceInstance<ServiceMeta>> servers) {
        TreeMap<Integer, ServiceInstance<ServiceMeta>> ring = new TreeMap<>();
        for (ServiceInstance<ServiceMeta> instance : servers) {
            for (int i = 0; i < VIRTUAL_NODE_SIZE; i++) {
                ring.put((buildServiceInstanceKey(instance) + VIRTUAL_NODE_SPLIT + i).hashCode(), instance);
            }
        }
        return ring;
    }

    // 解析出ip:port，拼接成字符串
    private String buildServiceInstanceKey(ServiceInstance<ServiceMeta> instance) {
        // payload中保存的是ServiceMeta
        ServiceMeta payload = instance.getPayload();
        // 通过payload或得ServiceMeta进而获取服务端ip地址和端口号，然后进行拼接
        return String.join(":", payload.getServiceAddr(), String.valueOf(payload.getServicePort()));
    }

}
