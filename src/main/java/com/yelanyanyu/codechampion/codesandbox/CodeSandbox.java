package com.yelanyanyu.codechampion.codesandbox;


import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
public interface CodeSandbox {
    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest);
}
