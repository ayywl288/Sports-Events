package com.sports.modules.result.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩导入/导出 Excel 行模型
 *
 * @author sports
 */
@Data
@ColumnWidth(18)
public class ResultExcelRow {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("运动项目")
    private String sportItemName;

    @ExcelProperty("成绩")
    private BigDecimal score;

    @ExcelProperty("成绩文本")
    private String scoreText;

    @ExcelProperty("名次")
    private Integer ranking;

    @ExcelProperty("积分")
    private Integer points;

    @ExcelProperty("备注")
    private String remark;
}
