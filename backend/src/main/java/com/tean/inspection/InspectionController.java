package com.tean.inspection;

import com.tean.auth.LoginUser;
import com.tean.auth.Role;
import com.tean.auth.UserContext;
import com.tean.common.ApiResponse;
import com.tean.common.BizException;
import com.tean.device.DeviceEntity;
import com.tean.device.DeviceRepository;
import com.tean.device.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {
    private final InspectionRecordRepository recordRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;

    public record CreateRequest(@NotNull Long deviceId, @NotNull InspectionRecord.Result result,
                                @NotNull LocalDate inspectedAt, LocalDate nextInspectionDate,
                                String reportNo, String remark) {}

    @GetMapping
    public ApiResponse<List<InspectionRecord>> list(@RequestParam Long deviceId) {
        deviceService.requireVisible(UserContext.require(), deviceId);
        return ApiResponse.ok(recordRepository.findByDeviceIdOrderByInspectedAtDesc(deviceId));
    }

    /** 检验员登记检验结论，并回写设备检验日期 */
    @PostMapping
    @Transactional
    public ApiResponse<InspectionRecord> create(@Valid @RequestBody CreateRequest req) {
        LoginUser user = UserContext.require();
        if (user.getRole() != Role.INSPECTOR && user.getRole() != Role.SUPERVISOR) {
            throw BizException.forbidden("仅检验员可登记检验记录");
        }
        DeviceEntity device = deviceService.requireVisible(user, req.deviceId());

        InspectionRecord record = new InspectionRecord();
        record.setDeviceId(device.getId());
        record.setOrgId(device.getOrgId());
        record.setInspectorId(user.getId());
        record.setInspectorName(user.getName());
        record.setResult(req.result());
        record.setInspectedAt(req.inspectedAt());
        record.setNextInspectionDate(req.nextInspectionDate());
        record.setReportNo(req.reportNo());
        record.setRemark(req.remark());
        InspectionRecord saved = recordRepository.save(record);

        device.setLastInspectionDate(req.inspectedAt());
        if (req.nextInspectionDate() != null) {
            device.setNextInspectionDate(req.nextInspectionDate());
        }
        deviceRepository.save(device);
        return ApiResponse.ok(saved);
    }
}
