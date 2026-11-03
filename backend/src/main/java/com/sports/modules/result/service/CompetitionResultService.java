package com.sports.modules.result.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.result.dto.ResultDTO;
import com.sports.modules.result.dto.ResultExcelRow;
import com.sports.modules.result.dto.ResultUpdateDTO;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.vo.CompetitionResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 比赛成绩 Service
 *
 * @author sports
 */
public interface CompetitionResultService extends IService<CompetitionResult> {

    /**
     * 分页查询成绩（返回VO，含数据权限控制）
     */
    IPage<CompetitionResultVO> pageResultsVO(Integer current, Integer size, Long eventId, Long sportItemId, Long userId);

    /**
     * 根据赛事和项目获取成绩排名
     */
    List<CompetitionResultVO> getRankingByEventAndItem(Long eventId, Long sportItemId);

    /**
     * 分页查询我的成绩
     */
    IPage<CompetitionResultVO> pageMyResults(Integer current, Integer size, Long eventId);

    /**
     * 我的成绩统计（参赛次数、金银铜牌）
     */
    Map<String, Object> getMyStatistics();

    /**
     * 录入成绩
     */
    Long saveResult(ResultDTO dto);

    /**
     * 修改成绩（只允许修改成绩相关字段）
     */
    void updateResult(Long id, ResultUpdateDTO dto);

    /**
     * 删除成绩
     */
    void deleteResult(Long id);

    /**
     * 批量录入成绩
     */
    Map<String, Object> batchSaveResults(List<ResultDTO> list);

    /**
     * 导出成绩数据
     */
    List<ResultExcelRow> listForExport(Long eventId, Long sportItemId);

    /**
     * 导入成绩
     */
    Map<String, Object> importResults(MultipartFile file, Long eventId);

    /**
     * 重新计算某项目名次与积分
     */
    void recalculateRanking(Long eventId, Long sportItemId);
}
