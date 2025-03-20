package com.yelanyanyu.codechampion.codesandbox.manager;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Service
@Slf4j
@ConfigurationProperties(prefix = "codesandbox.docker")
@Data
public class DockerManager implements ApplicationRunner {
    private boolean autoPullImages;
    private List<String> defaultImages;
    private boolean jdkImageAvailable = false;
    @Getter
    private DockerClient dockerClient;

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initDockerClient();
        checkJdkImageAvailability();

        // Add shutdown hook for crash scenarios
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            log.info("Application terminating - running shutdown hook");
//            cleanupDockerResources();
//        }));
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
                    log.info("Pulling Docker image: {}", imageName);
                    dockerClient.pullImageCmd(imageName)
                            .exec(new PullImageResultCallback())
                            .awaitCompletion();
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

    public CreateContainerResponse createContainer(String imageName, HostConfig hostConfig) {
        if (this.dockerClient == null) {
            log.error("Docker client not initialized");
            return null;
        }
        CreateContainerCmd containerCmd = this.dockerClient.createContainerCmd(imageName);
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
//                .withHostConfig(hostConfig.withAutoRemove(true))
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();
        log.info("Docker container created: {}", createContainerResponse);
        return createContainerResponse;
    }

    public void startContainer(String containerId) {
        try {
            this.dockerClient.startContainerCmd(containerId).exec();
        } catch (NotFoundException | NotModifiedException e) {
            log.error("Failed to start container: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
                log.info("Docker client is closed");
            } catch (IOException e) {
                log.error("Error closing Docker client: {}", e.getMessage());
            }
        }
    }
}
