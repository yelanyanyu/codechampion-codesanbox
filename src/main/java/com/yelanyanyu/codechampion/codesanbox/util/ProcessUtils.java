package com.yelanyanyu.codechampion.codesanbox.util;


import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import com.yelanyanyu.codechampion.codesanbox.model.ExecuteMessage;

import java.io.*;
import java.util.stream.Collectors;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
public class ProcessUtils {
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

    public static ExecuteMessage runProcessAndGetMsg(Process runProcess, String opName) throws InterruptedException {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
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
        stopWatch.stop();
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        return executeMessage;
    }

    /**
     * 通过 Scanner 方式获取输入的代码
     *
     * @param runProcess
     * @param args       以字符串形式的输入数组
     * @return
     * @throws InterruptedException
     */
    public static ExecuteMessage runProcessAndGetMsgWithInteraction(Process runProcess, String args) throws InterruptedException {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            //向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //相当于按下回车，执行发送
            outputStreamWriter.flush();
            //分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            //逐行读取
            String compileOutputLine = getCompiledOutputFromCmd(runProcess).toString();
            executeMessage.setNormalMessage(compileOutputLine);
            //记得资源释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopWatch.stop();
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        return executeMessage;
    }
}
