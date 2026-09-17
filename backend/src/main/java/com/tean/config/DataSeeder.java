package com.tean.config;

import com.tean.dashboard.DashboardService;
import com.tean.device.*;
import com.tean.hazard.Hazard;
import com.tean.hazard.HazardLevel;
import com.tean.hazard.HazardRepository;
import com.tean.hazard.HazardStatus;
import com.tean.maintenance.MaintenanceRecord;
import com.tean.maintenance.MaintenanceRecordRepository;
import com.tean.orgunit.OrgUnit;
import com.tean.orgunit.OrgUnitRepository;
import com.tean.user.Role;
import com.tean.user.SysUser;
import com.tean.user.SysUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预置数据：3 家使用单位、4 类账号、12 台覆盖全部状态的电梯/起重机械、隐患与维保记录。
 * 日期按当前日期相对计算，保证「检验临期 / 逾期 / 红点」开箱即可演示。
 * 仅在空库时执行；重置数据：docker compose down -v 后重新启动。
 */
@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "Tean@2026";

    private final OrgUnitRepository orgUnitRepository;
    private final SysUserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStatusLogRepository statusLogRepository;
    private final HazardRepository hazardRepository;
    private final MaintenanceRecordRepository maintenanceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(OrgUnitRepository orgUnitRepository, SysUserRepository userRepository,
                      DeviceRepository deviceRepository, DeviceStatusLogRepository statusLogRepository,
                      HazardRepository hazardRepository, MaintenanceRecordRepository maintenanceRepository,
                      PasswordEncoder passwordEncoder) {
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.statusLogRepository = statusLogRepository;
        this.hazardRepository = hazardRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (orgUnitRepository.count() > 0) {
            return;
        }
        LocalDate n = LocalDate.now();

        // ---------- 使用单位 ----------
        OrgUnit agency = org(DashboardService.AGENCY_CODE, "市特种设备检验研究院", "全市");
        OrgUnit org1 = org("ORG001", "蓝天物业服务有限公司", "城东区");
        OrgUnit org2 = org("ORG002", "云帆商业运营管理有限公司", "城西区");
        OrgUnit org3 = org("ORG003", "惠民安居社区服务有限公司", "城南区");

        // ---------- 账号（密码统一 Tean@2026） ----------
        String pwd = passwordEncoder.encode(DEFAULT_PASSWORD);
        user("eq_admin", pwd, "王建国", Role.DEVICE_ADMIN, org1);
        user("eq_admin2", pwd, "李慧", Role.DEVICE_ADMIN, org2);
        user("eq_admin3", pwd, "张伟", Role.DEVICE_ADMIN, org3);
        user("maint01", pwd, "赵铁柱", Role.MAINTAINER, org1);
        user("maint02", pwd, "钱进", Role.MAINTAINER, org2);
        user("inspector01", pwd, "陈明", Role.INSPECTOR, agency);
        user("superv01", pwd, "周正", Role.SUPERVISOR, agency);

        // ---------- 设备：覆盖 注册告知/验收/在用/停用/报废 ----------
        Device d1 = dev("EQ-2023-0001", "蓝天大厦A座客梯", DeviceType.ELEVATOR, "西奥 XO-CONB",
                org1, DeviceStatus.IN_USE, "城东区", "A-101", "城东区朝阳路88号蓝天大厦A座3单元",
                n.minusDays(400), n.plusDays(8));            // 临期
        Device d2 = dev("EQ-2023-0002", "蓝天大厦B座货梯", DeviceType.ELEVATOR, "通力 MonoSpace",
                org1, DeviceStatus.IN_USE, "城东区", "A-102", "城东区朝阳路88号蓝天大厦B座1单元",
                n.minusDays(380), n.minusDays(18));          // 逾期
        Device d3 = dev("EQ-2022-0007", "蓝天广场客梯", DeviceType.ELEVATOR, "日立 MCA",
                org1, DeviceStatus.SUSPENDED, "城东区", "A-103", "城东区解放大道196号蓝天广场西翼",
                n.minusDays(700), n.plusDays(120));
        Device d4 = dev("CR-2023-0003", "仓储中心桥式起重机", DeviceType.CRANE, "QD 20t-22.5m",
                org1, DeviceStatus.IN_USE, "城东区", "A-201", "城东区物流大道7号蓝天仓储中心2号库房",
                n.minusDays(350), n.plusDays(21));           // 临期
        dev("EQ-2026-0011", "蓝天大厦C座新装客梯", DeviceType.ELEVATOR, "西奥 XO-CONB",
                org1, DeviceStatus.REGISTERED, "城东区", "A-104", "城东区朝阳路88号蓝天大厦C座（新装）",
                n.minusDays(10), null);                      // 注册告知

        Device d6 = dev("EQ-2023-0004", "云帆中心观光梯", DeviceType.ELEVATOR, "三菱 LEHY-III",
                org2, DeviceStatus.IN_USE, "城西区", "B-101", "城西区金融街1号云帆中心T1",
                n.minusDays(360), n.plusDays(150));
        Device d7 = dev("CR-2022-0009", "物流园门式起重机", DeviceType.CRANE, "MG 32t-30m",
                org2, DeviceStatus.IN_USE, "城西区", "B-201", "城西区港前路22号云帆物流园北区堆场",
                n.minusDays(720), n.minusDays(9));           // 逾期
        dev("EQ-2026-0012", "云帆Mall自动扶梯", DeviceType.ELEVATOR, "迅达 9300AE",
                org2, DeviceStatus.ACCEPTED, "城西区", "B-102", "城西区金融街3号云帆Mall L1-L2",
                n.minusDays(25), null);                      // 验收
        Device d9 = dev("EQ-2015-0002", "老仓库载货电梯", DeviceType.ELEVATOR, "华升富士达",
                org2, DeviceStatus.SCRAPPED, "城西区", "B-103", "城西区金融街5号老仓库",
                n.minusDays(3900), n.minusDays(200));        // 报废

        Device d10 = dev("EQ-2023-0008", "惠民小区1栋住宅梯", DeviceType.ELEVATOR, "康力 KLK1",
                org3, DeviceStatus.IN_USE, "城南区", "C-101", "城南区安居路16号惠民小区1栋",
                n.minusDays(330), n.plusDays(12));           // 临期
        Device d11 = dev("CR-2023-0010", "二期工地塔式起重机", DeviceType.CRANE, "QTZ80",
                org3, DeviceStatus.SUSPENDED, "城南区", "C-201", "城南区安居路16号惠民小区二期工地",
                n.minusDays(300), n.plusDays(60));
        Device d12 = dev("EQ-2022-0015", "惠民小区2栋住宅梯", DeviceType.ELEVATOR, "康力 KLK1",
                org3, DeviceStatus.IN_USE, "城南区", "C-102", "城南区安居路16号惠民小区2栋",
                n.minusDays(760), n.minusDays(59));          // 逾期

        // ---------- 状态流转留痕 ----------
        log(d1, "REGISTERED", "ACCEPTED", "陈明", "监督检验合格", n.minusDays(395));
        log(d1, "ACCEPTED", "IN_USE", "王建国", "投入使用", n.minusDays(390));
        log(d3, "IN_USE", "SUSPENDED", "王建国", "商场装修，暂停使用", n.minusDays(30));
        log(d9, "IN_USE", "SUSPENDED", "李慧", "故障频发，停用观察", n.minusDays(260));
        log(d9, "SUSPENDED", "SCRAPPED", "陈明", "达到报废年限，予以报废", n.minusDays(200));
        log(d11, "IN_USE", "SUSPENDED", "张伟", "工地停工，设备停用", n.minusDays(15));

        // ---------- 隐患 ----------
        hazard(d2, "层门门锁啮合深度不足", HazardLevel.HIGH, HazardStatus.OPEN, n.minusDays(7), null);
        hazard(d7, "起升钢丝绳断丝数超标", HazardLevel.HIGH, HazardStatus.OPEN, n.plusDays(3), null);
        hazard(d12, "轿厢平层精度超差", HazardLevel.MEDIUM, HazardStatus.OPEN, n.minusDays(30), null);
        hazard(d1, "轿厢应急照明失效", HazardLevel.LOW, HazardStatus.RESOLVED, n.minusDays(40), n.minusDays(35));

        // ---------- 维保记录 ----------
        maintenance(d1, "seed-0001-0001-4000-8000-000000000001", "季度例行维保：曳引机、制动器、限速器检查，运行正常",
                "赵铁柱", n.minusDays(20), false);
        maintenance(d2, "seed-0001-0001-4000-8000-000000000002", "半月维保：层门调整，门锁啮合问题已上报隐患",
                "赵铁柱", n.minusDays(6), false);
        maintenance(d7, "seed-0001-0001-4000-8000-000000000003", "月度维保：卷扬机构润滑，钢丝绳断丝超标已上报隐患",
                "钱进", n.minusDays(15), true);

        log.info("========== 预置数据完成 ==========");
        log.info("账号（密码均为 {}）：eq_admin/eq_admin2/eq_admin3(设备管理员) maint01/maint02(维保人员) inspector01(检验员) superv01(监察员)", DEFAULT_PASSWORD);
    }

    private OrgUnit org(String code, String name, String region) {
        OrgUnit o = new OrgUnit();
        o.setCode(code);
        o.setName(name);
        o.setRegion(region);
        return orgUnitRepository.save(o);
    }

    private void user(String username, String password, String realName, Role role, OrgUnit org) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(password);
        u.setRealName(realName);
        u.setRole(role);
        u.setOrgId(org.getId());
        u.setEnabled(true);
        userRepository.save(u);
    }

    private Device dev(String code, String name, DeviceType type, String model, OrgUnit org,
                       DeviceStatus status, String region, String maskCode, String exactAddress,
                       LocalDate installDate, LocalDate nextInspectionDate) {
        Device d = new Device();
        d.setDeviceCode(code);
        d.setName(name);
        d.setType(type);
        d.setModel(model);
        d.setOrgId(org.getId());
        d.setStatus(status);
        d.setRegion(region);
        d.setMaskCode(maskCode);
        d.setExactAddress(exactAddress);
        d.setInstallDate(installDate);
        d.setNextInspectionDate(nextInspectionDate);
        return deviceRepository.save(d);
    }

    private void log(Device device, String from, String to, String operator, String reason, LocalDate date) {
        DeviceStatusLog l = new DeviceStatusLog();
        l.setDeviceId(device.getId());
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setOperatorName(operator);
        l.setReason(reason);
        l.setCreatedAt(date.atTime(9, 30));
        statusLogRepository.save(l);
    }

    private void hazard(Device device, String title, HazardLevel level, HazardStatus status,
                        LocalDate deadline, LocalDate resolvedAt) {
        Hazard h = new Hazard();
        h.setDeviceId(device.getId());
        h.setTitle(title);
        h.setLevel(level);
        h.setStatus(status);
        h.setDeadline(deadline);
        h.setCreatedAt(deadline.minusDays(20).atTime(10, 0));
        h.setResolvedAt(resolvedAt == null ? null : resolvedAt.atTime(15, 0));
        hazardRepository.save(h);
    }

    private void maintenance(Device device, String recordNo, String content, String maintainer,
                             LocalDate date, boolean offline) {
        MaintenanceRecord r = new MaintenanceRecord();
        r.setRecordNo(recordNo);
        r.setDeviceId(device.getId());
        r.setMaintainerId(0L);
        r.setMaintainerName(maintainer);
        r.setContent(content);
        r.setOffline(offline);
        LocalDateTime time = date.atTime(14, 0);
        r.setClientCreatedAt(time);
        r.setCreatedAt(time);
        maintenanceRepository.save(r);
    }
}
