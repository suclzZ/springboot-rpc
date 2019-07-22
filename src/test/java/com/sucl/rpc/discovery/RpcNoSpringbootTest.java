package com.sucl.rpc.discovery;

import com.sucl.app.producer.HelloService;
import com.sucl.app.producer.HelloServiceImpl;
import com.sucl.rpc.client.RpcCallback;
import com.sucl.rpc.client.RpcClient;
import com.sucl.rpc.client.RpcFuture;
import com.sucl.rpc.client.proxy.AsyncObjectProxy;
import com.sucl.rpc.mgt.ServiceCenter;
import com.sucl.rpc.server.RpcServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class RpcNoSpringbootTest {
    private ServiceCenter center;
    private ServiceDiscovery discovery;
    private ServiceRegistry registry;

    @Before
    public void before(){
        //服务中心 主要管理生产的服务；提供服务消费；构建netty客户端
        center = new ServiceCenter();
        //服务发现 监听zk，获取注册的服务，并同步到服务中心
        discovery = new ServiceDiscovery("localhost:2181",center);
        //服务注册 将服务地址（或更细粒度）注册到zk
        registry = new ServiceRegistry("localhost:2181");
    }

    @Test
    public void handler(){
        String serverAddr = "localhost:9999";
        //rpc服务端，提供服务；构建netty服务端
        RpcServer server = new RpcServer(serverAddr,registry);
        server.addHandler(HelloService.class.getName(),new HelloServiceImpl());
        //注册服务地址
        registry.registry(serverAddr);

        new Thread(()->{
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //rpc客户者，消费服务；
            RpcClient client = new RpcClient(discovery);

//            HelloService helloService = client.create(HelloService.class);
//            String result = helloService.hello("rpc");
//
//            System.err.println(">>>>>>>>>>>>>>>>>> "+result);

            AsyncObjectProxy helloService2 = client.asyncProxy(HelloService.class);
            RpcFuture future = helloService2.call("hello", new String[]{"rpc"});
            future.addCallback(new RpcCallback() {
                @Override
                public void success(Object resut) {
                    System.out.println(">>>>>>>>>>>>>>>> "+resut);
                }

                @Override
                public void failure(Exception e) {
                    System.out.println("---------------- "+e);
                }
            });

        }).start();

        server.start();
    }

}
