package com.yelanyanyu.codechampion.codesanbox.model;

import lombok.Data;

/**
 * Information about the judging process for a user's submitted question from sandbox, including
 * details such as execution failure reasons, execution time, memory consumption, etc.
 * <p>
 * Example JSON format:
 * {
 * "message": "程序执行信息", // Program execution information
 * "time": 1000, // Execution time in ms
 * "memory": 1000, // Memory usage in kb
 * }
 *
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@Data
public class JudgeInfo {
    /**
     * Information about program execution, which is defined in class 'JudgeInfoMessageEnum' in backend.
     */
    private String message;

    /**
     * The actual execution time of the solution in milliseconds.
     */
    private Long time;

    /**
     * The actual memory consumption of the solution in kilobytes.
     */
    private Long memory;
}
