package com.yelanyanyu.codechampion.codesanbox;


import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeResponse;

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
