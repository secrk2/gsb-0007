package com.tean.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tean.dashboard.DashboardDtos.*;
import com.tean.device.Device;
import com.tean.device.DeviceRepository;
import com.tean.device.DeviceStatus;
import com.tean.hazard.Hazard;
import com.tean.hazard.HazardDtos.HazardItem;
import com.tean.hazard.HazardRepository;
import com.tean.hazard.HazardStatus;
import com.tean.orgunit.OrgUnit;
import com.tean.orgunit.OrgUnitRepository;
import com.tean.security.AuthUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备作战台：按使用单位聚合在用漏斗、检验临期、隐患与逾期红点。
 * 结果缓存 Redis 60s；设备/隐患变更时主动失效。
 */
@Slf4j
@Service
public class DashboardService {

    /** 机构自身单位编码（不作为使用单位出现在漏斗中） */
    public static final String AGENCY_CODE = "AGENCY";

    private final DeviceRepository deviceRepository;
    private final HazardRepository hazardRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DashboardCache cache;
    private final ObjectMapper objectMapper;
    private final int dueSoonDays;

    public DashboardService(DeviceRepository deviceRepository,
                            HazardRepository hazardRepository,
                            OrgUnitRepository orgUnitRepository,
                            DashboardCache cache,
                            ObjectMapper objectMapper,
                            @Value("${tean.due-soon-days:30}") int dueSoonDays) {
        this.deviceRepository = deviceRepository;
        this.hazardRepository = hazardRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.cache = cache;
        this.objectMapper = objectMapper;
        this.dueSoonDays = dueSoonDays;
    }

    public Summary summary(AuthUser user) {
        String scopeKey = user.isOrgScoped() ? "org:" + user.getOrgId() : "all";
        String cached = cache.get(scopeKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, Summary.class);
            } catch (Exception e) {
                log.warn("看板缓存反序列化失败，重新计算: {}", e.getMessage());
            }
        }
        Summary summary = build(user);
        try {
            cache.put(scopeKey, objectMapper.writeValueAsString(summary));
        } catch (Exception e) {
            log.warn("看板缓存序列化失败: {}", e.getMessage());
        }
        return summary;
    }

    private Summary build(AuthUser user) {
        LocalDate today = LocalDate.now();
        LocalDate soonLimit = today.plusDays(dueSoonDays);

        List<OrgUnit> orgs = orgUnitRepository.findAll().stream()
                .filter(o -> !AGENCY_CODE.equals(o.getCode()))
                .filter(o -> !user.isOrgScoped() || Objects.equals(o.getId(), user.getOrgId()))
                .toList();
        List<Long> orgIds = orgs.stream().map(OrgUnit::getId).toList();
        Map<Long, String> orgNames = orgs.stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getName));

        List<Device> devices = orgIds.isEmpty() ? List.of() : deviceRepository.findByOrgIdIn(orgIds);
        Map<Long, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, d -> d));

        List<Hazard> openHazards = devices.isEmpty() ? List.of()
                : hazardRepository.findByStatusAndDeviceIdIn(HazardStatus.OPEN, deviceById.keySet());

        List<DueDevice> dueSoon = devices.stream()
                .filter(d -> d.getStatus() == DeviceStatus.IN_USE)
                .filter(d -> d.getNextInspectionDate() != null
                        && !d.getNextInspectionDate().isBefore(today)
                        && !d.getNextInspectionDate().isAfter(soonLimit))
                .map(d -> toDue(d, orgNames, today, false))
                .sorted(Comparator.comparing(DueDevice::nextInspectionDate))
                .toList();

        List<DueDevice> overdue = devices.stream()
                .filter(d -> d.getStatus() != DeviceStatus.SCRAPPED)
                .filter(d -> d.getNextInspectionDate() != null && d.getNextInspectionDate().isBefore(today))
                .map(d -> toDue(d, orgNames, today, true))
                .sorted(Comparator.comparing(DueDevice::nextInspectionDate))
                .toList();

        List<HazardItem> hazardItems = openHazards.stream()
                .map(h -> {
                    Device d = deviceById.get(h.getDeviceId());
                    return HazardItem.of(h, d.getName(), orgNames.get(d.getOrgId()), today);
                })
                .sorted(Comparator.comparing(HazardItem::overdue).reversed()
                        .thenComparing(HazardItem::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<OrgFunnel> funnels = orgs.stream().map(org -> {
            List<Device> own = devices.stream()
                    .filter(d -> Objects.equals(d.getOrgId(), org.getId())).toList();
            long dueSoonCount = dueSoon.stream().filter(d -> d.orgName().equals(org.getName())).count();
            long overdueCount = overdue.stream().filter(d -> d.orgName().equals(org.getName())).count();
            long openCount = hazardItems.stream().filter(h -> h.orgName().equals(org.getName())).count();
            long overdueHazardCount = hazardItems.stream()
                    .filter(h -> h.orgName().equals(org.getName()) && h.overdue()).count();
            return new OrgFunnel(
                    org.getId(), org.getName(),
                    count(own, DeviceStatus.REGISTERED),
                    count(own, DeviceStatus.ACCEPTED),
                    count(own, DeviceStatus.IN_USE),
                    count(own, DeviceStatus.SUSPENDED),
                    count(own, DeviceStatus.SCRAPPED),
                    dueSoonCount, overdueCount, openCount, overdueHazardCount,
                    overdueCount > 0 || overdueHazardCount > 0);
        }).toList();

        Totals totals = new Totals(
                devices.size(),
                devices.stream().filter(d -> d.getStatus() == DeviceStatus.IN_USE).count(),
                dueSoon.size(), overdue.size(),
                hazardItems.size(),
                hazardItems.stream().filter(HazardItem::overdue).count());

        return new Summary(totals, funnels, dueSoon, overdue, hazardItems);
    }

    private long count(List<Device> devices, DeviceStatus status) {
        return devices.stream().filter(d -> d.getStatus() == status).count();
    }

    private DueDevice toDue(Device d, Map<Long, String> orgNames, LocalDate today, boolean overdue) {
        long days = overdue
                ? ChronoUnit.DAYS.between(d.getNextInspectionDate(), today)
                : ChronoUnit.DAYS.between(today, d.getNextInspectionDate());
        return new DueDevice(d.getId(), d.getDeviceCode(), d.getName(), d.getType().getLabel(),
                orgNames.get(d.getOrgId()), d.getNextInspectionDate(), days);
    }
}
