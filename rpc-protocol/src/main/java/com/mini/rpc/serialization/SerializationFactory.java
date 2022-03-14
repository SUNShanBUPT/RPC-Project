package com.mini.rpc.serialization;


// 根据传入的byte的大小的序列化方法标记，到序列化方法枚举类SerializationTypeEnum中匹配序列化方法
// 因为Java7之前，switch中不能使用String
// Java7之后可以使用String，但是其实是转化成了String的HashCode
// 采用枚举类来匹配序列化方法
public class SerializationFactory {

    public static RpcSerialization getRpcSerialization(byte serializationType) {
        // 匹配枚举类
        SerializationTypeEnum typeEnum = SerializationTypeEnum.findByType(serializationType);

        switch (typeEnum) {
            case HESSIAN:
                return new HessianSerialization();
            case JSON:
                return new JsonSerialization();
            default:
                throw new IllegalArgumentException("serialization type is illegal, " + serializationType);
        }
    }
}
