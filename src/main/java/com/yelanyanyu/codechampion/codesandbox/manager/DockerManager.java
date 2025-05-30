package com.yelanyanyu.codechampion.codesandbox.manager;


import cn.hutool.core.date.StopWatch;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.yelanyanyu.codechampion.codesandbox.model.ExecuteMessage;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Service
@Slf4j
@ConfigurationProperties(prefix = "codesandbox.docker")
@Data
public class DockerManager implements ApplicationRunner, DisposableBean {
    private boolean autoPullImages;
    private List<String> defaultImages;
    private boolean jdkImageAvailable = false;
    @Getter
    private DockerClient dockerClient;
    @Value("${codesandbox.compile-config.timeout}")
    private Long timeOut;
    /**
     * 存储所有启动的 container
     */
    private List<String> containerIds = new ArrayList<>();
    /**
     * key: imageName
     * value: 是否存在于 docker 中
     */
    private Map<String, Boolean> imageToExist = new HashMap<>();

    private static DockerClient openDockerClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * 查询所有已经存在的 images，将其加入 imageToExist 中，防止重复拉取
     */
    private void checkAllExistImages() {
        if (dockerClient == null) {
            log.error("Docker client not initialized");
            return;
        }

        try {
            List<Image> images = dockerClient.listImagesCmd().exec();

            for (Image image : images) {
                if (image.getRepoTags() != null) {
                    for (String tag : image.getRepoTags()) {
                        // Skip <none>:<none> images
                        if (!"<none>:<none>".equals(tag)) {
                            imageToExist.put(tag, true);
                            log.info("Found existing Docker image: {}", tag);
                        }
                    }
                }
            }
            log.info("Found {} existing Docker images", imageToExist.size());
        } catch (Exception e) {
            log.error("Error checking for existing Docker images: {}", e.getMessage());
        }
    }

    public ExecuteMessage executeCommand(String[] cmdArray, String containerId) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        ExecCreateCmdResponse execCreateCmdResponse = this.dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .exec();
        log.info("创建执行命令：{}", execCreateCmdResponse);
        final String[] message = {null};
        final String[] errorMessage = {null};
        long time = 0L;
        // 判断是否超时，实际情况下只用调用 timeout[0]
        final boolean[] timeout = {true};
        String execId = execCreateCmdResponse.getId();
        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
            @Override
            public void onNext(Frame frame) {
                /*
                 从 docker container 的输出流中得到结构信息，
                 如果该信息隶属于错误信息，则执行失败，否则执行成功
                */
                StreamType streamType = frame.getStreamType();
                if (StreamType.STDERR.equals(streamType)) {
                    errorMessage[0] = new String(frame.getPayload());
                    System.out.println("输出错误结果：" + errorMessage[0]);
                } else {
                    message[0] = new String(frame.getPayload());
                    System.out.println("输出结果：" + message[0]);
                }
                super.onNext(frame);
            }

            @Override
            public void onComplete() {
                // 如果执行完成，则表示没有超时
                timeout[0] = false;
                super.onComplete();
            }
        };
        final long[] maxMemory = {0L};
        StatsCmd statsCmd = this.dockerClient.statsCmd(containerId);
        ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback.Adapter<Statistics>() {
            @Override
            public void onNext(Statistics statistics) {
                System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
            }

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        });
        statsCmd.exec(statisticsResultCallback);
        StopWatch stopWatch = new StopWatch();
        try {
            Thread.sleep(1000);
            stopWatch.start();
            this.dockerClient.execStartCmd(execId)
                    .exec(execStartResultCallback)
                    .awaitCompletion(this.timeOut, TimeUnit.MILLISECONDS);
            stopWatch.stop();
            time = stopWatch.getLastTaskTimeMillis();
            statsCmd.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executeMessage.setNormalMessage(message[0]);
        executeMessage.setErrorMessage(errorMessage[0]);
        executeMessage.setTime(time);
        executeMessage.setMemory(maxMemory[0]);

        return executeMessage;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initDockerClient();
        checkAllExistImages();
        checkJdkImageAvailability();

//         Add shutdown hook for crash scenarios
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Application terminating - running shutdown hook");
//            cleanupDockerResources();
            close();
        }));
        if (autoPullImages) {
            pullImages();
            return;
        }
        log.info("No images will be pulled");
    }

    private void cleanupDockerResources() {
        // todo
        if (dockerClient != null) {
            try {
                // Use shorter timeout for cleanup operations
                log.info("Cleaning up Docker resources...");
                cleanupRunningContainers();

                dockerClient.close();
                log.info("Docker client closed successfully");
            } catch (IOException e) {
                log.error("Error closing Docker client: {}", e.getMessage());
            }
        }
    }

    private void cleanupRunningContainers() {
        try {
            // Only clean containers created by this application
            dockerClient.listContainersCmd()
                    .withLabelFilter(Collections.singleton("created-by=codechampion-codesandbox"))
                    .exec()
                    .forEach(container -> {
                        try {
                            log.info("Stopping container: {}", container.getId());
                            dockerClient.stopContainerCmd(container.getId())
                                    .withTimeout(5) // short timeout
                                    .exec();
                        } catch (Exception e) {
                            log.warn("Failed to stop container {}: {}",
                                    container.getId(), e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error during container cleanup: {}", e.getMessage());
        }
    }

    public boolean isJDKImageAvailable() {
        return jdkImageAvailable;
    }

    private void initDockerClient() {
        try {
            dockerClient = openDockerClient();
            dockerClient.pingCmd().exec();
            log.info("Docker client is ready");
        } catch (Exception e) {
            log.error("Failed to connect to Docker: {}", e.getMessage());
        }
    }

    /**
     * 判断是否有 jdk image 存在
     */
    private void checkJdkImageAvailability() {
        if (dockerClient == null) {
            log.error("Docker client not initialized");
            return;
        }

        try {
            List<Image> images = dockerClient.listImagesCmd().exec();

            // Check for any JDK image from our default images list
            for (String defaultImage : defaultImages) {
                for (Image image : images) {
                    if (image.getRepoTags() != null) {
                        for (String tag : image.getRepoTags()) {
                            if (tag.equals(defaultImage)) {
                                jdkImageAvailable = true;
                                log.info("Docker image found: {}", defaultImage);
                                return;
                            }
                        }
                    }
                }
                log.warn("Docker image not found: {}", defaultImage);
            }
        } catch (Exception e) {
            log.error("Error checking for Docker images: {}", e.getMessage());
        }
    }

    public void pullImages() {
        if (dockerClient != null && defaultImages != null && !defaultImages.isEmpty()) {
            for (String imageName : defaultImages) {
                try {
                    if (imageToExist.containsKey(imageName) || imageToExist.get(imageName)) {
                        continue;
                    }
                    log.info("Pulling Docker image: {}", imageName);
                    dockerClient.pullImageCmd(imageName)
                            .exec(new PullImageResultCallback())
                            .awaitCompletion();
                    imageToExist.put(imageName, true);
                    log.info("Docker image successfully pulled: {}", imageName);

                    // Set JDK image flag if this is a JDK image (you may need to adjust this condition)
                    if (imageName.contains("jdk") || imageName.contains("openjdk")) {
                        jdkImageAvailable = true;
                    }
                } catch (Exception e) {
                    log.error("Failed to pull Docker image {}: {}", imageName, e.getMessage());
                }
            }
        }
    }

    public String getJdkImageName() {
        List<String> jdkImageNames = new ArrayList<>();
        for (String imageName : defaultImages) {
            if (imageName.contains("jdk")) {
                jdkImageNames.add(imageName);
            }
        }
        if (jdkImageNames.size() > 1) {
            log.warn("Multiple JDK images found: {}, the first defined jdk: {} will be Available"
                    , jdkImageNames, jdkImageNames.get(0));
        }
        return jdkImageNames.get(0);
    }

    public CreateContainerResponse createContainer(String imageName, HostConfig hostConfig) {
        if (this.dockerClient == null) {
            log.error("Docker client not initialized");
            return null;
        }
        CreateContainerCmd containerCmd = this.dockerClient.createContainerCmd(imageName);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withTty(true)
                .exec();
        log.info("Docker container created: {}", createContainerResponse);
        return createContainerResponse;
    }

    public void startContainer(String containerId) {
        try {
            this.dockerClient.startContainerCmd(containerId).exec();
            this.containerIds.add(containerId);
            log.info("Container started: {}", containerId);
        } catch (NotFoundException | NotModifiedException e) {
            log.error("Failed to start container: {}", e.getMessage());
        }
    }

    /**
     * 删除指定的Docker容器
     *
     * @param containerId 要删除的容器ID
     * @return 是否成功删除
     */
    public boolean removeContainer(String containerId) {
        if (dockerClient == null) {
            log.error("Docker client not initialized");
            return false;
        }

        try {
            // 先尝试停止容器（如果它正在运行）
            try {
                log.info("Stopping container: {}", containerId);
                dockerClient.stopContainerCmd(containerId)
                        .withTimeout(10) // 10秒超时
                        .exec();
            } catch (NotModifiedException e) {
                // 容器已经停止，可以忽略这个异常
                log.debug("Container {} already stopped", containerId);
            } catch (NotFoundException e) {
                log.warn("Container {} not found, cannot stop", containerId);
                return false;
            } catch (Exception e) {
                log.warn("Error stopping container {}: {}", containerId, e.getMessage());
                // 继续尝试删除容器
            }

            // 删除容器
            log.info("Removing container: {}", containerId);
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true) // 强制删除，即使正在运行
                    .withRemoveVolumes(true) // 同时删除关联的匿名卷
                    .exec();

            // 从容器ID列表中移除
            containerIds.remove(containerId);
            log.info("Container removed successfully: {}", containerId);

            return true;
        } catch (NotFoundException e) {
            log.warn("Container {} not found, cannot remove", containerId);
            // 容器不存在，从列表中移除ID
            containerIds.remove(containerId);
            return false;
        } catch (Exception e) {
            log.error("Failed to remove container {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    public void close() {
        if (dockerClient != null) {
            try {
                // 当springboot 实例关闭后，自动停止容器运行
                for (String containerId : this.containerIds) {
                    this.dockerClient.stopContainerCmd(containerId).exec();
                    log.info("Container stopped: {}", containerId);
                }
                this.dockerClient.close();
                log.info("Docker client is closed");
            } catch (IOException e) {
                log.error("Error closing Docker client: {}", e.getMessage());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
//        close();
    }
}

