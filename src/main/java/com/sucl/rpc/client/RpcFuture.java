package com.sucl.rpc.client;

import com.sucl.rpc.protocol.RpcRequest;
import com.sucl.rpc.protocol.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 客户端调用的真实处理
 * @author sucl
 * @since 2019/7/16
 */
@Slf4j
public class RpcFuture implements Future<Object> {
    private RpcRequest request;
    private RpcResponse response;

    private long startTime;

    private Sync sync;
    private ReentrantLock lock = new ReentrantLock();
    private List<RpcCallback> callbacks = new ArrayList<>();
    private long responseHoldTime = 3*1000;

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.sync = new Sync();
        this.startTime = System.currentTimeMillis();
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        long runTime = startTime - System.currentTimeMillis();
        if(runTime >= responseHoldTime){
            log.warn("response outtime [{}.{}]",request.getClassName(),request.getMethodName());
        }
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            for(RpcCallback callback : callbacks){
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    private void runCallback(RpcCallback callback) {
        RpcResponse res = this.response;
        RpcClient.submit(()->{
            if (!res.isError()) {
                callback.success(res.getResult());
            } else {
                callback.failure(new RuntimeException("Response error", new Throwable(res.getError())));
            }
        });
    }

    public RpcFuture addCallback(RpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.callbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    public Object get() {
        sync.acquire(-1);
        if(response!=null){
            return response.getResult();
        }else{
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getReqId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 1L;

        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean isDone() {
            getState();
            return getState() == done;
        }
    }
}
