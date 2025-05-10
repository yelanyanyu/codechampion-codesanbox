package com.yelanyanyu.codechampion.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@SpringBootTest
@Slf4j
class JavaDockerCodeSandboxTest {
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;
    @Test
    void dockerCodeSandbox() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        String code = ResourceUtil.readStr("simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.execute(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Test
    void dockerCodeSandboxOfSimpleComputeArgs() {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        String code = ResourceUtil.readStr("simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.execute(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
