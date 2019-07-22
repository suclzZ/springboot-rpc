package com.sucl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author sucl
 * @since 2019/7/16
 */
@SpringBootApplication
//@ComponentScan("com.sucl")
public class StartApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class,args);
    }
}
