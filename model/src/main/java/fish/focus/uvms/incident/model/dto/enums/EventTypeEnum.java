package fish.focus.uvms.incident.model.dto.enums;

public enum EventTypeEnum {
    MANUAL_POSITION("New Manual position created", RelatedObjectType.MOVEMENT),
    MANUAL_POSITION_LATE("No new manual position in 65 minutes while in manual position mode", RelatedObjectType.NONE),
    RECEIVED_AIS_POSITION("Received AIS position", RelatedObjectType.MOVEMENT),
    RECEIVED_VMS_POSITION("Received VMS position", RelatedObjectType.MOVEMENT),
    POLL_CREATED("Poll has been created", RelatedObjectType.POLL),
    AUTO_POLL_CREATED("Automatic poll has been created", RelatedObjectType.POLL),
    AUTO_POLL_CREATION_FAILED("Automatic poll creation has failed", RelatedObjectType.NONE),
    NOTE_CREATED("Note has been created", RelatedObjectType.NOTE),
    INCIDENT_CREATED("Incident have been created", RelatedObjectType.NONE),
    EXPIRY_UPDATED("Incident expiry has been updated", RelatedObjectType.NONE),
    INCIDENT_CLOSED("Incident has been closed", RelatedObjectType.MOVEMENT),
    INCIDENT_STATUS("Incident Status updated", RelatedObjectType.NONE),
    INCIDENT_TYPE("Incident Type updated", RelatedObjectType.NONE);

    private String message;
    private RelatedObjectType relatedObjectType;

    EventTypeEnum(String message, RelatedObjectType relatedObjectType) {
        this.message = message;
        this.relatedObjectType = relatedObjectType;
    }

    public String getMessage() {
        return message;
    }

    public RelatedObjectType getRelatedObjectType() {
        return relatedObjectType;
    }
}

