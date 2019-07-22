package com.sucl.rpc.discovery;

import com.sucl.app.producer.HelloService;
import com.sucl.rpc.client.RpcClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author sucl
 * @since 2019/7/17
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RpcWithSpringbootTest {
    @Autowired
    private RpcClient client;

    @Test
    public void handle(){
        new Thread(()->{
            HelloService helloService = client.create(HelloService.class);
            System.out.println(">>>>>>>>>>> " + helloService.hello("rpc test"));
        }).start();
    }

}
