package com.yelanyanyu.codechampion.codesandbox;

import com.yelanyanyu.codechampion.codesandbox.manager.DockerManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@SpringBootTest
class JavaDockerCodeSandboxTest {
    @Resource
    private DockerManager dockerManager;

    @Test
    void getDefaultImages() {
        List<String> defaultImages = dockerManager.getDefaultImages();
        for (String defaultImage : defaultImages) {
            System.out.println(defaultImage);
        }
    }
}
