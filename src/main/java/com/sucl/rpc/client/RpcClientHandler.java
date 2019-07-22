package com.sucl.rpc.client;

import com.sucl.rpc.protocol.RpcRequest;
import com.sucl.rpc.protocol.RpcResponse;
import com.sun.org.apache.regexp.internal.RE;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
@Data
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private Channel channel;

    /**
     * 该处理器的服务提供者
     */
    private SocketAddress serverAddr;

    private Map<String,RpcFuture> pendingRpc = new HashMap<>();

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.serverAddr = this.channel.remoteAddress();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("rpc client handle error",cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        RpcFuture rpcFuture = pendingRpc.get(msg.getReqId());
        if(pendingRpc != null ){
            pendingRpc.remove(msg.getReqId());
            rpcFuture.done(msg);
        }
    }

    /**
     * ??
     */
    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 发送请求
     * 真正的调用在代理类中完成
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture future = new RpcFuture(request);
        pendingRpc.put(request.getReqId(),future);
        CountDownLatch downLatch = new CountDownLatch(1);
        this.channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                downLatch.countDown();
            }
        });
        try {
            downLatch.await();
        } catch (InterruptedException e) {
//            e.printStackTrace();
            log.error(e.getMessage());
        }
        return future;
    }
}
