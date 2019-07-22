package com.sucl.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sucl
 * @since 2019/7/15
 */
@Data
public class RpcRequest implements Serializable {
    private String reqId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
