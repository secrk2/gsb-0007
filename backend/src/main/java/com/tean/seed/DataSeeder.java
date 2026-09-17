package com.tean.seed;

import com.tean.auth.Role;
import com.tean.device.*;
import com.tean.hazard.HazardEntity;
import com.tean.hazard.HazardRepository;
import com.tean.inspection.InspectionRecord;
import com.tean.inspection.InspectionRecordRepository;
import com.tean.maintenance.MaintenanceRecord;
import com.tean.maintenance.MaintenanceRecordRepository;
import com.tean.org.OrgEntity;
import com.tean.org.OrgRepository;
import com.tean.org.OrgType;
import com.tean.user.UserEntity;
import com.tean.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预置数据：特检机构、3 家使用单位、1 家维保单位；
 * 设备管理员/维保人员/检验员/监察员四类账号；
 * 覆盖注册告知/验收/在用/停用/报废全状态的电梯与起重机械。
 * 仅空库时执行，幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final OrgRepository orgRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusHistoryRepository historyRepository;
    private final HazardRepository hazardRepository;
    private final InspectionRecordRepository inspectionRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("空库，写入预置数据...");

        // ---------- 机构 ----------
        OrgEntity agency = org("市特种设备检验研究院", OrgType.AGENCY, "市级");
        OrgEntity wuye = org("云栖物业管理有限公司", OrgType.USER, "城东片区");
        OrgEntity yiyuan = org("仁济医院", OrgType.USER, "城西片区");
        OrgEntity mall = org("万象商业广场", OrgType.USER, "高新片区");
        OrgEntity maint = org("迅捷特种设备维保有限公司", OrgType.MAINTAINER, "全市");

        // ---------- 账号（密码均为 123456） ----------
        String hash = passwordEncoder.encode("123456");
        user("wuye_admin", hash, "王建国", Role.DEVICE_ADMIN, wuye);
        user("yiyuan_admin", hash, "李秀兰", Role.DEVICE_ADMIN, yiyuan);
        user("mall_admin", hash, "张伟", Role.DEVICE_ADMIN, mall);
        user("weibao01", hash, "赵铁柱", Role.MAINTAINER, maint);
        user("weibao02", hash, "孙丽", Role.MAINTAINER, maint);
        user("jianyan01", hash, "陈明", Role.INSPECTOR, agency);
        user("jiancha01", hash, "周正", Role.SUPERVISOR, agency);

        // ---------- 设备 ----------
        LocalDate today = LocalDate.now();
        DeviceEntity d1 = device("DT-2024-0001", "1号楼客梯", DeviceType.ELEVATOR, DeviceStatus.IN_USE,
                wuye, maint, "城东片区", "A-101", "云栖雅苑1号楼", "城东区云栖路88号云栖雅苑1号楼1单元",
                today.minusMonths(11), today.plusDays(12));
        DeviceEntity d2 = device("DT-2024-0002", "2号楼客梯", DeviceType.ELEVATOR, DeviceStatus.IN_USE,
                wuye, maint, "城东片区", "A-102", "云栖雅苑2号楼", "城东区云栖路88号云栖雅苑2号楼2单元",
                today.minusMonths(13), today.minusDays(5));   // 检验逾期
        DeviceEntity d3 = device("QZ-2023-0007", "地库汽车升降机", DeviceType.CRANE, DeviceStatus.IN_USE,
                wuye, maint, "城东片区", "A-201", "云栖雅苑地下车库", "城东区云栖路88号云栖雅苑地下车库B区",
                today.minusMonths(6), today.plusMonths(6));
        DeviceEntity d4 = device("DT-2025-0011", "住院部病床梯", DeviceType.ELEVATOR, DeviceStatus.IN_USE,
                yiyuan, maint, "城西片区", "B-301", "仁济医院住院部大楼", "城西区杏林路120号住院部大楼3号梯",
                today.minusMonths(10), today.plusDays(25));   // 临期
        DeviceEntity d5 = device("DT-2025-0012", "门诊楼扶梯", DeviceType.ELEVATOR, DeviceStatus.SUSPENDED,
                yiyuan, maint, "城西片区", "B-302", "仁济医院门诊楼", "城西区杏林路120号门诊楼一层东侧",
                today.minusMonths(14), today.plusMonths(2));
        DeviceEntity d6 = device("QZ-2022-0003", "供应室桥式起重机", DeviceType.CRANE, DeviceStatus.IN_USE,
                yiyuan, maint, "城西片区", "B-401", "仁济医院后勤保障楼", "城西区杏林路120号后勤保障楼一层",
                today.minusMonths(8), today.plusMonths(4));
        DeviceEntity d7 = device("DT-2024-0021", "商场中庭观光梯", DeviceType.ELEVATOR, DeviceStatus.IN_USE,
                mall, maint, "高新片区", "C-101", "万象商业广场中庭", "高新区科汇路6号万象商业广场中庭1号梯",
                today.minusMonths(12), today.minusDays(20));  // 检验逾期
        DeviceEntity d8 = device("QZ-2024-0009", "屋面检修起重机", DeviceType.CRANE, DeviceStatus.IN_USE,
                mall, maint, "高新片区", "C-501", "万象商业广场屋面", "高新区科汇路6号万象商业广场屋面层",
                today.minusMonths(5), today.plusDays(18));    // 临期
        DeviceEntity d9 = device("DT-2026-0031", "新装货梯", DeviceType.ELEVATOR, DeviceStatus.REGISTERED,
                mall, maint, "高新片区", "C-102", "万象商业广场北翼", "高新区科汇路6号万象商业广场北翼货梯井",
                null, null);
        DeviceEntity d10 = device("QZ-2026-0002", "新装门式起重机", DeviceType.CRANE, DeviceStatus.ACCEPTED,
                mall, maint, "高新片区", "C-502", "万象商业广场卸货区", "高新区科汇路6号万象商业广场卸货区",
                null, null);
        DeviceEntity d11 = device("DT-2019-0006", "老旧住宅梯（已报废）", DeviceType.ELEVATOR, DeviceStatus.SCRAPPED,
                wuye, maint, "城东片区", "A-103", "云栖雅苑3号楼", "城东区云栖路88号云栖雅苑3号楼",
                today.minusYears(3), today.minusYears(1));
        DeviceEntity d12 = device("QZ-2020-0015", "旧车间行车（已报废）", DeviceType.CRANE, DeviceStatus.SCRAPPED,
                yiyuan, maint, "城西片区", "B-402", "仁济医院旧设备楼", "城西区杏林路120号旧设备楼",
                today.minusYears(4), today.minusYears(2));

        // ---------- 状态流转留痕 ----------
        List<DeviceEntity> all = deviceRepository.findAll();
        for (DeviceEntity d : all) {
            DeviceStatus s = d.getStatus();
            if (s == DeviceStatus.REGISTERED) {
                continue;
            }
            DeviceStatus[] path = switch (s) {
                case ACCEPTED -> new DeviceStatus[]{DeviceStatus.REGISTERED, DeviceStatus.ACCEPTED};
                case IN_USE -> new DeviceStatus[]{DeviceStatus.REGISTERED, DeviceStatus.ACCEPTED, DeviceStatus.IN_USE};
                case SUSPENDED -> new DeviceStatus[]{DeviceStatus.REGISTERED, DeviceStatus.ACCEPTED, DeviceStatus.IN_USE, DeviceStatus.SUSPENDED};
                case SCRAPPED -> new DeviceStatus[]{DeviceStatus.REGISTERED, DeviceStatus.ACCEPTED, DeviceStatus.IN_USE, DeviceStatus.SCRAPPED};
                default -> new DeviceStatus[]{};
            };
            for (int i = 1; i < path.length; i++) {
                DeviceStatusHistory h = new DeviceStatusHistory();
                h.setDeviceId(d.getId());
                h.setFromStatus(path[i - 1]);
                h.setToStatus(path[i]);
                h.setReason("预置流转");
                h.setOperatorId(6L);
                h.setOperatorName("陈明");
                historyRepository.save(h);
            }
        }

        // ---------- 隐患 ----------
        hazard(d2, "轿厢紧急报警装置失效", HazardEntity.Level.MAJOR, today.minusDays(3));   // 逾期未整改
        hazard(d7, "层门门锁啮合深度不足", HazardEntity.Level.GENERAL, today.plusDays(10));
        hazard(d8, "钢丝绳断丝超标", HazardEntity.Level.MAJOR, today.plusDays(5));
        HazardEntity done = hazard(d5, "扶梯梳齿板异物卡阻", HazardEntity.Level.GENERAL, today.minusDays(10));
        done.setStatus(HazardEntity.Status.RECTIFIED);
        done.setRectifiedAt(LocalDateTime.now().minusDays(12));
        hazardRepository.save(done);

        // ---------- 检验记录 ----------
        inspection(d1, InspectionRecord.Result.PASS, today.minusMonths(11), today.plusDays(12), "JY-2025-1001");
        inspection(d4, InspectionRecord.Result.PASS, today.minusMonths(10), today.plusDays(25), "JY-2025-1088");
        inspection(d7, InspectionRecord.Result.RECTIFY, today.minusMonths(12), today.minusDays(20), "JY-2024-0777");

        // ---------- 维保记录 ----------
        maintenance(d1, "半月保：机房清洁、曳引机检查、层门门锁测试", today.minusDays(6));
        maintenance(d4, "季度保：安全钳联动试验、平层精度调整", today.minusDays(15));
        maintenance(d8, "月保：钢丝绳润滑、限位器动作试验", today.minusDays(9));

        log.info("预置数据完成：{} 台设备，{} 个账号", all.size(), userRepository.count());
    }

    private OrgEntity org(String name, OrgType type, String region) {
        OrgEntity o = new OrgEntity();
        o.setName(name);
        o.setType(type);
        o.setRegionCode(region);
        return orgRepository.save(o);
    }

    private void user(String username, String hash, String name, Role role, OrgEntity org) {
        UserEntity u = new UserEntity();
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setName(name);
        u.setRole(role);
        u.setOrgId(org.getId());
        userRepository.save(u);
    }

    private DeviceEntity device(String code, String name, DeviceType type, DeviceStatus status,
                                OrgEntity org, OrgEntity maint, String region, String locationCode,
                                String building, String address,
                                LocalDate lastInspection, LocalDate nextInspection) {
        DeviceEntity d = new DeviceEntity();
        d.setCode(code);
        d.setName(name);
        d.setType(type);
        d.setStatus(status);
        d.setOrgId(org.getId());
        d.setMaintOrgId(maint.getId());
        d.setRegionCode(region);
        d.setLocationCode(locationCode);
        d.setBuildingName(building);
        d.setExactAddress(address);
        d.setInstallDate(lastInspection == null ? LocalDate.now().minusMonths(2) : lastInspection.minusMonths(2));
        d.setLastInspectionDate(lastInspection);
        d.setNextInspectionDate(nextInspection);
        return deviceRepository.save(d);
    }

    private HazardEntity hazard(DeviceEntity d, String title, HazardEntity.Level level, LocalDate deadline) {
        HazardEntity h = new HazardEntity();
        h.setDeviceId(d.getId());
        h.setOrgId(d.getOrgId());
        h.setTitle(title);
        h.setLevel(level);
        h.setDeadline(deadline);
        return hazardRepository.save(h);
    }

    private void inspection(DeviceEntity d, InspectionRecord.Result result,
                            LocalDate inspectedAt, LocalDate next, String reportNo) {
        InspectionRecord r = new InspectionRecord();
        r.setDeviceId(d.getId());
        r.setOrgId(d.getOrgId());
        r.setInspectorId(6L);
        r.setInspectorName("陈明");
        r.setResult(result);
        r.setInspectedAt(inspectedAt);
        r.setNextInspectionDate(next);
        r.setReportNo(reportNo);
        inspectionRepository.save(r);
    }

    private void maintenance(DeviceEntity d, String content, LocalDate happened) {
        MaintenanceRecord r = new MaintenanceRecord();
        r.setClientId("seed-" + d.getCode() + "-" + happened);
        r.setDeviceId(d.getId());
        r.setOrgId(d.getOrgId());
        r.setMaintainerId(4L);
        r.setMaintainerName("赵铁柱");
        r.setContent(content);
        r.setHappenedAt(happened.atTime(10, 0));
        r.setOffline(false);
        maintenanceRepository.save(r);
    }
}
