package com.tean.hazard;

import com.tean.common.BizException;
import com.tean.dashboard.DashboardCache;
import com.tean.device.Device;
import com.tean.device.DeviceRepository;
import com.tean.hazard.HazardDtos.HazardItem;
import com.tean.orgunit.OrgUnit;
import com.tean.orgunit.OrgUnitRepository;
import com.tean.security.AuthUser;
import com.tean.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HazardService {

    private final HazardRepository hazardRepository;
    private final DeviceRepository deviceRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DashboardCache dashboardCache;

    public HazardService(HazardRepository hazardRepository, DeviceRepository deviceRepository,
                         OrgUnitRepository orgUnitRepository, DashboardCache dashboardCache) {
        this.hazardRepository = hazardRepository;
        this.deviceRepository = deviceRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.dashboardCache = dashboardCache;
    }

    public List<HazardItem> list(AuthUser user, HazardStatus status) {
        List<Hazard> hazards = status == null
                ? hazardRepository.findAll()
                : hazardRepository.findByStatus(status);
        Map<Long, Device> devices = deviceRepository.findAllById(
                        hazards.stream().map(Hazard::getDeviceId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<Long, String> orgNames = orgUnitRepository.findAllById(
                        devices.values().stream().map(Device::getOrgId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getName));
        LocalDate today = LocalDate.now();
        return hazards.stream()
                .map(h -> {
                    Device d = devices.get(h.getDeviceId());
                    if (d == null) {
                        return null;
                    }
                    // 使用单位隔离：越权数据直接不可见
                    if (user.isOrgScoped() && !Objects.equals(d.getOrgId(), user.getOrgId())) {
                        return null;
                    }
                    return HazardItem.of(h, d.getName(), orgNames.get(d.getOrgId()), today);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<HazardItem> listByDevice(AuthUser user, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> BizException.notFound("设备不存在或已被删除"));
        if (user.isOrgScoped() && !Objects.equals(device.getOrgId(), user.getOrgId())) {
            throw BizException.forbidden("无权访问其他使用单位的设备隐患");
        }
        String orgName = orgUnitRepository.findById(device.getOrgId()).map(OrgUnit::getName).orElse("");
        LocalDate today = LocalDate.now();
        return hazardRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId).stream()
                .map(h -> HazardItem.of(h, device.getName(), orgName, today))
                .toList();
    }

    /** 隐患闭环 */
    @Transactional
    public HazardItem resolve(AuthUser user, Long id) {
        Hazard hazard = hazardRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("隐患不存在"));
        Device device = deviceRepository.findById(hazard.getDeviceId())
                .orElseThrow(() -> BizException.notFound("隐患关联设备不存在"));
        if (user.isOrgScoped() && !Objects.equals(device.getOrgId(), user.getOrgId())) {
            throw BizException.forbidden("无权处理其他使用单位的隐患");
        }
        if (user.getRole() == Role.MAINTAINER) {
            throw BizException.forbidden("维保人员无权闭环隐患，请由设备管理员或检验员确认");
        }
        if (hazard.getStatus() == HazardStatus.RESOLVED) {
            throw BizException.conflict("ALREADY_RESOLVED", "该隐患已闭环，无需重复操作");
        }
        hazard.setStatus(HazardStatus.RESOLVED);
        hazard.setResolvedAt(LocalDateTime.now());
        hazardRepository.save(hazard);
        dashboardCache.evictAll();
        String orgName = orgUnitRepository.findById(device.getOrgId()).map(OrgUnit::getName).orElse("");
        return HazardItem.of(hazard, device.getName(), orgName, LocalDate.now());
    }
}
