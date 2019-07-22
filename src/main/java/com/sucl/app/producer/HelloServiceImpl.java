package com.sucl.app.producer;

import com.sucl.rpc.server.RpcService;
import org.springframework.stereotype.Service;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Service
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService{
    @Override
    public String hello(String name) {
        return "hello "+name+ "!";
    }
}
