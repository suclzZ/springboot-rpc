package com.sucl.rpc.protocol;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sucl
 * @since 2019/7/15
 */
@Data
public class RpcResponse implements Serializable {
    private String reqId;
    private Object result;
    private String error;

    public boolean isError(){
        return error!=null;
    }
}
