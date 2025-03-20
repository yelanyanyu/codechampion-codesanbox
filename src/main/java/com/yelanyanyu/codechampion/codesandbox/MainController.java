package com.yelanyanyu.codechampion.codesandbox;


import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@RestController("/")
@Slf4j
public class MainController {
    //定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "secretKey";
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @PostMapping("executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                    HttpServletResponse response,
                                    HttpServletRequest request) {
        log.info("executeRequest: {}", executeCodeRequest);
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!authHeader.equals(AUTH_REQUEST_SECRET)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        if (executeCodeRequest == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            log.info("请求参数为空");
            return null;
        }

        if (executeCodeRequest.getInputList() == null || executeCodeRequest.getInputList().isEmpty()) {
            List<String> inputList = new ArrayList<>();
            inputList.add("");
            executeCodeRequest.setInputList(inputList);
        }
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandbox.execute(executeCodeRequest);
        log.info("executeCodeResponse: {}", executeCodeResponse);
        return executeCodeResponse;
    }
}
