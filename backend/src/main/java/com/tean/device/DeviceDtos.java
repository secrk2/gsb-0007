package com.tean.device;

import com.tean.auth.LoginUser;
import com.tean.auth.Role;

import java.util.List;

public final class DeviceDtos {

    private DeviceDtos() {}

    /** 脱敏后的设备视图：位置只给区域+编号 */
    public record DeviceView(
            Long id,
            String code,
            String name,
            DeviceType type,
            String typeLabel,
            DeviceStatus status,
            String statusLabel,
            Long orgId,
            String orgName,
            String maskedLocation,
            String lastInspectionDate,
            String nextInspectionDate,
            long openHazards,
            List<TransitionOption> allowedTransitions,
            boolean canRevealAddress
    ) {
        public static DeviceView of(DeviceEntity d, String orgName, long openHazards,
                                    List<TransitionOption> transitions, LoginUser viewer) {
            return new DeviceView(
                    d.getId(), d.getCode(), d.getName(),
                    d.getType(), d.getType().getLabel(),
                    d.getStatus(), d.getStatus().getLabel(),
                    d.getOrgId(), orgName,
                    d.getRegionCode() + " · " + d.getLocationCode(),
                    d.getLastInspectionDate() == null ? null : d.getLastInspectionDate().toString(),
                    d.getNextInspectionDate() == null ? null : d.getNextInspectionDate().toString(),
                    openHazards, transitions,
                    viewer.getRole() == Role.SUPERVISOR);
        }
    }

    public record TransitionOption(DeviceStatus to, String label) {}

    public record TransitionRequest(DeviceStatus to, String reason) {}

    public record RevealRequest(String reason, boolean confirm) {}

    public record RevealResult(String buildingName, String exactAddress) {}
}
