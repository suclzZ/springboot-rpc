package com.sucl.rpc.client.proxy;

import com.sucl.rpc.client.RpcFuture;

/**
 * @author sucl
 * @since 2019/7/16
 */
public interface AsyncObjectProxy {

    RpcFuture call(String methodName, Object ... args);
}
