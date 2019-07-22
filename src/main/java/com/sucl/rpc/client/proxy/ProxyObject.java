package com.sucl.rpc.client.proxy;

import com.sucl.rpc.client.RpcClientHandler;
import com.sucl.rpc.client.RpcFuture;
import com.sucl.rpc.mgt.ServiceCenter;
import com.sucl.rpc.protocol.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Slf4j
public class ProxyObject<T> implements InvocationHandler ,AsyncObjectProxy{
    private Class<T> clazz;
    private ServiceCenter serviceCenter;

    public ProxyObject(ServiceCenter serviceCenter, Class<T> clazz){
        this.serviceCenter = serviceCenter;
        this.clazz = clazz;
    }

    /**
     * 创建代理对象
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        RpcRequest request = new RpcRequest();
        request.setReqId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        RpcClientHandler handler = serviceCenter.chooseHandler();
        RpcFuture future = handler.sendRequest(request);
        return future.get();
    }

    /**
     * 创建future对象
     * @param methodName
     * @param args
     * @return
     */
    @Override
    public RpcFuture call(String methodName, Object... args) {
        RpcClientHandler handler = serviceCenter.chooseHandler();
        RpcRequest request = createRequest(this.clazz.getName(),methodName,args);
        RpcFuture future = handler.sendRequest(request);
        return future;
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setReqId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);

        Class[] parameterTypes = new Class[args.length];
        // Get the right class type
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = getClassType(args[i]);
        }
        request.setParameterTypes(parameterTypes);
        return request;
    }

    private Class getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        switch (typeName) {
            case "java.lang.Integer":
                return Integer.TYPE;
            case "java.lang.Long":
                return Long.TYPE;
            case "java.lang.Float":
                return Float.TYPE;
            case "java.lang.Double":
                return Double.TYPE;
            case "java.lang.Character":
                return Character.TYPE;
            case "java.lang.Boolean":
                return Boolean.TYPE;
            case "java.lang.Short":
                return Short.TYPE;
            case "java.lang.Byte":
                return Byte.TYPE;
        }

        return classType;
    }
}
