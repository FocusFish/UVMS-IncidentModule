package fish.focus.uvms.incident.service;

import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;

import java.util.Arrays;
import java.util.List;

public class ServiceConstants {

    public static final int MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES = 65;
    public static final int OWNER_TRANSFER_VMS_THRESHOLD_IN_DAYS = 1;

    public static final List<StatusEnum> RESOLVED_STATUS_LIST = Arrays.asList(StatusEnum.RESOLVED);
    public static final List<IncidentType> REACT_ON_RECENT_AIS = Arrays.asList(IncidentType.PARKED, IncidentType.SEASONAL_FISHING);
}
