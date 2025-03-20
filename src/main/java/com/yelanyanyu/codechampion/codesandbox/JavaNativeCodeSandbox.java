package com.yelanyanyu.codechampion.codesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteCodeResponse;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteMessage;
import com.yelanyanyu.codechampion.codesandbox.model.JudgeInfo;
import com.yelanyanyu.codechampion.codesandbox.util.ProcessUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.yelanyanyu.codechampion.codesandbox.util.ProcessUtils.runProcessAndGetMsg;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Component
public class JavaNativeCodeSandbox implements com.yelanyanyu.codechampion.codesandbox.CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    @Value("${codesandbox.compile-config.java-class-name}")
    private String javaClassName;


    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
/*
        明确代码沙箱的执行流程：
        1.  利用 `javac` 命令对 `Main.java` 进行编译；
        2. 利用 `java`  命令执行该代码：
        1. 执行成功，则将控制台输出格式化填入 response，并且返回控制台信息赋值给 message，和 judgeInfo。
        2. 如果执行失败，则从从控制台获取失败信息填入 response；
*/
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        List<String> inputList = executeCodeRequest.getInput();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 返回项目的根目录: D:\myCode\formal-projects\codechampion-codesandbox
        String userDir = System.getProperty("user.dir");
        // 用于存储用户提交代码的总目录
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 将用户代码隔离存放，具体方法是为每个用户生成一个临时文件夹，再将用户执行的相关文件放入这个文件夹
        String userCodeParentPathDir = globalCodePathName + File.separator + UUID.randomUUID();
        // 得到用户的代码的绝对路劲，并将用户提交的字符串 code 写入这个文件
        String userCodePath = userCodeParentPathDir + File.separator + javaClassName;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        // 编译代码
        String compiledCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());

        try {
            Process compiledProcess = Runtime.getRuntime().exec(compiledCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMsg(compiledProcess, "编译");
            System.out.println(executeMessage);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 执行代码
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPathDir, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = runProcessAndGetMsg(runProcess, "运行");
//                ExecuteMessage executeMessage = runProcessAndGetMsgWithInteraction(runProcess, "1 2");
                System.out.println(executeMessage);
                executeMessages.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 收集信息
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessages) {
            String errorMessage = executeMessage.getErrorMessage();
            if (!StrUtil.isBlankIfStr(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getNormalMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }

        if (outputList.size() == executeMessages.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutput(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        // 清理执行文件
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPathDir);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return executeCodeResponse;
    }
}
