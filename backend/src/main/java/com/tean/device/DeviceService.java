package com.tean.device;

import com.tean.auth.LoginUser;
import com.tean.auth.Role;
import com.tean.common.BizException;
import com.tean.device.DeviceDtos.DeviceView;
import com.tean.device.DeviceDtos.TransitionOption;
import com.tean.hazard.HazardEntity;
import com.tean.hazard.HazardRepository;
import com.tean.org.OrgRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final DeviceStatusHistoryRepository historyRepository;
    private final AddressRevealLogRepository revealLogRepository;
    private final HazardRepository hazardRepository;
    private final OrgRepository orgRepository;
    private final DeviceStateMachine stateMachine;

    /** 数据可见性：特检机构看全部；设备管理员看本单位；维保人员看本维保单位承维设备 */
    public static boolean canView(LoginUser user, DeviceEntity device) {
        if (user.getRole().isAgency()) {
            return true;
        }
        if (user.getRole() == Role.DEVICE_ADMIN) {
            return device.getOrgId().equals(user.getOrgId());
        }
        if (user.getRole() == Role.MAINTAINER) {
            return device.getMaintOrgId().equals(user.getOrgId());
        }
        return false;
    }

    /** 状态机操作权限：使用单位设备管理员或特检机构人员 */
    public static boolean canOperate(LoginUser user, DeviceEntity device) {
        if (user.getRole().isAgency()) {
            return true;
        }
        return user.getRole() == Role.DEVICE_ADMIN && device.getOrgId().equals(user.getOrgId());
    }

    public Page<DeviceView> list(LoginUser user, DeviceStatus status, DeviceType type,
                                 String keyword, int page, int size) {
        Specification<DeviceEntity> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            // 租户隔离：机构外角色只能看到本机构关联设备
            if (!user.getRole().isAgency()) {
                if (user.getRole() == Role.DEVICE_ADMIN) {
                    ps.add(cb.equal(root.get("orgId"), user.getOrgId()));
                } else {
                    ps.add(cb.equal(root.get("maintOrgId"), user.getOrgId()));
                }
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                ps.add(cb.equal(root.get("type"), type));
            }
            if (StringUtils.hasText(keyword)) {
                ps.add(cb.or(
                        cb.like(root.get("code"), "%" + keyword + "%"),
                        cb.like(root.get("name"), "%" + keyword + "%")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<DeviceEntity> result = deviceRepository.findAll(spec, PageRequest.of(page, size));
        Map<Long, String> orgNames = orgNames();
        return result.map(d -> toView(d, orgNames, user));
    }

    public DeviceEntity requireVisible(LoginUser user, Long id) {
        DeviceEntity device = deviceRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("设备不存在"));
        if (!canView(user, device)) {
            // 越权访问他人设备：明确错误态，不回空数据
            throw BizException.forbidden("无权查看其他单位的设备档案");
        }
        return device;
    }

    public DeviceView detail(LoginUser user, Long id) {
        return toView(requireVisible(user, id), orgNames(), user);
    }

    @Transactional
    public DeviceView transit(LoginUser user, Long id, DeviceStatus to, String reason) {
        DeviceEntity device = requireVisible(user, id);
        if (!canOperate(user, device)) {
            throw BizException.forbidden("维保人员无设备档案状态操作权限");
        }
        stateMachine.assertTransit(device.getStatus(), to);

        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setDeviceId(device.getId());
        history.setFromStatus(device.getStatus());
        history.setToStatus(to);
        history.setReason(reason);
        history.setOperatorId(user.getId());
        history.setOperatorName(user.getName());
        historyRepository.save(history);

        device.setStatus(to);
        deviceRepository.save(device);
        return toView(device, orgNames(), user);
    }

    public List<DeviceStatusHistory> history(LoginUser user, Long id) {
        requireVisible(user, id);
        return historyRepository.findByDeviceIdOrderByCreatedAtDesc(id);
    }

    /** 监察员二次确认后查看精确地址，全程留痕 */
    @Transactional
    public DeviceDtos.RevealResult revealAddress(LoginUser user, Long id, String reason, boolean confirm, String ip) {
        if (user.getRole() != Role.SUPERVISOR) {
            throw BizException.forbidden("仅监察员可查看设备精确地址");
        }
        if (!confirm) {
            throw BizException.badRequest("查看精确地址需二次确认");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 4) {
            throw BizException.badRequest("请填写查看理由（不少于4个字）");
        }
        DeviceEntity device = requireVisible(user, id);

        AddressRevealLog log = new AddressRevealLog();
        log.setDeviceId(device.getId());
        log.setUserId(user.getId());
        log.setUserName(user.getName());
        log.setReason(reason.trim());
        log.setIp(ip);
        revealLogRepository.save(log);

        return new DeviceDtos.RevealResult(device.getBuildingName(), device.getExactAddress());
    }

    public List<AddressRevealLog> revealLogs(LoginUser user, Long id) {
        if (user.getRole() != Role.SUPERVISOR) {
            throw BizException.forbidden("仅监察员可查看地址查看留痕");
        }
        requireVisible(user, id);
        return revealLogRepository.findByDeviceIdOrderByCreatedAtDesc(id);
    }

    private DeviceView toView(DeviceEntity d, Map<Long, String> orgNames, LoginUser viewer) {
        long openHazards = hazardRepository.countByDeviceIdAndStatus(d.getId(), HazardEntity.Status.OPEN);
        List<TransitionOption> transitions = canOperate(viewer, d)
                ? stateMachine.allowedTargets(d.getStatus()).stream()
                    .map(t -> new TransitionOption(t, t.getLabel()))
                    .toList()
                : List.of();
        return DeviceView.of(d, orgNames.getOrDefault(d.getOrgId(), "未知单位"), openHazards, transitions, viewer);
    }

    private Map<Long, String> orgNames() {
        return orgRepository.findAll().stream()
                .collect(Collectors.toMap(o -> o.getId(), o -> o.getName()));
    }

    public Map<Long, String> orgNameMap() {
        return orgNames();
    }

    /** 供作战台/维保等模块按可见性取设备 */
    public List<DeviceEntity> listVisible(LoginUser user) {
        return deviceRepository.findAll().stream()
                .filter(d -> canView(user, d))
                .toList();
    }

    public Function<Long, String> orgNamer() {
        Map<Long, String> names = orgNames();
        return id -> names.getOrDefault(id, "未知单位");
    }
}
