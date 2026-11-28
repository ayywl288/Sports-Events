package com.sports.modules.fitness.scoring.model;

import java.math.BigDecimal;

/**
 * 一条原始成绩：某个体测项目的实测值。
 */
public class RawScore {

    private String itemCode;
    private BigDecimal value;

    public RawScore() {
    }

    public RawScore(String itemCode, BigDecimal value) {
        this.itemCode = itemCode;
        this.value = value;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
