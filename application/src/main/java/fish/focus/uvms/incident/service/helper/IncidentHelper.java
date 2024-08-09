package fish.focus.uvms.incident.service.helper;

import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.asset.client.model.AssetIdentifier;
import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.incident.model.dto.*;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.MovementSourceType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import fish.focus.uvms.movement.client.MovementRestClient;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Stateless
public class IncidentHelper {

    @Inject
    private AssetClient assetClient;

    @Inject
    private MovementRestClient movementClient;

    private Jsonb jsonb = new JsonBConfigurator().getContext(null);


    public Incident constructIncident(IncidentTicketDto ticket) {
        Incident incident = new Incident();
        if (ticket.getMobTermId() != null) {
            incident.setMobileTerminalId(UUID.fromString(ticket.getMobTermId()));
        }
        incident.setCreateDate(Instant.now());
        incident.setStatus(StatusEnum.INCIDENT_CREATED);
        incident.setTicketId(ticket.getId());
        incident.setType(ticket.getType());
        incident.setMovementId(ticket.getMovementId() != null ? UUID.fromString(ticket.getMovementId()) : null);
        setAssetValues(incident, ticket.getAssetId());
        return incident;
    }

    private void setAssetValues(Incident incident, String assetId) {
        AssetDTO asset = assetClient.getAssetById(AssetIdentifier.GUID, assetId);
        if (asset != null) {
            incident.setAssetId(asset.getId());
            incident.setAssetName(asset.getName());
            incident.setIrcs(asset.getIrcs());
        } else {
            throw new IllegalArgumentException("Trying to create an incident for an assetGuid that does not exist. Guid: " + assetId);
        }
    }

    public Incident incidentDtoToIncident(IncidentDto incidentDto) {
        Incident incident = new Incident();
        return populateIncident(incident, incidentDto);
    }

    public Incident populateIncident(Incident incident, IncidentDto incidentDto) {
        incident.setAssetId(incidentDto.getAssetId());
        incident.setMobileTerminalId(incidentDto.getMobileTerminalId());
        incident.setTicketId(incidentDto.getTicketId());
        incident.setType(incidentDto.getType());
        incident.setAssetName(incidentDto.getAssetName());
        incident.setIrcs(incidentDto.getAssetIrcs());
        incident.setExpiryDate(incidentDto.getExpiryDate());
        if (incidentDto.getLastKnownLocation() != null) {
            incident.setMovementId(incidentDto.getLastKnownLocation().getId());
        }
        incident.setStatus(incidentDto.getStatus());
        incident.setRisk(incidentDto.getRisk());
        return incident;
    }

    public IncidentDto incidentEntityToDto(Incident incident) {
        fish.focus.uvms.movement.model.dto.MovementDto movement = null;
        if (incident.getMovementId() != null) {
            movement = movementClient.getMovementById(incident.getMovementId());
            movement = movement != null && movement.getId() != null ? movement : null;    //for the sole reason that getMovementById returns an empty object if that move id does not exist
        }
        return mapEntityToDto(incident, movement);
    }

    public Map<Long, IncidentDto> incidentToDtoMap(List<Incident> incidentList) {
        List<UUID> movementIds = incidentList.stream()
                .map(Incident::getMovementId)
                .collect(Collectors.toList());
        Map<UUID, fish.focus.uvms.movement.model.dto.MovementDto> movementMap = movementClient.getMovementDtoByIdList(movementIds)
                .stream()
                .collect(Collectors.toMap(fish.focus.uvms.movement.model.dto.MovementDto::getId, Function.identity()));
        return incidentList
                .stream()
                .collect(Collectors.toMap(Incident::getId, i -> mapEntityToDto(i, movementMap.get(i.getMovementId()))));
    }

    private IncidentDto mapEntityToDto(Incident entity, fish.focus.uvms.movement.model.dto.MovementDto move) {
        IncidentDto dto = new IncidentDto();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setMobileTerminalId(entity.getMobileTerminalId());
        dto.setTicketId(entity.getTicketId());
        dto.setType(entity.getType());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetIrcs(entity.getIrcs());
        dto.setStatus(entity.getStatus());
        dto.setRisk(entity.getRisk());
        dto.setCreateDate(entity.getCreateDate());
        dto.setExpiryDate(entity.getExpiryDate());
        if (entity.getUpdateDate() != null) {
            dto.setUpdateDate(entity.getUpdateDate());
        }
        if (entity.getMovementId() != null && move != null) {
            dto.setLastKnownLocation(mapMovementMovementToIncidentMovement(move));
        }
        return dto;
    }

    private MovementDto mapMovementMovementToIncidentMovement(fish.focus.uvms.movement.model.dto.MovementDto move) {
        MovementDto dto = new MovementDto();

        MovementPointDto location = new MovementPointDto();
        location.setLatitude(move.getLocation().getLatitude());
        location.setLongitude(move.getLocation().getLongitude());
        dto.setLocation(location);

        dto.setAisPositionAccuracy(move.getAisPositionAccuracy());
        dto.setAsset(move.getAsset());
        dto.setHeading(move.getHeading());
        dto.setId(move.getId());
        dto.setLesReportTime(move.getLesReportTime());
        dto.setMovementType(move.getMovementType() != null ? move.getMovementType().value() : null);
        dto.setSource(MovementSourceType.fromValue(move.getSource().value()));
        dto.setSourceSatelliteId(move.getSourceSatelliteId() != null ? move.getSourceSatelliteId().name() : null);
        dto.setSpeed(move.getSpeed());
        dto.setStatus(move.getStatus());
        dto.setTimestamp(move.getTimestamp());
        dto.setUpdated(move.getUpdated());
        dto.setUpdatedBy(move.getUpdatedBy());

        return dto;
    }

    public Map<Long, IncidentLogDto> incidentLogToDtoMap(List<IncidentLog> incidentLogList) {
        Map<Long, IncidentLogDto> retVal = new HashMap<>(incidentLogList.size());
        for (IncidentLog entity : incidentLogList) {
            IncidentLogDto dto = new IncidentLogDto();
            dto.setId(entity.getId());
            dto.setIncidentId(entity.getIncidentId());
            dto.setMessage(entity.getMessage());
            dto.setEventType(entity.getEventType());
            dto.setCreateDate(entity.getCreateDate());
            dto.setRelatedObjectId(entity.getRelatedObjectId());
            dto.setRelatedObjectType(entity.getRelatedObjectType());
            dto.setIncidentStatus(entity.getIncidentStatus());
            dto.setData(entity.getData());
            retVal.put(dto.getId(), dto);
        }
        return retVal;
    }

    public void checkIfUpdateIsAllowed(Incident oldIncident, StatusEnum status) {
        if (oldIncident.getStatus().equals(StatusEnum.RESOLVED)) {
            throw new IllegalArgumentException("Not allowed to update incident " + oldIncident.getId() + " since it has status 'RESOLVED'");
        }

        if (!oldIncident.getType().getValidStatuses().contains(status)) {
            throw new IllegalArgumentException("Incident type " + oldIncident.getType() + " does not support being placed in status " + status);
        }
    }

    public void setCorrectValuesForIncidentType(Incident incident) {

        if (incident.getType().equals(IncidentType.ASSET_NOT_SENDING)) {
            incident.setExpiryDate(null);
        }

        if (incident.getType().equals(IncidentType.MANUAL_POSITION_MODE)) {
            if (incident.getStatus().equals(StatusEnum.RESOLVED)) {
                //do nothing
            } else {
                Instant expiry;
                if (incident != null && incident.getMovementId() != null) {
                    fish.focus.uvms.movement.model.dto.MovementDto microMovementById = movementClient.getMovementById(incident.getMovementId());
                    StatusEnum status;
                    if (microMovementById != null) {
                        expiry = microMovementById.getTimestamp().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES);
                        status = expiry.isBefore(Instant.now()) ? StatusEnum.MANUAL_POSITION_LATE : StatusEnum.MANUAL_POSITION_MODE;

                    } else {
                        status = StatusEnum.MANUAL_POSITION_MODE;
                        expiry = Instant.now().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES);
                    }
                    incident.setStatus(status);

                } else {
                    expiry = Instant.now().plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES);
                }
                incident.setExpiryDate(expiry);
            }
        }
    }

    public String createJsonString(IncidentLogData data) {
        return jsonb.toJson(data);
    }

}
