package com.sucl.rpc.client;

/**
 * @author sucl
 * @since 2019/7/16
 */
public interface RpcCallback {

    void success(Object resut);

    void failure(Exception e);
}
