package com.tean.device;

import com.tean.common.BizException;
import com.tean.common.PageResult;
import com.tean.dashboard.DashboardCache;
import com.tean.device.DeviceDtos.*;
import com.tean.orgunit.OrgUnit;
import com.tean.orgunit.OrgUnitRepository;
import com.tean.security.AuthUser;
import com.tean.user.Role;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设备档案：多单位数据隔离、状态机流转、地址脱敏与查看留痕。
 */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusLogRepository statusLogRepository;
    private final AddressRevealLogRepository revealLogRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DashboardCache dashboardCache;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceStatusLogRepository statusLogRepository,
                         AddressRevealLogRepository revealLogRepository,
                         OrgUnitRepository orgUnitRepository,
                         DashboardCache dashboardCache) {
        this.deviceRepository = deviceRepository;
        this.statusLogRepository = statusLogRepository;
        this.revealLogRepository = revealLogRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.dashboardCache = dashboardCache;
    }

    // ---------- 查询 ----------

    public PageResult<DeviceView> page(AuthUser user, DeviceStatus status, DeviceType type,
                                       String keyword, int page, int size) {
        Specification<Device> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (user.isOrgScoped()) {
                ps.add(cb.equal(root.get("orgId"), user.getOrgId()));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                ps.add(cb.equal(root.get("type"), type));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("deviceCode"), like),
                        cb.like(root.get("maskCode"), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Device> result = deviceRepository.findAll(spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, String> orgNames = orgNamesOf(result.getContent());
        return PageResult.of(result, d -> DeviceView.of(d, orgNames.get(d.getOrgId())));
    }

    public DeviceDetail detail(AuthUser user, Long id) {
        Device device = mustGet(id);
        checkScope(user, device);
        Map<Long, String> orgNames = orgNamesOf(List.of(device));
        List<StatusLogItem> logs = statusLogRepository.findByDeviceIdOrderByCreatedAtDesc(id).stream()
                .map(this::toLogItem)
                .toList();
        List<StatusOption> allowed = DeviceStatusMachine.allowedTargets(device.getStatus()).stream()
                .sorted(Comparator.comparingInt(DeviceStatus::ordinal))
                .map(s -> new StatusOption(s.name(), s.getLabel()))
                .toList();
        return new DeviceDetail(
                DeviceView.of(device, orgNames.get(device.getOrgId())),
                device.getModel(),
                device.getInstallDate(),
                device.getCreatedAt(),
                allowed,
                user.isSupervisor(),
                logs);
    }

    // ---------- 状态流转 ----------

    @Transactional
    public DeviceDetail transition(AuthUser user, Long id, TransitionRequest request) {
        Device device = mustGet(id);
        checkScope(user, device);
        if (user.getRole() != Role.DEVICE_ADMIN && user.getRole() != Role.INSPECTOR) {
            throw BizException.forbidden("当前角色「" + user.getRole().getLabel() + "」无权变更设备状态，仅设备管理员或检验员可操作");
        }
        DeviceStatus to = parseStatus(request.toStatus());
        DeviceStatus from = device.getStatus();
        DeviceStatusMachine.check(from, to);

        device.setStatus(to);
        deviceRepository.save(device);

        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(device.getId());
        log.setFromStatus(from.name());
        log.setToStatus(to.name());
        log.setOperatorId(user.getId());
        log.setOperatorName(user.getRealName());
        log.setReason(request.reason() == null || request.reason().isBlank()
                ? from.getLabel() + " → " + to.getLabel()
                : request.reason().trim());
        statusLogRepository.save(log);

        dashboardCache.evictAll();
        return detail(user, id);
    }

    // ---------- 精确地址：二次确认 + 留痕 ----------

    @Transactional
    public RevealResult revealAddress(AuthUser user, Long id, RevealRequest request, String ip) {
        if (!user.isSupervisor()) {
            throw BizException.forbidden("仅监察员可查看设备精确地址；当前角色「" + user.getRole().getLabel() + "」无权查看");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw BizException.badRequest("请填写查看理由，理由将写入审计留痕");
        }
        if (!Boolean.TRUE.equals(request.confirm())) {
            throw BizException.badRequest("查看精确地址需二次确认，请勾选确认后重试");
        }
        Device device = mustGet(id);

        AddressRevealLog log = new AddressRevealLog();
        log.setDeviceId(device.getId());
        log.setUserId(user.getId());
        log.setUsername(user.getUsername());
        log.setRealName(user.getRealName());
        log.setReason(request.reason().trim());
        log.setIp(ip);
        revealLogRepository.save(log);

        return new RevealResult(
                device.getExactAddress(),
                user.getRealName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "本次查看已留痕（操作人、理由、时间、IP），可在查看记录中追溯");
    }

    public List<RevealLogItem> revealLogs(AuthUser user, Long id) {
        if (!user.isSupervisor()) {
            throw BizException.forbidden("仅监察员可查看地址查看记录");
        }
        mustGet(id);
        return revealLogRepository.findByDeviceIdOrderByCreatedAtDesc(id).stream()
                .map(l -> new RevealLogItem(l.getRealName(), l.getUsername(), l.getReason(), l.getIp(), l.getCreatedAt()))
                .toList();
    }

    // ---------- 内部 ----------

    private Device mustGet(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("设备不存在或已被删除"));
    }

    /** 多单位隔离：使用单位侧账号越权访问他单位设备时，返回明确错误而非空数据 */
    private void checkScope(AuthUser user, Device device) {
        if (user.isOrgScoped() && !Objects.equals(device.getOrgId(), user.getOrgId())) {
            throw BizException.forbidden("无权访问其他使用单位的设备，本次越权访问已被拦截");
        }
    }

    private DeviceStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw BizException.badRequest("缺少目标状态 toStatus");
        }
        try {
            return DeviceStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw BizException.badRequest("未知目标状态「" + value + "」，合法值："
                    + Arrays.stream(DeviceStatus.values()).map(Enum::name).collect(Collectors.joining("/")));
        }
    }

    private Map<Long, String> orgNamesOf(Collection<Device> devices) {
        Set<Long> orgIds = devices.stream().map(Device::getOrgId).collect(Collectors.toSet());
        return orgUnitRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getName));
    }

    private StatusLogItem toLogItem(DeviceStatusLog log) {
        return new StatusLogItem(
                log.getFromStatus(), labelOf(log.getFromStatus()),
                log.getToStatus(), labelOf(log.getToStatus()),
                log.getOperatorName(), log.getReason(), log.getCreatedAt());
    }

    private String labelOf(String statusName) {
        if (statusName == null) {
            return "—";
        }
        try {
            return DeviceStatus.valueOf(statusName).getLabel();
        } catch (IllegalArgumentException e) {
            return statusName;
        }
    }

    /** 供看板复用 */
    public Function<Device, DeviceView> viewMapper(Map<Long, String> orgNames) {
        return d -> DeviceView.of(d, orgNames.get(d.getOrgId()));
    }
}
