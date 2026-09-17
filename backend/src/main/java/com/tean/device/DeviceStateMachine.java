package com.tean.device;

import com.tean.common.BizException;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tean.device.DeviceStatus.*;

/**
 * 设备档案状态机。
 * 合法流转：注册告知→验收→在用⇄停用→报废；其余一律拦截并说明原因。
 */
@Component
public class DeviceStateMachine {

    private static final Map<DeviceStatus, Set<DeviceStatus>> ALLOWED = Map.of(
            REGISTERED, EnumSet.of(ACCEPTED, SCRAPPED),
            ACCEPTED, EnumSet.of(IN_USE, SCRAPPED),
            IN_USE, EnumSet.of(SUSPENDED, SCRAPPED),
            SUSPENDED, EnumSet.of(IN_USE, SCRAPPED),
            SCRAPPED, EnumSet.noneOf(DeviceStatus.class)
    );

    /** 常见非法流转的具体原因，未列出的走兜底文案 */
    private static final Map<String, String> REASONS = Map.ofEntries(
            Map.entry("SCRAPPED->*", "设备已报废，档案已封存，不允许任何状态变更"),
            Map.entry("IN_USE->REGISTERED", "设备已在用，不允许回退到注册告知；如需退出使用，请先停用再报废"),
            Map.entry("IN_USE->ACCEPTED", "设备已在用，不允许回退到验收状态"),
            Map.entry("ACCEPTED->REGISTERED", "设备已完成验收，不允许回退到注册告知"),
            Map.entry("SUSPENDED->REGISTERED", "停用设备不允许回退到注册告知；可启用恢复在用，或办理报废"),
            Map.entry("SUSPENDED->ACCEPTED", "停用设备不允许回退到验收状态；可启用恢复在用，或办理报废"),
            Map.entry("REGISTERED->IN_USE", "设备尚未验收，不允许直接投入使用，请先完成验收"),
            Map.entry("REGISTERED->SUSPENDED", "设备尚未投入使用，无需停用"),
            Map.entry("ACCEPTED->SUSPENDED", "设备尚未投入使用，无需停用")
    );

    public boolean canTransit(DeviceStatus from, DeviceStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    /** 校验流转，非法时抛出带原因的业务异常 */
    public void assertTransit(DeviceStatus from, DeviceStatus to) {
        if (from == to) {
            throw BizException.badRequest("设备已处于「" + from.getLabel() + "」状态，无需重复操作");
        }
        if (canTransit(from, to)) {
            return;
        }
        String reason = REASONS.getOrDefault(from.name() + "->" + to.name(),
                REASONS.getOrDefault(from.name() + "->*",
                        "当前状态「" + from.getLabel() + "」不允许变更为「" + to.getLabel() + "」"));
        throw BizException.badRequest(reason);
    }

    /** 当前状态下允许的目标状态（前端据此渲染可用操作） */
    public Set<DeviceStatus> allowedTargets(DeviceStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }
}
