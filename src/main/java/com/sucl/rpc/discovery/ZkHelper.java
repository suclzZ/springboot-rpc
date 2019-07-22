package com.sucl.rpc.discovery;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.omg.CORBA.TIMEOUT;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

/**
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
public class ZkHelper {

    public static ZooKeeper connect(String address){
        CountDownLatch downLatch = new CountDownLatch(1);
        try {
            ZooKeeper zk = new ZooKeeper(address, RpcConstant.CONNECT_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getState() ==  SyncConnected){
                        downLatch.countDown();
                    }
                }
            });
            downLatch.await(10,TimeUnit.SECONDS);
            log.info("connect zookeeper [{}] successed!",address);
            return zk;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        throw new RuntimeException("connect zookeeper [{}] failure!");
        return null;
    }
}
