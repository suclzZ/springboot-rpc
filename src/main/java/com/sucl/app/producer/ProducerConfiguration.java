package com.sucl.app.producer;

import com.sucl.rpc.RpcConfiguration;
import com.sucl.rpc.server.RpcServer;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Configuration
@AutoConfigureAfter(RpcConfiguration.class)
public class ProducerConfiguration implements ApplicationContextAware {

    private RpcServer rpcServer;

    public ProducerConfiguration(RpcServer rpcServer){
        this.rpcServer = rpcServer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        AccountService accountService = applicationContext.getBean(AccountService.class);
        rpcServer.addHandler(AccountService.class.getName(),accountService);
    }
}
