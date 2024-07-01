package fish.focus.uvms.incident.service.bean;

import fish.focus.uvms.incident.model.dto.EventCreationDto;
import fish.focus.uvms.incident.model.dto.IncidentDto;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.incident.model.dto.OpenAndRecentlyResolvedIncidentsDto;
import fish.focus.uvms.incident.model.dto.enums.*;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.dao.IncidentDao;
import fish.focus.uvms.incident.service.dao.IncidentLogDao;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import fish.focus.uvms.incident.service.domain.interfaces.IncidentCreate;
import fish.focus.uvms.incident.service.domain.interfaces.IncidentUpdate;
import fish.focus.uvms.incident.service.helper.IncidentHelper;
import fish.focus.uvms.incident.service.helper.IncidentLogData;
import fish.focus.uvms.movement.client.MovementRestClient;
import fish.focus.uvms.movement.model.dto.MovementDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class IncidentServiceBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentServiceBean.class);

    private static final String UUID_PATTERN = "[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}";

    @Inject
    AssetCommunicationBean assetCommunication;

    @Inject
    MovementRestClient movementRestClient;

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @Inject
    @IncidentCreate
    private Event<Incident> createdIncident;

    @Inject
    @IncidentUpdate
    private Event<Incident> updatedIncident;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    @Inject
    private RiskCalculationsBean riskCalculationsBean;

    public OpenAndRecentlyResolvedIncidentsDto getAllOpenAndRecentlyResolvedIncidents() {
        OpenAndRecentlyResolvedIncidentsDto dto = new OpenAndRecentlyResolvedIncidentsDto();
        List<Incident> unresolvedIncidents = incidentDao.findOpenByTypes(Arrays.asList(IncidentType.values()));
        dto.setUnresolved(incidentHelper.incidentToDtoMap(unresolvedIncidents));

        List<Incident> resolvedSinceLast12Hours = incidentDao.findByStatusAndUpdatedSince12Hours(Arrays.asList(IncidentType.values()));
        dto.setRecentlyResolved(incidentHelper.incidentToDtoMap(resolvedSinceLast12Hours));
        return dto;
    }

    public void createIncident(IncidentTicketDto ticket) {
        if (ticket.getId() == null) {
            upsertIncidentLackingTicketId(ticket);
        } else {
            internalCreateIncident(ticket);
        }
    }

    private void internalCreateIncident(IncidentTicketDto ticket) {
        if (ticket.getType() == null) {
            return;
        }

        try {
            if (IncidentType.ASSET_NOT_SENDING.equals(ticket.getType())) {
                Optional<String> pollId = assetCommunication.createPollInternal(ticket);
                if (pollId.isEmpty()) {
                    // don't create an incident for inactive/parked assets
                    return;
                }
                ticket.setPollId(pollId.get());
            }

            Incident incident = incidentHelper.constructIncident(ticket);
            incidentDao.save(incident);

            if ("Asset not sending".equalsIgnoreCase(ticket.getRuleGuid())) {
                RiskLevel riskLevel = riskCalculationsBean.calculateRiskLevelForIncident(incident);
                incident.setRisk(riskLevel);

                if (ticket.getPollId() != null && !ticket.getPollId().matches(UUID_PATTERN)) {
                    IncidentLogData data = new IncidentLogData();
                    data.setErrorMessage(ticket.getPollId());
                    String json = incidentHelper.createJsonString(data);
                    incidentLogServiceBean.createIncidentLogForStatus(incident,
                            EventTypeEnum.AUTO_POLL_CREATION_FAILED, null, json);
                } else {
                    incidentLogServiceBean.createIncidentLogForStatus(incident,
                            EventTypeEnum.AUTO_POLL_CREATED, (ticket.getPollId() == null ? null : UUID.fromString(ticket.getPollId())),
                            null);
                }
            } else {
                IncidentLogData data = new IncidentLogData();
                data.setUser(ticket.getRuleGuid());
                String json = incidentHelper.createJsonString(data);
                incidentLogServiceBean.createIncidentLogForStatus(incident,
                        EventTypeEnum.INCIDENT_CREATED, null, json);
            }

            createdIncident.fire(incident);
        } catch (IllegalArgumentException e) {
            LOG.error("Error: {} from UUID {}", e.getMessage(), ticket.getPollId());
            throw e;
        }
    }

    public IncidentDto createIncident(IncidentDto incidentDto, String user) {
        Incident incident = incidentHelper.incidentDtoToIncident(incidentDto);
        incidentHelper.checkIfUpdateIsAllowed(incident, incidentDto.getStatus());
        incidentHelper.setCorrectValuesForIncidentType(incident);
        incident.setCreateDate(Instant.now());
        Incident persistedIncident = incidentDao.save(incident);

        IncidentLogData data = new IncidentLogData();
        data.setUser(user);
        String json = incidentHelper.createJsonString(data);
        incidentLogServiceBean.createIncidentLogForStatus(persistedIncident, EventTypeEnum.INCIDENT_CREATED, null, json);
        createdIncident.fire(persistedIncident);
        return incidentHelper.incidentEntityToDto(persistedIncident);
    }

    public IncidentDto updateIncidentType(Long incidentId, IncidentType update, String user) {
        Incident oldIncident = incidentDao.findById(incidentId);
        incidentHelper.checkIfUpdateIsAllowed(oldIncident, oldIncident.getStatus());

        IncidentType oldType = oldIncident.getType();
        oldIncident.setType(update);
        oldIncident.setStatus(update.getValidStatuses().get(0));

        incidentHelper.setCorrectValuesForIncidentType(oldIncident);
        Incident updated = incidentDao.update(oldIncident);

        IncidentLogData data = new IncidentLogData();
        data.setUser(user);
        data.setFrom(oldType.name());
        data.setTo(update.name());
        String json = incidentHelper.createJsonString(data);
        incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_TYPE, null, json);

        updatedIncident.fire(updated);
        return incidentHelper.incidentEntityToDto(updated);
    }

    public IncidentDto updateIncidentStatus(Long incidentId, StatusEnum update, String user) {
        Incident oldIncident = incidentDao.findById(incidentId);
        incidentHelper.checkIfUpdateIsAllowed(oldIncident, update);

        StatusEnum oldStatus = oldIncident.getStatus();
        oldIncident.setStatus(update);

        Incident updated = incidentDao.update(oldIncident);

        IncidentLogData data = new IncidentLogData();
        data.setUser(user);
        data.setFrom(oldStatus.name());
        data.setTo(update.name());
        String json = incidentHelper.createJsonString(data);
        if (update.equals(StatusEnum.RESOLVED)) {
            incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_CLOSED, null, json);
        } else {
            incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_STATUS, null, json);
        }

        updatedIncident.fire(updated);
        return incidentHelper.incidentEntityToDto(oldIncident);
    }


    public IncidentDto updateIncidentExpiry(Long incidentId, Instant update, String user) {
        Incident oldIncident = incidentDao.findById(incidentId);
        incidentHelper.checkIfUpdateIsAllowed(oldIncident, oldIncident.getStatus());

        if (oldIncident.getType().equals(IncidentType.ASSET_NOT_SENDING)) {
            throw new IllegalArgumentException("Asset not sending does not support having an expiry date");
        } else if (oldIncident.getType().equals(IncidentType.MANUAL_POSITION_MODE)) {
            throw new IllegalArgumentException("Manual position mode does not support that the user sets an expiry date");
        }

        oldIncident.setExpiryDate(update);

        Incident updated = incidentDao.update(oldIncident);

        IncidentLogData data = new IncidentLogData();
        data.setUser(user);
        data.setExpiry(update);
        String json = incidentHelper.createJsonString(data);
        incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.EXPIRY_UPDATED, null, json);

        if (updated.getType().equals(IncidentType.PARKED) || updated.getType().equals(IncidentType.SEASONAL_FISHING)) {
            data = new IncidentLogData();
            data.setUser(user);
            if (updated.getStatus().equals(StatusEnum.OVERDUE) && updated.getExpiryDate().isAfter(Instant.now())) {
                updated.setStatus(updated.getType().getValidStatuses().get(0));
                data.setFrom(StatusEnum.OVERDUE.name());
                data.setTo(updated.getStatus().name());
                json = incidentHelper.createJsonString(data);
                incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_STATUS, null, json);
            } else if (updated.getExpiryDate().isBefore(Instant.now())) {
                data.setFrom(updated.getStatus().name());
                updated.setStatus(StatusEnum.OVERDUE);
                data.setTo(StatusEnum.OVERDUE.name());
                json = incidentHelper.createJsonString(data);
                incidentLogServiceBean.createIncidentLogForStatus(updated, EventTypeEnum.INCIDENT_STATUS, null, json);
            }
        }

        updatedIncident.fire(updated);
        return incidentHelper.incidentEntityToDto(oldIncident);
    }

    public void updateIncident(IncidentTicketDto ticket) {
        if (ticket.getId() == null) {
            upsertIncidentLackingTicketId(ticket);
        } else {
            Incident persisted = incidentDao.findByTicketId(ticket.getId());
            internalUpdateIncident(ticket, persisted);
        }
    }

    private void internalUpdateIncident(IncidentTicketDto ticket, Incident persisted) {
        if (persisted == null) {
            return;
        }

        switch (persisted.getType()) {
            case ASSET_NOT_SENDING:
                updateAssetNotSending(ticket, persisted);
                break;
            case MANUAL_POSITION_MODE:
                updateManualMovement(ticket, persisted);
                break;
            case PARKED:
                updateParked(ticket, persisted);
                break;
            case SEASONAL_FISHING:
                updateSeasonalFishing(ticket, persisted);
                break;
            case OWNERSHIP_TRANSFER:
                updateOwnerTransfer(ticket, persisted);
                break;
        }

        Incident updated = incidentDao.update(persisted);
        updatedIncident.fire(updated);
    }

    private void updateAssetNotSending(IncidentTicketDto ticket, Incident persisted) {
        UUID movementId = UUID.fromString(ticket.getMovementId());
        if (ticket.getMovementId() == null || movementId.equals(persisted.getMovementId())) {
            return;
        }

        if (ticket.getMovementSource() != null && ticket.getMovementSource().equals(MovementSourceType.MANUAL)) {
            persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
            persisted.setType(IncidentType.MANUAL_POSITION_MODE);
            persisted.setExpiryDate(ticket.getPositionTime().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES));

            IncidentLogData data = new IncidentLogData();
            data.setUser("UVMS");
            data.setFrom(IncidentType.ASSET_NOT_SENDING.name());
            data.setTo(IncidentType.MANUAL_POSITION_MODE.name());
            data.setExpiry(persisted.getExpiryDate());
            String json = incidentHelper.createJsonString(data);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.INCIDENT_TYPE, null, json);

            persisted.setMovementId(movementId);
            incidentLogServiceBean.createIncidentLogForManualPosition(persisted, movementId);
        } else if (ticket.getMovementSource() != null && !ticket.getMovementSource().equals(MovementSourceType.AIS)) {
            persisted.setStatus(StatusEnum.RESOLVED);
            Incident updated = incidentDao.update(persisted);
            incidentLogServiceBean.createIncidentLogForStatus(updated,
                    EventTypeEnum.INCIDENT_CLOSED, movementId, null);
        }
    }

    private void updateManualMovement(IncidentTicketDto ticket, Incident persisted) {
        if (ticket.getMovementSource().equals(MovementSourceType.MANUAL)
                && !incidentLogDao.checkIfMovementAlreadyExistsForIncident(persisted.getId(), UUID.fromString(ticket.getMovementId()))) {

            if (!(persisted.getExpiryDate() != null
                    && persisted.getExpiryDate().isAfter(ticket.getPositionTime().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES)))) {
                persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
                persisted.setMovementId(UUID.fromString(ticket.getMovementId()));
                persisted.setExpiryDate(ticket.getPositionTime().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES));
            }

            incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));

        } else if (ticket.getMovementSource().equals(MovementSourceType.AIS)) {   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if (recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            } else {
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()), null);
            }
        } else {
            checkAndUpdateIncidentPosition(ticket, persisted);
            persisted.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
            incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()), null);
        }
    }

    private void updateParked(IncidentTicketDto ticket, Incident persisted) {
        if (ticket.getMovementSource().equals(MovementSourceType.AIS)) {   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if (recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            } else {
                persisted.setStatus(StatusEnum.RECEIVING_AIS_POSITIONS);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()), null);
            }
        } else {
            checkAndUpdateIncidentPosition(ticket, persisted);
            if (ticket.getMovementSource().equals(MovementSourceType.MANUAL)) {
                incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));
            } else {
                persisted.setStatus(StatusEnum.RESOLVED);
                assetCommunication.setAssetParkedStatus(persisted.getAssetId(), false);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()), null);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()), null);
            }

        }
    }

    private void updateSeasonalFishing(IncidentTicketDto ticket, Incident persisted) {
        if (ticket.getMovementSource().equals(MovementSourceType.AIS)) {   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if (recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            } else {
                persisted.setStatus(StatusEnum.RECEIVING_AIS_POSITIONS);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()), null);
            }
        } else {
            checkAndUpdateIncidentPosition(ticket, persisted);
            if (ticket.getMovementSource().equals(MovementSourceType.MANUAL)) {
                incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));
            } else {
                persisted.setStatus(StatusEnum.RESOLVED);
                assetCommunication.setAssetParkedStatus(persisted.getAssetId(), false);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()), null);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.INCIDENT_CLOSED, UUID.fromString(ticket.getMovementId()), null);
            }
        }
    }

    private void updateOwnerTransfer(IncidentTicketDto ticket, Incident persisted) {
        if (ticket.getMovementSource().equals(MovementSourceType.AIS)) {   //how often should I do this?
            IncidentLog recentAis = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(persisted.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
            if (recentAis != null) {
                recentAis.setCreateDate(Instant.now());
                recentAis.setRelatedObjectId(UUID.fromString(ticket.getMovementId()));
            } else {
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_AIS_POSITION, UUID.fromString(ticket.getMovementId()), null);
            }
        } else {
            checkAndUpdateIncidentPosition(ticket, persisted);
            if (ticket.getMovementSource().equals(MovementSourceType.MANUAL)) {
                incidentLogServiceBean.createIncidentLogForManualPosition(persisted, UUID.fromString(ticket.getMovementId()));
            } else {
                persisted.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
                incidentLogServiceBean.createIncidentLogForStatus(persisted, EventTypeEnum.RECEIVED_VMS_POSITION, UUID.fromString(ticket.getMovementId()), null);

            }
        }
    }

    private void checkAndUpdateIncidentPosition(IncidentTicketDto ticket, Incident persisted) {
        MovementDto microMovementById = persisted.getMovementId() != null ? movementRestClient.getMovementById(persisted.getMovementId()) : null;
        if (microMovementById == null || ticket.getPositionTime().isAfter(microMovementById.getTimestamp())) {
            persisted.setMovementId(UUID.fromString(ticket.getMovementId()));
        }
    }

    public void addEventToIncident(long incidentId, EventCreationDto eventCreationDto) {
        Incident persisted = incidentDao.findById(incidentId);
        if (persisted.getStatus().equals(StatusEnum.RESOLVED) && !eventCreationDto.getEventType().equals(EventTypeEnum.NOTE_CREATED)) {
            throw new IllegalArgumentException("Not allowed to add event to incident " + incidentId + " since it has status 'RESOLVED'");
        }

        incidentLogServiceBean.createIncidentLogForStatus(persisted,
                eventCreationDto.getEventType(), eventCreationDto.getRelatedObjectId(), null);
    }

    public Incident findByTicketId(UUID ticketId) {
        return incidentDao.findByTicketId(ticketId);
    }

    public void upsertIncidentLackingTicketId(IncidentTicketDto ticketDto) {
        List<Incident> openByAsset = incidentDao.findOpenByAsset(UUID.fromString(ticketDto.getAssetId()));
        if (openByAsset != null && !openByAsset.isEmpty()) {
            if (ticketDto.getType() != null) {
                return;
            }
            Incident openIncident = openByAsset.get(0);
            internalUpdateIncident(ticketDto, openIncident);
        } else {
            internalCreateIncident(ticketDto);
        }
    }

    public List<Incident> findOpenByAssetId(UUID assetId) {
        return incidentDao.findOpenByAsset(assetId);
    }
}
