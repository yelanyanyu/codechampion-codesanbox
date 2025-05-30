package com.yelanyanyu.codechampion.codesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yelanyanyu.codechampion.codesandbox.manager.DockerManager;
import com.yelanyanyu.codechampion.codesandbox.model.*;
import com.yelanyanyu.codechampion.codesandbox.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    protected static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    @Value("${codesandbox.compile-config.java-class-name}")
    protected String javaClassName;
    @Value("${codesandbox.compile-config.timeout: 5000L}")
    protected int timeout;
    @Resource
    protected DockerManager dockerManager;

    public File saveCodeToFile(String code) {
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
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 编译用户代码
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compiledCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        ExecuteMessage executeMessage = null;
        try {
            Process compiledProcess = Runtime.getRuntime().exec(compiledCmd);
            executeMessage = ProcessUtils.runProcessAndGetMsg(compiledProcess, "编译");
            log.info("compileMsg: {}", executeMessage);
            return executeMessage;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPathDir = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPathDir, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = runProcessAndGetMsg(runProcess, "运行");
//                ExecuteMessage executeMessage = runProcessAndGetMsgWithInteraction(runProcess, "1 2");
                log.info("runMsg: {}", executeMessage);
                executeMessages.add(executeMessage);
                return executeMessages;
            } catch (IOException | InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        return executeMessages;
    }

    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        JudgeInfo judgeInfo = new JudgeInfo();
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (!StrUtil.isBlankIfStr(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getNormalMessage());
            Long time = executeMessage.getTime();
            Long memory = executeMessage.getMemory();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }

        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);

        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    public boolean deleteCodeFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPathDir = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPathDir);
            log.info("删除{}", del ? "成功" : "失败");
            return del;
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        try {
            List<String> inputList = executeCodeRequest.getInputList();
            String code = executeCodeRequest.getCode();
            String language = executeCodeRequest.getLanguage();

            File userCodeFile = saveCodeToFile(code);

            // 编译代码
            ExecuteMessage compileMessage = compileFile(userCodeFile);
            log.info("compileMsg: {}", compileMessage);

            // 编译错误处理
            if (compileMessage != null && !StrUtil.isBlankIfStr(compileMessage.getErrorMessage())) {
                // 有编译错误
                ExecuteCodeResponse errorResponse = getCompileErrorResponse(compileMessage);
                // 删除用户代码文件
                deleteCodeFile(userCodeFile);
                return errorResponse;
            }

            // 运行代码
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

            // 获取输出结果
            ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);

            // 删除用户代码文件
            boolean b = deleteCodeFile(userCodeFile);
            if (!b) {
                log.error("deleteFile error,userCodeFilePath = {}", userCodeFile.getAbsolutePath());
            }
            return outputResponse;
        } catch (Exception e) {
            log.error("代码执行异常", e);
            return getErrorResponse(e);
        }
    }

    /**
     * 处理编译错误，生成编译错误响应
     *
     * @param compileMessage 编译信息
     * @return 编译错误响应
     */
    private ExecuteCodeResponse getCompileErrorResponse(ExecuteMessage compileMessage) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage("编译错误: " + compileMessage.getErrorMessage());
        // 状态码2表示代码编译错误
        executeCodeResponse.setStatus(2);

        // 设置编译错误的判题信息
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.COMPILE_ERROR.getValue());
        judgeInfo.setTime(0L);
        judgeInfo.setMemory(0L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        
        return executeCodeResponse;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        //表示代码沙箱错误（系统错误）
        executeCodeResponse.setStatus(3);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
