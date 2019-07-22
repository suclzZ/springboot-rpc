package com.sucl.rpc.client;

import com.sucl.rpc.client.proxy.AsyncObjectProxy;
import com.sucl.rpc.client.proxy.ProxyObject;
import com.sucl.rpc.discovery.ServiceDiscovery;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author sucl
 * @since 2019/7/14
 */
public class RpcClient {

    private ServiceDiscovery serviceDiscovery;

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    public RpcClient(ServiceDiscovery serviceDiscovery){
        this.serviceDiscovery = serviceDiscovery;
    }

    public static void submit(Runnable task){
        threadPoolExecutor.submit(task);
    }

    /**
     * 获取服务代理->RpcRequest
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public <T> T  create(Class<T> interfaceClass){
        return (T)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{interfaceClass},
                new ProxyObject<T>(serviceDiscovery.getServiceCenter(),interfaceClass));
    }

    public <T> AsyncObjectProxy  asyncProxy(Class<T> interfaceClass){
        return new ProxyObject<T>(serviceDiscovery.getServiceCenter(),interfaceClass);
    }

    public void stop(){
        serviceDiscovery.close();
        threadPoolExecutor.shutdown();
    }
}
