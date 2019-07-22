package com.sucl.rpc.discovery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author sucl
 * @since 2019/7/14
 */
@Data
@ConfigurationProperties(prefix = "rpc.registry")
public class RegProperties {

    private String address;

}
