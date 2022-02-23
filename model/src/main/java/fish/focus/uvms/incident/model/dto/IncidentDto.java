package fish.focus.uvms.incident.model.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.RiskLevel;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;

public class IncidentDto implements Serializable {
    private final static long serialVersionUID = 1L;

    private Long id;
    private UUID assetId;
    private UUID mobileTerminalId;
    private UUID ticketId;
    private IncidentType type;
    private String assetName;
    private String assetIrcs;
    private StatusEnum status;
    private RiskLevel risk;
    private Instant createDate;
    private Instant updateDate;
    private MovementDto lastKnownLocation;
    private Instant expiryDate;

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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetIrcs() {
        return assetIrcs;
    }

    public void setAssetIrcs(String assetIrcs) {
        this.assetIrcs = assetIrcs;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public MovementDto getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(MovementDto lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
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
}
