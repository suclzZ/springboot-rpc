package com.sucl.app.consumer;

import com.sucl.app.producer.HelloService;
import com.sucl.rpc.client.RpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author sucl
 * @since 2019/7/16
 */
@RestController
@RequestMapping("/hello")
public class HelloController {
    @Autowired
    private RpcClient client;

    @GetMapping("/{name}")
    public String hello(@PathVariable String name){
        HelloService helloService = client.create(HelloService.class);
        return helloService.hello(name);
    }
}
