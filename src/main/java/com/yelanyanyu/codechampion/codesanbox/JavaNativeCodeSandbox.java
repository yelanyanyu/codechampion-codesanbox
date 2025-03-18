package com.yelanyanyu.codechampion.codesanbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeRequest;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteCodeResponse;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Component
public class JavaNativeCodeSandbox implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    @Value("${codesandbox.compile-config.java-class-name}")
    private String javaClassName;

    /**
     * 分批获取进程的正常输出
     *
     * @param compiledProcess
     * @return
     */
    private static StringBuilder getCompiledOutputFromCmd(Process compiledProcess) {
        StringBuilder builder = new StringBuilder();
        new BufferedReader(new InputStreamReader(compiledProcess.getInputStream()))
                .lines().forEach((s -> builder.append(s).append("\n")));
        return builder;
    }

    private static ExecuteMessage runProcessAndGetMsg(Process runProcess, String opName) throws InterruptedException {
        ExecuteMessage executeMessage = new ExecuteMessage();
        int exitValue = runProcess.waitFor();
        executeMessage.setExitValue(exitValue);
        if (exitValue == 0) {
            // 正常退出
            System.out.println(opName + "成功");

            String normalCompileOutput = new BufferedReader(
                    new InputStreamReader(
                            runProcess.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));
            executeMessage.setNormalMessage(normalCompileOutput);
        } else {
            // 发生错误
            System.out.println(opName + "失败，错误码" + exitValue);
            // 逐行获取编译的正确信息
            StringBuilder builder = getCompiledOutputFromCmd(runProcess);
            // 逐行获取编译的错误信息
            String errorCompileOutput = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));
            executeMessage.setErrorMessage(errorCompileOutput);
        }
        return executeMessage;
    }

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

        // 返回项目的根目录: D:\myCode\formal-projects\codechampion-codesanbox
        String userDir = System.getProperty("user.dir");
        // 用于存储用户提交代码的总目录
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 将用户代码隔离存放，具体方法是为每个用户生成一个临时文件夹，再将用户执行的相关文件放入这个文件夹
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 得到用户的代码的绝对路劲，并将用户提交的字符串 code 写入这个文件
        String userCodePath = userCodeParentPath + File.separator + javaClassName;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        // 编译代码
        String compiledCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());

        try {
            Process compiledProcess = Runtime.getRuntime().exec(compiledCmd);
            ExecuteMessage executeMessage = runProcessAndGetMsg(compiledProcess, "编译");
            System.out.println(executeMessage);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 执行代码
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = runProcessAndGetMsg(runProcess, "运行");
                System.out.println(executeMessage);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return executeCodeResponse;
    }
}
