package com.yelanyanyu.codechampion.codesandbox;


import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate{
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        return super.execute(executeCodeRequest);
    }
}
