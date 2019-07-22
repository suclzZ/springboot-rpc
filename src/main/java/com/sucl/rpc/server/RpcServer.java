package com.sucl.rpc.server;

import com.sucl.rpc.discovery.ServiceRegistry;
import com.sucl.rpc.handler.RpcDecoder;
import com.sucl.rpc.handler.RpcEncoder;
import com.sucl.rpc.protocol.RpcRequest;
import com.sucl.rpc.protocol.RpcResponse;
import com.sun.org.apache.regexp.internal.RE;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * rpc服务
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
public class RpcServer implements InitializingBean,ApplicationContextAware {
    /**
     * 服务地址
     */
    private String serverAddress;
    /**
     * 服务注册
     */
    private ServiceRegistry serviceRegistry;

    private static ThreadPoolExecutor threadPoolExecutor;

    private boolean isRegistry = true;

    private NioEventLoopGroup bossLoopGroup;
    private NioEventLoopGroup workLoopGroup;

    /**
     * 记录所有接口与bean的映射
     */
    private Map<String,Object> handlerMap = new ConcurrentHashMap<>();

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry){
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    public static void submit(Runnable task){
        if(threadPoolExecutor == null){
            synchronized(RpcServer.class){
                if(threadPoolExecutor == null){
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }

    /**
     * 创建一个netty服务
     */
    public void start(){
        if(bossLoopGroup == null && workLoopGroup == null){
            bossLoopGroup = new NioEventLoopGroup();
            workLoopGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossLoopGroup,workLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,100)
                    .option(ChannelOption.SO_RCVBUF,10*1024)
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    .addLast(new RpcServerHandler(handlerMap));
                        }
                    });
            try {
                String[] serverParts = StringUtils.split(this.serverAddress, ":");
                ChannelFuture future = bootstrap.bind(Integer.valueOf(serverParts[1])).sync();

                if(isRegistry){
                    log.info("server registry ad addresss [{}:{}]",serverParts);
                    serviceRegistry.registry(this.serverAddress);
                }

                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加服务，接口=>bean
     * @param interfaceName
     * @param bean
     */
    public void addHandler(String interfaceName,Object bean){
        if(!handlerMap.containsKey(interfaceName)){
            log.info("add server [{}]",interfaceName);
            handlerMap.put(interfaceName,bean);
        }
    }

    /**
     * bean创建后建立对应服务连接
     */
    @Override
    public void afterPropertiesSet(){
        if(invalidServerAddr(serverAddress)){
            start();
        }else{
            log.error("server address [{}] error",serverAddress);
        }
    }

    private boolean invalidServerAddr(String serAddr) {
        if(serAddr == null){
            return false;
        }else{
            Pattern pattern = Pattern.compile(".*(:)\\d+$");
            return pattern.matcher(serAddr).matches();
        }
    }

    /**
     * 查找系统中的所有服务
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(!CollectionUtils.isEmpty(beanMap)){
            for(Map.Entry<String,Object> entry : beanMap.entrySet()){
                Object bean = entry.getValue();
                log.info("init server bean [{}] of rpc.",bean);
                Class<?> targetClass = bean.getClass().getAnnotation(RpcService.class).value();
                if(targetClass != null){
                    handlerMap.put(targetClass.getName(),bean);
                }else{
                    handlerMap.put(bean.getClass().getName(),bean);
                }
            }
        }
    }

    public void setRegistry(boolean registry) {
        isRegistry = registry;
    }
}
