package com.mini.rpc.handler;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


// rpc请求处理类
public class RpcRequestProcessor {
    private static ThreadPoolExecutor threadPoolExecutor;

    // 新建线程池
    public static void submitRequest(Runnable task) {
        if (threadPoolExecutor == null) {
            synchronized (RpcRequestProcessor.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }
}
