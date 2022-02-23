package fish.focus.uvms.incident.service.domain.entities;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.RiskLevel;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "incident")
@NamedQueries({
        @NamedQuery(name = Incident.FIND_ALL_EXCLUDE_STATUS, query = "SELECT i FROM Incident i WHERE i.type = :type AND i.status NOT IN :status"),
        @NamedQuery(name = Incident.FIND_BY_STATUS, query = "SELECT i FROM Incident i WHERE i.status = :status"),
        @NamedQuery(name = Incident.FIND_BY_STATUS_AND_UPDATED_SINCE, query = "SELECT i FROM Incident i WHERE i.type in :type AND i.status IN :status AND i.updateDate > :updatedSince"),
        @NamedQuery(name = Incident.FIND_BY_TICKET_ID, query = "SELECT i FROM Incident i WHERE i.ticketId = :ticketId"),
        @NamedQuery(name = Incident.FIND_BY_ASSET_ID, query = "SELECT i FROM Incident i WHERE i.assetId = :assetId"),
        @NamedQuery(name = Incident.FIND_BY_ASSET_TYPE_AND_EXCLUDE_STATUS, query = "SELECT i FROM Incident i WHERE i.assetId = :assetId AND i.type = :type AND i.status NOT IN :status"),
        @NamedQuery(name = Incident.FIND_BY_TYPES_AND_EXCLUDE_STATUS, query = "SELECT i FROM Incident i WHERE i.type IN :type AND i.status NOT IN :status"),
        @NamedQuery(name = Incident.FIND_BY_ASSET_AND_EXCLUDE_STATUS, query = "SELECT i FROM Incident i WHERE i.assetId = :assetId AND i.status NOT IN :status"),
})
public class Incident {

    public static final String FIND_ALL_EXCLUDE_STATUS = "Incident.findByNotInStatus";
    public static final String FIND_BY_STATUS = "Incident.findByStatus";
    public static final String FIND_BY_TICKET_ID = "Incident.findByTicketId";
    public static final String FIND_BY_STATUS_AND_UPDATED_SINCE = "Incident.findByStatusAndUpdatedSince";
    public static final String FIND_BY_ASSET_ID = "Incident.findByAssetId";
    public static final String FIND_BY_ASSET_TYPE_AND_EXCLUDE_STATUS = "Incident.findByAssetTypeAndExcludeStatus";
    public static final String FIND_BY_TYPES_AND_EXCLUDE_STATUS = "Incident.findByTypeAndExcludeStatus";
    public static final String FIND_BY_ASSET_AND_EXCLUDE_STATUS = "Incident.findByAssetAndExcludeStatus";

    /* Daniel Wirdehäll 2020-06-25
    Ja, jag föredrar löpnummer.
    Ser ingen anledning för UUID i dessa läget.
     */

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "mobterm_id")
    private UUID mobileTerminalId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @NotNull
    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    private IncidentType type;

    @NotNull
    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "ircs")
    private String ircs;

    @Column(name = "movement_id")
    private UUID movementId;

    @NotNull
    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private StatusEnum status;

    @Column(name = "risk")
    @Enumerated(value = EnumType.STRING)
    private RiskLevel risk;

    @NotNull
    @Column(name = "create_date")
    private Instant createDate;

    @Column(name = "update_date")
    private Instant updateDate;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @PrePersist
    @PreUpdate
    public void preUpdate(){
        updateDate = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public void setAssetId(UUID assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getIrcs() {
        return ircs;
    }

    public void setIrcs(String ircs) {
        this.ircs = ircs;
    }

    public UUID getMobileTerminalId() {
        return mobileTerminalId;
    }

    public void setMobileTerminalId(UUID mobileTerminalId) {
        this.mobileTerminalId = mobileTerminalId;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public IncidentType getType() {
        return type;
    }

    public void setType(IncidentType type) {
        this.type = type;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public UUID getMovementId() {
        return movementId;
    }

    public void setMovementId(UUID movementId) {
        this.movementId = movementId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public RiskLevel getRisk() {
        return risk;
    }

    public void setRisk(RiskLevel risk) {
        this.risk = risk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incident that = (Incident) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(assetId, that.assetId) &&
                Objects.equals(mobileTerminalId, that.mobileTerminalId) &&
                Objects.equals(ticketId, that.ticketId) &&
                Objects.equals(assetName, that.assetName) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(updateDate, that.updateDate) &&
                Objects.equals(movementId, that.movementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
