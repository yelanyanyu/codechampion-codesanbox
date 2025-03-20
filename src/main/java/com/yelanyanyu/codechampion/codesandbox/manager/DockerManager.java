package com.yelanyanyu.codechampion.codesandbox.manager;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Service
@Slf4j
@ConfigurationProperties(prefix = "codesandbox.docker")
@Data
public class DockerManager {
    private List<String> defaultImages;
}
