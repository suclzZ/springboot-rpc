package com.sucl.rpc;

import com.sucl.rpc.client.RpcClient;
import com.sucl.rpc.discovery.RegProperties;
import com.sucl.rpc.discovery.ServiceDiscovery;
import com.sucl.rpc.discovery.ServiceRegistry;
import com.sucl.rpc.mgt.ServiceCenter;
import com.sucl.rpc.server.RpcServer;
import com.sucl.rpc.server.RpcService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Configuration
@EnableConfigurationProperties(RegProperties.class)
public class RpcConfiguration {
    private final RegProperties properties;

    public RpcConfiguration(RegProperties properties){
        this.properties = properties;
    }

    @Bean
    public ServiceCenter serviceCenter(){
        return new ServiceCenter();
    }

    @Bean
    public ServiceDiscovery serviceDiscovery(){
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery(properties.getAddress(), serviceCenter());
        return serviceDiscovery;
    }

    @Bean
    @DependsOn("serviceDiscovery")
    public ServiceRegistry serviceRegistry(){
        ServiceRegistry serviceRegistry = new ServiceRegistry(properties.getAddress());
        return serviceRegistry;
    }

    @Bean
    public RpcClient client(){
        return new RpcClient(serviceDiscovery());
    }

    @Bean
    public RpcServer server(){
        return new RpcServer(getServerAddress(), serviceRegistry());
    }

    private String getServerAddress() {
        return "localhost:9910";
    }

}
