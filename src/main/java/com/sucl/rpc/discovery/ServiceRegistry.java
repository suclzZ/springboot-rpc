package com.sucl.rpc.discovery;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务注册
 * @author sucl
 * @since 2019/7/14
 */
@Slf4j
@Data
public class ServiceRegistry {
    private RegProperties properties;
    private String  registryAddress;

    public ServiceRegistry(String registryAddress){
        this.registryAddress = registryAddress;
    }

    public ServiceRegistry(RegProperties properties){
        this.properties = properties;
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
     * 注册服务
     * @param serviceAddr
     */
    public void registry(String serviceAddr){
        if(serviceAddr != null){
            if(log.isDebugEnabled()){
                log.debug("begin regist server {}",serviceAddr);
            }
            if(!checkServiceAddr(serviceAddr)){
                log.error("service address [{}] formate error.",serviceAddr);
            }
            ZooKeeper zk =  ZkHelper.connect(getRegistryAddress());
            if(zk != null){
                createRegistryPath(zk);
                saveServiceData(zk, serviceAddr);
            }
        }
    }

    private boolean checkServiceAddr(String serviceAddr) {
        Pattern pattern = Pattern.compile(".+(:)\\d+");
        Matcher matcher = pattern.matcher(serviceAddr);
        matcher.matches();
        return true;
    }

    /**
     * zk根路径不存在，则创建
     * @param zk
     */
    private void createRegistryPath(ZooKeeper zk) {
        try {
            if(zk.exists(RpcConstant.REGISTRY_PATH,null)==null){
                zk.create(RpcConstant.REGISTRY_PATH,"rpc".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
                if(log.isDebugEnabled()){
                    log.debug("zookeeper root node {} don't exist. create success",RpcConstant.REGISTRY_PATH);
                }
            }
        } catch (KeeperException e) {
//            e.printStackTrace();
            log.error("",e);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            log.error("",e);
        }
    }

    /**
     * 保存服务地址到zk根节点下的子路径中
     * 子节点类型为临时顺序
     * @param zk
     * @param serviceAddr
     */
    private void saveServiceData(ZooKeeper zk, String serviceAddr) {
        try {
            String cpath = zk.create(RpcConstant.DATA_PATH, serviceAddr.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("create zookeeper node [{}=>{}] complete.",cpath,serviceAddr);
        } catch (KeeperException e) {
//            e.printStackTrace();
            log.error("",e);
        } catch (InterruptedException e) {
//            e.printStackTrace();
            log.error("",e);
        }
    }

}
