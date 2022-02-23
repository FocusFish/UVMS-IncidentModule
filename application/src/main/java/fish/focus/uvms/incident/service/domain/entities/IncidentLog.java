package fish.focus.uvms.incident.service.domain.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.model.dto.enums.RelatedObjectType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_log")
@NamedQueries({
        @NamedQuery(name = IncidentLog.FIND_ALL_BY_INCIDENT_ID, query = "SELECT i FROM IncidentLog i WHERE i.incidentId = :incidentId"),
        @NamedQuery(name = IncidentLog.FIND_ALL_BY_INCIDENT_ID_LIST, query = "SELECT i FROM IncidentLog i WHERE i.incidentId in :incidentId"),
        @NamedQuery(name = IncidentLog.CHECK_IF_INCIDENT_ALREADY_HAS_MOVEMENT, query = "SELECT i FROM IncidentLog i WHERE i.incidentId = :incidentId AND i.relatedObjectId = :movementId"),
        @NamedQuery(name = IncidentLog.FIND_LOG_WITH_EVENT_TYPE_AFTER, query = "SELECT i FROM IncidentLog i WHERE i.incidentId = :incidentId AND i.eventType = :eventType AND i.createDate >= :date"),

})
public class IncidentLog {

    public static final String FIND_ALL_BY_INCIDENT_ID = "IncidentLog.findByIncidentId";
    public static final String FIND_ALL_BY_INCIDENT_ID_LIST = "IncidentLog.findByIncidentIdList";
    public static final String CHECK_IF_INCIDENT_ALREADY_HAS_MOVEMENT = "IncidentLog.findIfIncidentAlreadyHasMovement";
    public static final String FIND_LOG_WITH_EVENT_TYPE_AFTER = "IncidentLog.findLogWithEventTypeAfter";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", name = "id")
    private Long id;

    @NotNull
    @Column(name = "incident_id")
    private long incidentId;

    @NotNull
    @Column(name = "message")
    private String message;

    @NotNull
    @Column(name = "event_type")
    @Enumerated(value = EnumType.STRING)
    private EventTypeEnum eventType;

    @Column(name = "related_object_id")
    private UUID relatedObjectId;

    @NotNull
    @Column(name = "create_date")
    private Instant createDate;

    @Column(name = "related_object_type")
    @Enumerated(value = EnumType.STRING)
    private RelatedObjectType relatedObjectType;

    @Column(name = "incident_status")
    @Enumerated(value = EnumType.STRING)
    private StatusEnum incidentStatus;

    @Column(name = "data")
    private String data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(long incidentId) {
        this.incidentId = incidentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEnum eventType) {
        this.eventType = eventType;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public UUID getRelatedObjectId() {
        return relatedObjectId;
    }

    public void setRelatedObjectId(UUID relatedObjectId) {
        this.relatedObjectId = relatedObjectId;
    }

    public RelatedObjectType getRelatedObjectType() {
        return relatedObjectType;
    }

    public void setRelatedObjectType(RelatedObjectType relatedObjectType) {
        this.relatedObjectType = relatedObjectType;
    }

    public StatusEnum getIncidentStatus() {
        return incidentStatus;
    }

    public void setIncidentStatus(StatusEnum incidentStatus) {
        this.incidentStatus = incidentStatus;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
