package fish.focus.uvms.incident.model.dto.enums;

import java.util.Arrays;
import java.util.List;

public enum IncidentType {
    ASSET_NOT_SENDING(StatusEnum.INCIDENT_CREATED, StatusEnum.ATTEMPTED_CONTACT, StatusEnum.RESOLVED),
    SEASONAL_FISHING(StatusEnum.PARKED, StatusEnum.RECEIVING_AIS_POSITIONS, StatusEnum.OVERDUE, StatusEnum.RESOLVED),
    OWNERSHIP_TRANSFER(StatusEnum.NOT_RECEIVING_VMS_POSITIONS, StatusEnum.RECEIVING_VMS_POSITIONS, StatusEnum.RESOLVED),
    PARKED(StatusEnum.PARKED, StatusEnum.RECEIVING_AIS_POSITIONS, StatusEnum.OVERDUE, StatusEnum.RESOLVED),
    MANUAL_POSITION_MODE(StatusEnum.MANUAL_POSITION_MODE, StatusEnum.MANUAL_POSITION_LATE, StatusEnum.RECEIVING_VMS_POSITIONS, StatusEnum.RESOLVED);

    private List<StatusEnum> validStatuses;

    IncidentType(StatusEnum... statuses) {
        validStatuses = Arrays.asList(statuses);
    }

    public List<StatusEnum> getValidStatuses() {
        return validStatuses;
    }
}
