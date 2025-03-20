package com.yelanyanyu.codechampion.codesandbox.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExecuteCodeResponse {
    private List<String> output;
    /**
     * 程序执行信息
     */
    private String message;
    private Integer status;
    /**
     * 程序执行信息以及资源消耗
     */
    private JudgeInfo judgeInfo;
}
