package com.sucl.rpc.handler;

import com.sucl.rpc.protocol.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.util.SerializationUtils;

import java.util.List;

/**
 * Unpooled获取不同（三种）类型的ByteBuf
 *  1、缓存数组 Unpooled.buffer()
 *  2、直接 Unpooled.directBuffer()
 *  3、组合 Unpooled.compositeBuffer()
 *
 * ByteBufAllocator 按需分配
 * Unpooled 缓冲区
 * ByteBufUtil
 *
 * @author sucl
 * @since 2019/7/15
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private Class clazz;

    public RpcDecoder(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableSize = in.readableBytes();
        if(readableSize < 4){
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();

        if(readableSize < dataLength){
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        Object msg = SerializationUtils.deserialize(data);
        out.add(msg);
    }
}
