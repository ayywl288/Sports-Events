package com.sports.modules.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sports.common.exception.BusinessException;
import com.sports.modules.fitness.dto.FitnessScoreRequest;
import com.sports.modules.fitness.entity.FitnessScoreRecord;
import com.sports.modules.fitness.mapper.FitnessScoreMapper;
import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.ItemScoreResult;
import com.sports.modules.fitness.scoring.model.RawScore;
import com.sports.modules.fitness.scoring.model.RecordStatus;
import com.sports.modules.fitness.scoring.model.ScoreInput;
import com.sports.modules.fitness.scoring.model.ScoreReport;
import com.sports.modules.fitness.scoring.service.FitnessScoringService;
import com.sports.modules.fitness.scoring.service.InvalidRecordException;
import com.sports.modules.fitness.vo.FitnessScoreVO;
import com.sports.modules.fitness.vo.FitnessStudentVO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.mapper.SysUserMapper;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国标体测评分业务：
 * <ul>
 *   <li>按 2014 国标大学组评分表计算单项分、标准分、附加分、总分与等级；</li>
 *   <li>教师/管理员可判定成绩无效，无效记录不参与评分、学生端不可见；</li>
 *   <li>学生只能查看本人已审核有效的成绩。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class FitnessScoreService {

    private final FitnessScoreMapper scoreMapper;
    private final SysUserMapper userMapper;
    private final FitnessScoringService scoringService;

    /** 不落库试算。 */
    public FitnessScoreVO preview(FitnessScoreRequest request) {
        SysUser student = requireStudent(request.getUserId());
        Gender gender = resolveGender(request.getGender(), student);
        String academicYear = resolveAcademicYear(request.getAcademicYear(), request.getTestDate());
        ScoreReport report = calculate(request, gender);
        FitnessScoreVO vo = buildVO(null, student, request, gender, academicYear, report);
        vo.setStatus(RecordStatus.VALID.name());
        vo.setStatusLabel(RecordStatus.VALID.getLabel());
        return vo;
    }

    /** 新建评分记录。 */
    public Long create(FitnessScoreRequest request) {
        SysUser student = requireStudent(request.getUserId());
        Gender gender = resolveGender(request.getGender(), student);
        String academicYear = resolveAcademicYear(request.getAcademicYear(), request.getTestDate());
        ScoreReport report = calculate(request, gender);

        FitnessScoreRecord record = new FitnessScoreRecord();
        copyRequest(request, record);
        record.setGender(gender.name());
        record.setAcademicYear(academicYear);
        record.setStandardScore(report.getStandardScore());
        record.setBonusScore(report.getBonusScore());
        record.setTotalScore(report.getTotalScore());
        record.setGrade(report.getGrade());
        record.setStatus(RecordStatus.VALID.name());
        record.setAuditOperator(SecurityUtils.getUsername());
        record.setAuditTime(LocalDateTime.now());
        try {
            scoreMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "该学生当天已有国标评分记录，请编辑原记录");
        }
        return record.getId();
    }

    /** 修改评分记录并重新计分；已判定无效的记录禁止重算。 */
    public void update(Long id, FitnessScoreRequest request) {
        FitnessScoreRecord existing = requireRecord(id);
        if (RecordStatus.INVALID.name().equals(existing.getStatus())) {
            throw new BusinessException(409, "该体测记录已判定无效，不参与评分");
        }
        SysUser student = requireStudent(request.getUserId());
        Gender gender = resolveGender(request.getGender(), student);
        String academicYear = resolveAcademicYear(request.getAcademicYear(), request.getTestDate());
        ScoreReport report = calculate(request, gender);

        LambdaUpdateWrapper<FitnessScoreRecord> update = new LambdaUpdateWrapper<FitnessScoreRecord>()
                .eq(FitnessScoreRecord::getId, id)
                .set(FitnessScoreRecord::getUserId, request.getUserId())
                .set(FitnessScoreRecord::getTestDate, request.getTestDate())
                .set(FitnessScoreRecord::getHeightCm, request.getHeightCm())
                .set(FitnessScoreRecord::getWeightKg, request.getWeightKg())
                .set(FitnessScoreRecord::getVitalCapacity, request.getVitalCapacity())
                .set(FitnessScoreRecord::getRun50Seconds, request.getRun50Seconds())
                .set(FitnessScoreRecord::getSitReachCm, request.getSitReachCm())
                .set(FitnessScoreRecord::getJumpCm, request.getJumpCm())
                .set(FitnessScoreRecord::getStrengthCount, request.getStrengthCount())
                .set(FitnessScoreRecord::getEnduranceSeconds, request.getEnduranceSeconds())
                .set(FitnessScoreRecord::getGender, gender.name())
                .set(FitnessScoreRecord::getGroupType, request.getGroupType().name())
                .set(FitnessScoreRecord::getAcademicYear, academicYear)
                .set(FitnessScoreRecord::getStandardScore, report.getStandardScore())
                .set(FitnessScoreRecord::getBonusScore, report.getBonusScore())
                .set(FitnessScoreRecord::getTotalScore, report.getTotalScore())
                .set(FitnessScoreRecord::getGrade, report.getGrade());
        try {
            scoreMapper.update(null, update);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "该学生当天已有国标评分记录");
        }
    }

    /** 教师/管理员分页查询。 */
    public Page<FitnessScoreVO> page(int current, int size, Long userId, LocalDate testDate) {
        Page<FitnessScoreRecord> page = scoreMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<FitnessScoreRecord>()
                        .eq(userId != null, FitnessScoreRecord::getUserId, userId)
                        .eq(testDate != null, FitnessScoreRecord::getTestDate, testDate)
                        .orderByDesc(FitnessScoreRecord::getTestDate)
                        .orderByDesc(FitnessScoreRecord::getId));
        Map<Long, SysUser> students = loadStudents(page.getRecords());
        List<FitnessScoreVO> records = page.getRecords().stream()
                .map(record -> toVO(record, students.get(record.getUserId())))
                .toList();
        Page<FitnessScoreVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(records);
        return result;
    }

    /** 学生本人成绩：只取有效记录，同一学年以最新一次测试为准。 */
    public List<FitnessScoreVO> myScores() {
        Long userId = SecurityUtils.getUserId();
        List<FitnessScoreRecord> records = scoreMapper.selectList(
                new LambdaQueryWrapper<FitnessScoreRecord>()
                        .eq(FitnessScoreRecord::getUserId, userId)
                        .eq(FitnessScoreRecord::getStatus, RecordStatus.VALID.name())
                        .orderByDesc(FitnessScoreRecord::getTestDate)
                        .orderByDesc(FitnessScoreRecord::getId));
        SysUser student = userMapper.selectById(userId);
        Map<String, FitnessScoreRecord> latestByYear = new LinkedHashMap<>();
        for (FitnessScoreRecord record : records) {
            latestByYear.putIfAbsent(record.getAcademicYear(), record);
        }
        return latestByYear.values().stream().map(record -> toVO(record, student)).toList();
    }

    /** 详情：教师/管理员可看全部，学生只能看本人。 */
    public FitnessScoreVO detail(Long id) {
        FitnessScoreRecord record = requireRecord(id);
        if (SecurityUtils.isStudent() && !record.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(403, "只能查看本人的体测成绩");
        }
        return toVO(record, userMapper.selectById(record.getUserId()));
    }

    /** 审核状态变更。 */
    public FitnessScoreVO updateStatus(Long id, RecordStatus status, String operator) {
        FitnessScoreRecord record = requireRecord(id);
        String auditOperator = operator == null || operator.isBlank()
                ? SecurityUtils.getUsername() : operator.trim();
        LocalDateTime auditTime = LocalDateTime.now();
        scoreMapper.update(null, new LambdaUpdateWrapper<FitnessScoreRecord>()
                .eq(FitnessScoreRecord::getId, id)
                .set(FitnessScoreRecord::getStatus, status.name())
                .set(FitnessScoreRecord::getAuditOperator, auditOperator)
                .set(FitnessScoreRecord::getAuditTime, auditTime));
        record.setStatus(status.name());
        record.setAuditOperator(auditOperator);
        record.setAuditTime(auditTime);
        return toVO(record, userMapper.selectById(record.getUserId()));
    }

    /** 录入页学生下拉。 */
    public List<FitnessStudentVO> studentOptions(String q) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getRealName, SysUser::getStudentNo,
                        SysUser::getGender, SysUser::getClassName)
                .eq(SysUser::getRole, "STUDENT");
        if (q != null && !q.isBlank()) {
            String keyword = q.strip();
            query.and(w -> w.like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getStudentNo, keyword));
        }
        query.orderByAsc(SysUser::getStudentNo, SysUser::getId).last("LIMIT 30");
        return userMapper.selectList(query).stream()
                .map(user -> new FitnessStudentVO(user.getId(), user.getRealName(),
                        user.getStudentNo(), user.getGender(), user.getClassName()))
                .toList();
    }

    private ScoreReport calculate(FitnessScoreRequest request, Gender gender) {
        ScoreInput input = buildInput(request, gender);
        try {
            // 新建/修改都只针对有效记录，状态守卫保留在引擎入口。
            return scoringService.calculate(input, RecordStatus.VALID);
        } catch (InvalidRecordException e) {
            throw new BusinessException(409, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, e.getMessage());
        }
    }

    private ScoreInput buildInput(FitnessScoreRequest request, Gender gender) {
        List<RawScore> rawScores = new ArrayList<>();
        rawScores.add(new RawScore("BMI", bmi(request.getHeightCm(), request.getWeightKg())));
        add(rawScores, "VITAL_CAPACITY", request.getVitalCapacity());
        add(rawScores, "RUN_50M", request.getRun50Seconds());
        add(rawScores, "SIT_REACH", request.getSitReachCm());
        add(rawScores, "LONG_JUMP", request.getJumpCm());
        add(rawScores, gender == Gender.FEMALE ? "SIT_UP" : "PULL_UP",
                request.getStrengthCount() == null ? null : request.getStrengthCount().doubleValue());
        add(rawScores, gender == Gender.FEMALE ? "RUN_800M" : "RUN_1000M", request.getEnduranceSeconds());
        return new ScoreInput(gender, request.getGroupType(), rawScores);
    }

    private static void add(List<RawScore> list, String code, Double value) {
        if (value != null) {
            list.add(new RawScore(code, BigDecimal.valueOf(value)));
        }
    }

    private static BigDecimal bmi(Double heightCm, Double weightKg) {
        double heightM = heightCm / 100d;
        return BigDecimal.valueOf(weightKg / (heightM * heightM))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private FitnessScoreVO buildVO(Long id, SysUser student, FitnessScoreRequest request,
                                   Gender gender, String academicYear, ScoreReport report) {
        FitnessScoreVO vo = new FitnessScoreVO();
        vo.setId(id);
        vo.setUserId(request.getUserId());
        fillStudent(vo, student);
        vo.setGender(gender.name());
        vo.setGenderLabel(gender == Gender.FEMALE ? "女" : "男");
        vo.setGroupType(request.getGroupType().name());
        vo.setGroupTypeLabel(request.getGroupType().getLabel());
        vo.setAcademicYear(academicYear);
        vo.setTestDate(request.getTestDate());
        vo.setHeightCm(request.getHeightCm());
        vo.setWeightKg(request.getWeightKg());
        vo.setBmi(bmi(request.getHeightCm(), request.getWeightKg()).doubleValue());
        vo.setVitalCapacity(request.getVitalCapacity());
        vo.setRun50Seconds(request.getRun50Seconds());
        vo.setSitReachCm(request.getSitReachCm());
        vo.setJumpCm(request.getJumpCm());
        vo.setStrengthCount(request.getStrengthCount());
        vo.setEnduranceSeconds(request.getEnduranceSeconds());
        vo.setStandardScore(report.getStandardScore());
        vo.setBonusScore(report.getBonusScore());
        vo.setTotalScore(report.getTotalScore());
        vo.setGrade(report.getGrade());
        vo.setItems(report.getItems());
        vo.setMissingItemCodes(report.getMissingItemCodes());
        return vo;
    }

    private FitnessScoreVO toVO(FitnessScoreRecord record, SysUser student) {
        FitnessScoreVO vo = new FitnessScoreVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        fillStudent(vo, student);
        vo.setGender(record.getGender());
        vo.setGenderLabel(Gender.FEMALE.name().equals(record.getGender()) ? "女" : "男");
        vo.setGroupType(record.getGroupType());
        try {
            vo.setGroupTypeLabel(GroupType.valueOf(record.getGroupType()).getLabel());
        } catch (Exception ignored) {
            vo.setGroupTypeLabel(record.getGroupType());
        }
        vo.setAcademicYear(record.getAcademicYear());
        vo.setTestDate(record.getTestDate());
        vo.setHeightCm(record.getHeightCm());
        vo.setWeightKg(record.getWeightKg());
        vo.setBmi(record.getHeightCm() == null || record.getWeightKg() == null
                ? null : bmi(record.getHeightCm(), record.getWeightKg()).doubleValue());
        vo.setVitalCapacity(record.getVitalCapacity());
        vo.setRun50Seconds(record.getRun50Seconds());
        vo.setSitReachCm(record.getSitReachCm());
        vo.setJumpCm(record.getJumpCm());
        vo.setStrengthCount(record.getStrengthCount());
        vo.setEnduranceSeconds(record.getEnduranceSeconds());
        vo.setStandardScore(record.getStandardScore());
        vo.setBonusScore(record.getBonusScore());
        vo.setTotalScore(record.getTotalScore());
        vo.setGrade(record.getGrade());
        vo.setStatus(record.getStatus());
        try {
            vo.setStatusLabel(RecordStatus.valueOf(record.getStatus()).getLabel());
        } catch (Exception ignored) {
            vo.setStatusLabel(record.getStatus());
        }
        vo.setAuditOperator(record.getAuditOperator());
        vo.setAuditTime(record.getAuditTime());

        // 按存档原始成绩回代评分引擎，保证页面单项明细与评分表一致。
        if (record.getGender() != null && record.getGroupType() != null
                && record.getHeightCm() != null && record.getWeightKg() != null) {
            FitnessScoreRequest request = new FitnessScoreRequest();
            request.setUserId(record.getUserId());
            request.setTestDate(record.getTestDate());
            request.setHeightCm(record.getHeightCm());
            request.setWeightKg(record.getWeightKg());
            request.setVitalCapacity(record.getVitalCapacity());
            request.setRun50Seconds(record.getRun50Seconds());
            request.setSitReachCm(record.getSitReachCm());
            request.setJumpCm(record.getJumpCm());
            request.setStrengthCount(record.getStrengthCount());
            request.setEnduranceSeconds(record.getEnduranceSeconds());
            request.setGender(Gender.valueOf(record.getGender()));
            request.setGroupType(GroupType.valueOf(record.getGroupType()));
            try {
                ScoreReport report = scoringService.calculate(buildInput(request, request.getGender()));
                vo.setItems(report.getItems());
                vo.setMissingItemCodes(report.getMissingItemCodes());
            } catch (RuntimeException ignored) {
                vo.setItems(List.of());
                vo.setMissingItemCodes(List.of());
            }
        }
        return vo;
    }

    private static void fillStudent(FitnessScoreVO vo, SysUser student) {
        if (student != null) {
            vo.setStudentName(student.getRealName());
            vo.setStudentNo(student.getStudentNo());
        }
    }

    private Map<Long, SysUser> loadStudents(List<FitnessScoreRecord> records) {
        if (records.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = records.stream().map(FitnessScoreRecord::getUserId).distinct().toList();
        Map<Long, SysUser> students = new LinkedHashMap<>();
        userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getRealName, SysUser::getStudentNo)
                        .in(SysUser::getId, ids))
                .forEach(user -> students.put(user.getId(), user));
        return students;
    }

    private FitnessScoreRecord requireRecord(Long id) {
        FitnessScoreRecord record = scoreMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "体测评分记录不存在");
        }
        return record;
    }

    private SysUser requireStudent(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"STUDENT".equals(user.getRole())) {
            throw new BusinessException(400, "请选择有效学生");
        }
        return user;
    }

    private Gender resolveGender(Gender requested, SysUser student) {
        if (requested != null) {
            return requested;
        }
        if (student.getGender() != null && student.getGender() == 1) {
            return Gender.MALE;
        }
        if (student.getGender() != null && student.getGender() == 2) {
            return Gender.FEMALE;
        }
        throw new BusinessException(400, "该学生未登记性别，请选择性别");
    }

    private String resolveAcademicYear(String requested, LocalDate testDate) {
        if (requested != null && !requested.isBlank()) {
            String value = requested.strip();
            if (!value.endsWith("学年")) {
                value += "学年";
            }
            if (!value.matches("\\d{4}-\\d{4}学年")) {
                throw new BusinessException(400, "学年格式应为 2026-2027学年");
            }
            return value;
        }
        int start = testDate.getMonthValue() >= 9 ? testDate.getYear() : testDate.getYear() - 1;
        return start + "-" + (start + 1) + "学年";
    }

    private static void copyRequest(FitnessScoreRequest request, FitnessScoreRecord record) {
        record.setUserId(request.getUserId());
        record.setTestDate(request.getTestDate());
        record.setHeightCm(request.getHeightCm());
        record.setWeightKg(request.getWeightKg());
        record.setVitalCapacity(request.getVitalCapacity());
        record.setRun50Seconds(request.getRun50Seconds());
        record.setSitReachCm(request.getSitReachCm());
        record.setJumpCm(request.getJumpCm());
        record.setStrengthCount(request.getStrengthCount());
        record.setEnduranceSeconds(request.getEnduranceSeconds());
        record.setGroupType(request.getGroupType().name());
    }
}
