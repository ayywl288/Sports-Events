package com.sports.modules.registration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.vo.RegistrationVO;

import java.util.List;
import java.util.Map;

/**
 * 报名 Service
 * 状态说明：1-待审核 2-已通过 3-已拒绝 4-已取消
 *
 * @author sports
 */
public interface RegistrationService extends IService<Registration> {

    /**
     * 学生报名
     */
    Long register(RegistrationDTO dto);

    /**
     * 取消报名
     *
     * @param id     报名ID
     * @param reason 取消原因（管理员取消时必填）
     */
    void cancelRegistration(Long id, String reason);

    /**
     * 审核报名
     *
     * @param id     报名ID
     * @param status 审核结果（2-通过 3-拒绝）
     * @param remark 审核意见
     */
    void reviewRegistration(Long id, Integer status, String remark);

    /**
     * 批量审核报名
     *
     * @return 成功数量与失败明细
     */
    Map<String, Object> batchReview(List<Long> ids, Integer status, String remark);

    /**
     * 分页查询报名列表
     */
    IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId, Long sportItemId, Integer status);

    /**
     * 分页查询我的报名
     */
    IPage<RegistrationVO> pageMyRegistrations(Integer current, Integer size, Long eventId, Integer status);

    /**
     * 查询某项目已通过审核的参赛人员（供成绩录入选择）
     */
    List<RegistrationVO> listParticipants(Long eventId, Long sportItemId);

    /**
     * 检查是否已报名（不含已取消/已拒绝）
     */
    boolean hasRegistered(Long eventId, Long sportItemId, Long userId);
}
