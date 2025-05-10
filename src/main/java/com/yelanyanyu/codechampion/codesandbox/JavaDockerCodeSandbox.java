package com.yelanyanyu.codechampion.codesandbox;


import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.yelanyanyu.codechampion.codesandbox.util.ProcessUtils.runProcessAndGetMsg;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final String CONTAINER_CODE_PATH = "/app";
    private boolean useReusableContainer = false;  // 控制是否使用可重用容器

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        // 先检查类文件是否存在，如果编译失败则不需要运行
        File classFile = new File(userCodeFile.getParent(), "Main.class");
        if (!classFile.exists()) {
            ExecuteMessage errorMessage = new ExecuteMessage();
            errorMessage.setErrorMessage("编译失败，无法运行代码");
            ArrayList<ExecuteMessage> result = new ArrayList<>();
            result.add(errorMessage);
            return result;
        }

        String userCodeParentPathDir = userCodeFile.getParentFile().getAbsolutePath();

        if (useReusableContainer) {
            // 使用可重用容器执行代码
            return runWithReusableContainer(userCodeParentPathDir, inputList);
        } else {
            // 使用原有方式执行代码
            return runWithNewContainer(userCodeParentPathDir, inputList);
        }
    }

    /**
     * 使用可重用容器执行代码
     */
    private List<ExecuteMessage> runWithReusableContainer(String userCodeParentPathDir, List<String> inputList) {
//        // 获取或创建专用于执行Java代码的容器
//        String containerId = dockerManager.getOrCreateJavaExecutionContainer();
//        if (containerId == null) {
//            // 如果无法创建可重用容器，回退到传统方式
//            return runWithNewContainer(userCodeParentPathDir, inputList);
//        }
//
//        // 将用户代码目录绑定到容器
//        boolean bindSuccess = dockerManager.bindDirectoryToContainer(
//                containerId, userCodeParentPathDir, CONTAINER_CODE_PATH);
//
//        if (!bindSuccess) {
//            // 绑定失败，使用传统方法
//            return runWithNewContainer(userCodeParentPathDir, inputList);
//        }
//
//        // 在容器中执行命令并获取结果
//        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
//        for (String inputArgs : inputList) {
//            String[] inputArgsArray = inputArgs.split(" ");
//            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", CONTAINER_CODE_PATH, "Main"}, inputArgsArray);
//            ExecuteMessage executeMessage = dockerManager.executeCommand(cmdArray, containerId);
//            executeMessageList.add(executeMessage);
//        }
//
//        return executeMessageList;
        return null;
    }

    /**
     * 使用传统方式创建新容器来运行代码（原始代码）
     */
    private List<ExecuteMessage> runWithNewContainer(String userCodeParentPathDir, List<String> inputList) {
        // 3：创建容器
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withAutoRemove(true);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp="));
        hostConfig.setBinds(new Bind(userCodeParentPathDir, new Volume(CONTAINER_CODE_PATH)));
        String image = dockerManager.getJdkImageName();
        CreateContainerResponse createContainerResponse = dockerManager.createContainer(image, hostConfig);
        String containerId = createContainerResponse.getId();

        // 4. 启动容器
        dockerManager.startContainer(containerId);
        // 5. 执行命令并获取结果
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
//            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main", "1", "3"}, inputArgsArray);
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", CONTAINER_CODE_PATH, "Main"}, inputArgsArray);
            ExecuteMessage executeMessage = dockerManager.executeCommand(cmdArray, containerId);
            executeMessageList.add(executeMessage);
        }
        dockerManager.removeContainer(containerId);
        return executeMessageList;
    }

    /**
     * 设置是否使用可重用容器
     *
     * @param useReusableContainer true表示使用可重用容器，false表示每次创建新容器
     */
    public void setUseReusableContainer(boolean useReusableContainer) {
        this.useReusableContainer = useReusableContainer;
    }
}

