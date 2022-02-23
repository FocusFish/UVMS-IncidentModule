package fish.focus.uvms.incident.model.dto;

import java.io.Serializable;
import java.util.UUID;
import fish.focus.uvms.incident.model.dto.enums.*;

public class EventCreationDto implements Serializable {

    private EventTypeEnum eventType;
    private UUID relatedObjectId;

    public EventCreationDto() {
    }

    public EventCreationDto(EventTypeEnum eventType, UUID relatedObjectId) {
        this.eventType = eventType;
        this.relatedObjectId = relatedObjectId;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public UUID getRelatedObjectId() {
        return relatedObjectId;
    }

    public void setRelatedObjectId(UUID relatedObjectId) {
        this.relatedObjectId = relatedObjectId;
    }
}
