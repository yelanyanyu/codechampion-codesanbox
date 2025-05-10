package com.yelanyanyu.codechampion.codesandbox.model;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing the possible judge information messages for code submissions.
 * These messages provide detailed feedback about the outcome of code evaluation
 * in the Code Champion judging system.
 *
 * @author yelanyanyu
 * @version 1.0
 */
@Getter
public enum JudgeInfoMessageEnum {

    /**
     * Indicates that the submission has passed all test cases successfully
     */
    ACCEPT("成功", "Accepted"),

    /**
     * Indicates that the submission produced incorrect output for at least one test case
     */
    WRONG_ANSWER("答案错误", "Wrong Answer"),

    /**
     * Indicates that the code failed to compile
     */
    COMPILE_ERROR("编译错误", "Compile Error"),

    /**
     * Indicates that the submission exceeded the memory usage limits
     */
    MEMORY_LIMIT_EXCEEDED("内存溢出", "Memory Limit Exceeded"),

    /**
     * Indicates that the submission exceeded the time limits
     */
    TIME_LIMIT_EXCEEDED("超时", "Time Limit Exceeded"),

    /**
     * Indicates that the output format is incorrect while the answer might be correct
     */
    PRESENTATION_ERROR("展示错误", "Presentation Error"),

    /**
     * Indicates that the submission is in the queue and waiting to be processed
     */
    WAITING("等待中", "Waiting"),

    /**
     * Indicates that the submission produced too much output data
     */
    OUTPUT_LIMIT_EXCEEDED("输出溢出", "Output Limit Exceeded"),

    /**
     * Indicates that the submission attempted to perform a dangerous or prohibited operation
     */
    DANGEROUS_OPERATION("危险操作", "Dangerous Operation"),

    /**
     * Indicates that an error occurred during the execution of the code
     */
    RUNTIME_ERROR("运行错误", "Runtime Error"),

    /**
     * Indicates that an error occurred in the judging system itself
     */
    SYSTEM_ERROR("系统错误", "System Error");

    /**
     * The human-readable text description of the message (in Chinese)
     */
    private final String text;

    /**
     * The string value representing the message (in English), used for storage and comparison
     */
    private final String value;

    /**
     * Constructor for the enum constants.
     *
     * @param text  The human-readable text description of the message in Chinese
     * @param value The string value representing the message in English
     */
    JudgeInfoMessageEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * Returns a list of all the string values defined in this enum.
     * Useful for validation to check if a given string represents a valid judge message.
     *
     * @return A list containing all string values defined in the enum
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * Finds and returns the enum constant that matches the provided value.
     *
     * @param value The string value to search for
     * @return The matching enum constant, or null if no match is found or value is empty
     */
    public static JudgeInfoMessageEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (JudgeInfoMessageEnum anEnum : JudgeInfoMessageEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
