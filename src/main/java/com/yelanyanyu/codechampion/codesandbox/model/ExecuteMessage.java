package com.yelanyanyu.codechampion.codesandbox.model;


import lombok.Data;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Data
public class ExecuteMessage {
    private Integer exitValue;
    /**
     * 程序正常执行信息
     */
    private String normalMessage;
    /**
     * 程序错误执行信息
     */
    private String errorMessage;
    /**
     * 程序执行时间
     */
    private Long time;
    private Long memory;
}
