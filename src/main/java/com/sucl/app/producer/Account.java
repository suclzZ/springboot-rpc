package com.sucl.app.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author sucl
 * @since 2019/7/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Serializable {

    private String acId;
    private String name;
    private int age;
}
