package com.tean.device;

import com.tean.common.BizException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备档案状态机：注册告知 → 验收 → 在用 → 停用 → 报废。
 * 停用可恢复在用；报废为终态；其余回退/跳变一律拦截并说明原因。
 */
public final class DeviceStatusMachine {

    private static final Map<DeviceStatus, Set<DeviceStatus>> ALLOWED = Map.of(
            DeviceStatus.REGISTERED, EnumSet.of(DeviceStatus.ACCEPTED),
            DeviceStatus.ACCEPTED, EnumSet.of(DeviceStatus.IN_USE),
            DeviceStatus.IN_USE, EnumSet.of(DeviceStatus.SUSPENDED, DeviceStatus.SCRAPPED),
            DeviceStatus.SUSPENDED, EnumSet.of(DeviceStatus.IN_USE, DeviceStatus.SCRAPPED),
            DeviceStatus.SCRAPPED, EnumSet.noneOf(DeviceStatus.class));

    private DeviceStatusMachine() {
    }

    public static Set<DeviceStatus> allowedTargets(DeviceStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }

    /**
     * 校验流转合法性；非法时抛出 409 并说明原因。
     */
    public static void check(DeviceStatus from, DeviceStatus to) {
        if (from == to) {
            throw BizException.conflict("ILLEGAL_TRANSITION",
                    "设备已处于「" + from.getLabel() + "」，无需重复变更");
        }
        Set<DeviceStatus> allowed = allowedTargets(from);
        if (!allowed.contains(to)) {
            String reason;
            if (from == DeviceStatus.SCRAPPED) {
                reason = "「报废」为终态，不允许再变更到「" + to.getLabel() + "」";
            } else {
                String allowedText = allowed.stream()
                        .map(s -> "「" + s.getLabel() + "」")
                        .collect(Collectors.joining("、"));
                reason = "当前状态「" + from.getLabel() + "」仅允许流转至 " + allowedText
                        + "，不允许变更到「" + to.getLabel() + "」。"
                        + "设备状态沿 注册告知→验收→在用→停用→报废 单向流转（停用可恢复在用），禁止回退或跳变";
            }
            throw BizException.conflict("ILLEGAL_TRANSITION", reason);
        }
    }
}
