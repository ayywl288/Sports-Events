package com.sports.modules.fitness.scoring.service;

/**
 * 业务异常：记录已被判定为无效，不再参与评分。
 */
public class InvalidRecordException extends RuntimeException {

    public InvalidRecordException(String message) {
        super(message);
    }
}
