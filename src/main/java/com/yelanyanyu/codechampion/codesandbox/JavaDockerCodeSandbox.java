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
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPathDir = userCodeFile.getParentFile().getAbsolutePath();
        // 3：创建容器
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withAutoRemove(true);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp="));
        hostConfig.setBinds(new Bind(userCodeParentPathDir, new Volume("/app")));
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
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecuteMessage executeMessage = dockerManager.executeCommand(cmdArray, containerId);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }
}
