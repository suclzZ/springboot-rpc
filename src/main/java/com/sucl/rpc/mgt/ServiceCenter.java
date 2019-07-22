package com.sucl.rpc.mgt;

import com.sucl.rpc.client.RpcClientHandler;
import com.sucl.rpc.handler.RpcDecoder;
import com.sucl.rpc.handler.RpcEncoder;
import com.sucl.rpc.protocol.RpcRequest;
import com.sucl.rpc.protocol.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 服务中心
 * 主要监控服务端变化同步更新客户端连接与handler
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
public class ServiceCenter {

    /**
     * 已连接服务地址
     */
    private Map<SocketAddress,RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    /**
     * 已有的处理器
     */
    private CopyOnWriteArrayList<RpcClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    /**
     * handler操作同步锁
     */
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();

    /**
     * netty 客户端线程池
     */
    private NioEventLoopGroup workLoopGroup = new NioEventLoopGroup();

    /**
     * 获取handler超时时间
     */
    private long connectTimeoutMillis = 3*1000;

    /**
     * 实现handler的获取机制
     */
    private AtomicInteger roundRobin = new AtomicInteger(0);

    /**
     * 记录服务中心运行状态
     */
    private volatile boolean isRunning = true;


    /**
     * 更新全局服务，当zk监听到节点变化时触发
     * @param currentServerAddrs
     */
    public void updateService(List<String> currentServerAddrs){
        if(!CollectionUtils.isEmpty(currentServerAddrs)){
            List<InetSocketAddress> needAddServerAddrs = new ArrayList<>();//需要新增的服务
            for(String curServer : currentServerAddrs){
                String[] netServerParts = StringUtils.split(curServer, ":");
                if(netServerParts.length == 2){
                    InetSocketAddress address = new InetSocketAddress(netServerParts[0],Integer.valueOf(netServerParts[1]));
                    needAddServerAddrs.add(address);
                }
            }
            //新增
            for(InetSocketAddress addr : needAddServerAddrs){
                if(!connectedServerNodes.keySet().contains(addr)){
                    addServer(addr);
                }
            }

            //处理(包括对无效服务与处理器清理)
            for(RpcClientHandler handler : connectedHandlers){
                SocketAddress  netServer = handler.getServerAddr();
                if(!needAddServerAddrs.contains(netServer)){
                    log.info("clean connected handler [{}]",handler);
                    RpcClientHandler hadHandler = connectedServerNodes.get(netServer);
                    if(hadHandler != null){
                        hadHandler.close();
                    }
                    connectedServerNodes.remove(netServer);
                    connectedHandlers.remove(handler);
                }
            }
        }else{
            //全部清除
            for(RpcClientHandler handler : connectedHandlers){
                SocketAddress  netServer = handler.getServerAddr();
                RpcClientHandler hadHandler = connectedServerNodes.get(netServer);
                hadHandler.close();
                connectedServerNodes.remove(netServer);
            }
            connectedHandlers.clear();
        }
    }

    /**
     * 添加到服务端的连接
     * @param addr
     */
    private void addServer(InetSocketAddress addr) {
        threadPoolExecutor.submit(()->{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_SNDBUF,10*1024)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new RpcEncoder(RpcRequest.class))
                                    .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,0))
                                    .addLast(new RpcDecoder(RpcResponse.class))
                                    .addLast(new RpcClientHandler());
                        }
                    });
            try {
                ChannelFuture future = bootstrap.connect(addr).sync();
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        log.info("connect server [{}] successfully.",addr);
                        RpcClientHandler handler = future.channel().pipeline().get(RpcClientHandler.class);
                        addHandler(handler);
                    }
                });
            } catch (InterruptedException e) {
                log.error("client connect [{}] error",addr);
            }
        });
    }

    private void addHandler(RpcClientHandler handler) {
        connectedHandlers.add(handler);
        connectedServerNodes.put(handler.getServerAddr(),handler);
        signalAvailableHandler();
    }

    /**
     * 添加、清除handler，同步
     */
    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 选择handler时，同步
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @return
     */
    public RpcClientHandler chooseHandler(){
        int handlerSize = connectedHandlers.size();
        while (isRunning && handlerSize <= 0){
            try {
                boolean available = waitingForHandler();
                if(available){
                    handlerSize = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                log.error("",e);
//                e.printStackTrace();
            }
        }
        int index = (roundRobin.addAndGet(1) + handlerSize)%handlerSize;
        return connectedHandlers.get(index);
    }

    /**
     * 重连
     * @param handler
     */
    public void reConnect(RpcClientHandler handler){
        connectedHandlers.remove(handler);
        connectedServerNodes.remove(handler.getServerAddr());
        addHandler(handler);
    }

    /**
     * 停止服务中心
     */
    public void stop(){
        isRunning = false;
        for(RpcClientHandler handler : connectedHandlers){
            handler.close();
        }
        signalAvailableHandler();
        workLoopGroup.shutdownGracefully();
        threadPoolExecutor.shutdown();
    }
}
