package com.tean.dashboard;

import com.tean.hazard.HazardDtos.HazardItem;

import java.time.LocalDate;
import java.util.List;

/**
 * 作战台 DTO。
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record Totals(long devices, long inUse, long dueSoon, long overdue,
                         long openHazards, long overdueHazards) {
    }

    /** 单个使用单位的漏斗与红点 */
    public record OrgFunnel(Long orgId, String orgName,
                            long registered, long accepted, long inUse, long suspended, long scrapped,
                            long dueSoon, long overdue,
                            long openHazards, long overdueHazards,
                            boolean redDot) {
    }

    /** 临期/逾期设备条目。days：临期为剩余天数，逾期为已逾期天数 */
    public record DueDevice(Long id, String deviceCode, String name, String typeLabel,
                            String orgName, LocalDate nextInspectionDate, long days) {
    }

    public record Summary(Totals totals,
                          List<OrgFunnel> orgs,
                          List<DueDevice> dueSoon,
                          List<DueDevice> overdue,
                          List<HazardItem> openHazards) {
    }
}
