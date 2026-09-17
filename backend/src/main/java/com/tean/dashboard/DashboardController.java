package com.tean.dashboard;

import com.tean.auth.LoginUser;
import com.tean.auth.UserContext;
import com.tean.common.ApiResponse;
import com.tean.device.DeviceEntity;
import com.tean.device.DeviceService;
import com.tean.device.DeviceStatus;
import com.tean.hazard.HazardEntity;
import com.tean.hazard.HazardRepository;
import com.tean.org.OrgEntity;
import com.tean.org.OrgRepository;
import com.tean.org.OrgType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备作战台：各使用单位在用漏斗、检验临期、隐患与逾期红点。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DeviceService deviceService;
    private final OrgRepository orgRepository;
    private final HazardRepository hazardRepository;

    @Value("${tean.inspection-due-days:30}")
    private int dueDays;

    public record DeviceBrief(Long id, String code, String name, String typeLabel,
                              String maskedLocation, String nextInspectionDate) {
        static DeviceBrief of(DeviceEntity d) {
            return new DeviceBrief(d.getId(), d.getCode(), d.getName(), d.getType().getLabel(),
                    d.getRegionCode() + " · " + d.getLocationCode(),
                    d.getNextInspectionDate() == null ? null : d.getNextInspectionDate().toString());
        }
    }

    public record OrgBattle(Long orgId, String orgName,
                            Map<String, Long> funnel,
                            List<DeviceBrief> dueSoon,
                            List<DeviceBrief> overdueInspections,
                            long openHazards,
                            long overdueHazards) {}

    public record BattleView(LocalDateTime generatedAt, int dueDays, List<OrgBattle> orgs) {}

    @GetMapping("/battle")
    public ApiResponse<BattleView> battle() {
        LoginUser user = UserContext.require();
        LocalDate today = LocalDate.now();
        LocalDate dueLimit = today.plusDays(dueDays);

        List<DeviceEntity> devices = deviceService.listVisible(user);
        Map<Long, List<DeviceEntity>> byOrg = devices.stream()
                .collect(Collectors.groupingBy(DeviceEntity::getOrgId));

        Map<Long, String> orgNames = deviceService.orgNameMap();
        // 机构用户只看本单位；特检机构看全部使用单位
        List<Long> orgIds = user.getRole().isAgency()
                ? orgRepository.findAll().stream()
                    .filter(o -> o.getType() == OrgType.USER).map(OrgEntity::getId).sorted().toList()
                : List.of(user.getOrgId());

        List<HazardEntity> hazards = user.getRole().isAgency()
                ? hazardRepository.findAll()
                : hazardRepository.findByOrgId(user.getOrgId());
        Map<Long, List<HazardEntity>> hazardsByOrg = hazards.stream()
                .collect(Collectors.groupingBy(HazardEntity::getOrgId));

        List<OrgBattle> orgs = new ArrayList<>();
        for (Long orgId : orgIds) {
            List<DeviceEntity> orgDevices = byOrg.getOrDefault(orgId, List.of());

            Map<String, Long> funnel = new LinkedHashMap<>();
            for (DeviceStatus s : DeviceStatus.values()) {
                funnel.put(s.name(), orgDevices.stream().filter(d -> d.getStatus() == s).count());
            }

            List<DeviceBrief> dueSoon = new ArrayList<>();
            List<DeviceBrief> overdue = new ArrayList<>();
            for (DeviceEntity d : orgDevices) {
                if (d.getStatus() != DeviceStatus.IN_USE || d.getNextInspectionDate() == null) {
                    continue;
                }
                if (d.getNextInspectionDate().isBefore(today)) {
                    overdue.add(DeviceBrief.of(d));
                } else if (!d.getNextInspectionDate().isAfter(dueLimit)) {
                    dueSoon.add(DeviceBrief.of(d));
                }
            }
            dueSoon.sort(Comparator.comparing(DeviceBrief::nextInspectionDate));
            overdue.sort(Comparator.comparing(DeviceBrief::nextInspectionDate));

            List<HazardEntity> orgHazards = hazardsByOrg.getOrDefault(orgId, List.of());
            long open = orgHazards.stream().filter(h -> h.getStatus() == HazardEntity.Status.OPEN).count();
            long overdueHz = orgHazards.stream().filter(HazardEntity::isOverdue).count();

            orgs.add(new OrgBattle(orgId, orgNames.getOrDefault(orgId, "未知单位"),
                    funnel, dueSoon, overdue, open, overdueHz));
        }
        return ApiResponse.ok(new BattleView(LocalDateTime.now(), dueDays, orgs));
    }
}
