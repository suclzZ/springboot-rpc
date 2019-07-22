package com.sucl.rpc.handler;

import com.sucl.rpc.protocol.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.util.SerializationUtils;

/**
 * @author sucl
 * @since 2019/7/15
 */
public class RpcEncoder extends MessageToByteEncoder {

    private Class clazz;

    public RpcEncoder(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if(clazz.isInstance(msg)){
            byte[] data = SerializationUtils.serialize(msg);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
