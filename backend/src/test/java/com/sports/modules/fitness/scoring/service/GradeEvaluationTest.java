package com.sports.modules.fitness.scoring.service;

import com.sports.modules.fitness.scoring.data.InMemoryScoringDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradeEvaluationTest {

    private FitnessScoringService service;

    @BeforeEach
    void setUp() {
        service = new FitnessScoringService(new InMemoryScoringDataProvider());
    }

    @Test
    @DisplayName("等级评定的边界必须精确卡在 90 / 80 / 60")
    void gradeBoundaries() {
        assertEquals("优秀", service.judgeLevel(new BigDecimal("120.00")));
        assertEquals("优秀", service.judgeLevel(new BigDecimal("90.00")));
        assertEquals("良好", service.judgeLevel(new BigDecimal("89.99")));
        assertEquals("良好", service.judgeLevel(new BigDecimal("80.00")));
        assertEquals("及格", service.judgeLevel(new BigDecimal("79.99")));
        assertEquals("及格", service.judgeLevel(new BigDecimal("60.00")));
        assertEquals("不及格", service.judgeLevel(new BigDecimal("59.99")));
        assertEquals("不及格", service.judgeLevel(new BigDecimal("0")));
    }
}
