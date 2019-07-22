package com.sucl.rpc.discovery;

import com.sucl.rpc.mgt.ServiceCenter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务发现
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
@Data
public class ServiceDiscovery {
    private RegProperties properties;

    private String registryAddress;

    private ZooKeeper zk;

    private List<String> serviceAddrs;

    private ServiceCenter serviceCenter;

    public ServiceDiscovery(String registryAddress,ServiceCenter serviceCenter){
        this.registryAddress = registryAddress;
        this.serviceCenter = serviceCenter;
        ZooKeeper zk = ZkHelper.connect(getRegistryAddress());
        if(zk != null){
            watchNode(zk);
        }
    }

    public String getRegistryAddress() {
        if(registryAddress == null){
            if(properties == null){
                log.error("registryAddress and properties both are null.");
            }else{
                registryAddress = properties.getAddress();
            }
        }
        return registryAddress;
    }

    /**
     * 监听zk root节点下的子节点变化，然后进行对应处理
     * 1、修改已连接的服务
     * 2、同步更新netty服务连接
     * @param zk
     */
    private void watchNode(ZooKeeper zk) {
        System.out.println(
                "xxxxxxxxxxxxxxxxxxxxx"
        );
        try {
            List<String> childPaths = zk.getChildren(RpcConstant.REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getType() == Event.EventType.NodeChildrenChanged){
                        watchNode(zk);
                    }
                }
            });
            List<String> servicies = new ArrayList<>();
            if(childPaths!=null && childPaths.size()>0){
                for(String path : childPaths){
                    byte[] serverAddr = zk.getData(RpcConstant.REGISTRY_PATH + "/" + path, false, null);
                    servicies.add(new String(serverAddr));
                }
                log.info("service updateed {}",childPaths);
                this.serviceAddrs = servicies;
            }

            log.info("update connected service [{}].",this.serviceAddrs);
            updateConnectedService();
        } catch (KeeperException | InterruptedException e) {
//            e.printStackTrace();
            log.error("",e);
        }
    }

    /**
     * 更新所有netty服务连接
     */
    private void updateConnectedService() {
        serviceCenter.updateService(this.serviceAddrs);
    }

    /**
     * 随机获取一个服务地址
     * 如何知道每个服务地址有哪些服务？
     * @return
     */
    public String discover(){
        String addr = null;
        if(serviceAddrs != null && serviceAddrs.size() > 0){
            if(serviceAddrs.size() == 0){
                addr = serviceAddrs.get(0);
                log.debug("using only data: {}", addr);
            }else{
                addr = serviceAddrs.get(ThreadLocalRandom.current().nextInt(serviceAddrs.size()));
                log.debug("using random data: {}", addr);
            }
        }
        return addr;
    }

    public void close(){
        if(zk != null){
            try {
                zk.close();
            } catch (InterruptedException e) {
//                e.printStackTrace();
                log.error("zookeeper close error.",e);
            }
        }
    }

}
