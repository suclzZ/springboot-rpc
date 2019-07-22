package com.sucl.rpc.server;

import java.lang.annotation.*;

/**
 * @author sucl
 * @since 2019/7/15
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {

    Class<?> value();
}
