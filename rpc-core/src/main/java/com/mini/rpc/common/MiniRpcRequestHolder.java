package com.mini.rpc.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// 存放客服端PRC请求的列表，key为AtomicLong，代表请求编号，value为rpc执行结果Promise包装类，其中包括任务执行的情况
public class MiniRpcRequestHolder {
    // 采用atomic类生成请求的序列号
    public final static AtomicLong REQUEST_ID_GEN = new AtomicLong(0);

    public static final Map<Long, MiniRpcFuture<MiniRpcResponse>> REQUEST_MAP = new ConcurrentHashMap<>();
}
