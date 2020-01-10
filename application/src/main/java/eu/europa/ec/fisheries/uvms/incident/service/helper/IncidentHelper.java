package eu.europa.ec.fisheries.uvms.incident.service.helper;

import eu.europa.ec.fisheries.schema.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.asset.client.AssetClient;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetIdentifier;
import eu.europa.ec.fisheries.uvms.incident.service.domain.dto.*;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.IncidentLog;
import eu.europa.ec.fisheries.uvms.incident.service.domain.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentHelper {

    @EJB
    private AssetClient assetClient;

    @EJB
    private MovementRestClient movementClient;

    public Incident constructIncident(TicketType ticket, MicroMovement movement) {
        Incident incident = new Incident();
        if (ticket.getMobileTerminalGuid() != null) {
            incident.setMobileTerminalId(UUID.fromString(ticket.getMobileTerminalGuid()));
        }
        incident.setCreateDate(Instant.now());
        incident.setStatus(StatusEnum.POLL_FAILED);
        incident.setTicketId(UUID.fromString(ticket.getGuid()));
        if (movement != null) {
            incident.setMovementId(UUID.fromString(movement.getGuid()));
        }
        setAssetValues(incident, ticket.getAssetGuid());
        return incident;
    }

    private void setAssetValues(Incident incident, String assetId) {
        AssetDTO asset = assetClient.getAssetById(AssetIdentifier.GUID, assetId);
        if(asset != null) {
            incident.setAssetId(asset.getId());
            incident.setAssetName(asset.getName());
            incident.setIrcs(asset.getIrcs());
        }
    }

    public IncidentDto incidentEntityToDto(Incident incident) {
        return mapEntityToDto(incident);
    }

    public List<IncidentDto> incidentToDtoList(List<Incident> incidentList) {
        List<IncidentDto> retVal = new ArrayList<>();
        for (Incident i : incidentList) {
            IncidentDto dto = mapEntityToDto(i);
            retVal.add(dto);
        }
        return retVal;
    }

    private IncidentDto mapEntityToDto(Incident entity) {
        IncidentDto dto = new IncidentDto();
        dto.setId(entity.getId());
        dto.setAssetId(entity.getAssetId());
        dto.setMobileTerminalId(entity.getMobileTerminalId());
        dto.setTicketId(entity.getTicketId());
        dto.setAssetName(entity.getAssetName());
        dto.setAssetIrcs(entity.getIrcs());
        dto.setStatus(entity.getStatus().name());
        dto.setCreateDate(entity.getCreateDate().getEpochSecond());
        if (entity.getUpdateDate() != null)
            dto.setUpdateDate(entity.getUpdateDate().getEpochSecond());

        if(entity.getMovementId() != null) {
            MicroMovement micro = movementClient.getMicroMovementById(entity.getMovementId());

            MicroMovementDto lastKnownLocation = new MicroMovementDto();

            MovementPointDto location = new MovementPointDto();
            location.setLatitude(micro.getLocation().getLatitude());
            location.setLongitude(micro.getLocation().getLongitude());
            if (micro.getLocation().getAltitude() != null)
                location.setAltitude(micro.getLocation().getAltitude());

            lastKnownLocation.setLocation(location);
            lastKnownLocation.setHeading(micro.getHeading());
            lastKnownLocation.setGuid(micro.getGuid());
            lastKnownLocation.setTimestamp(micro.getTimestamp().getEpochSecond());
            lastKnownLocation.setSpeed(micro.getSpeed());
            lastKnownLocation.setSource(MovementSourceType.fromValue(micro.getSource().name()));

            dto.setLastKnownLocation(lastKnownLocation);
        }

        return dto;
    }

    public List<IncidentLogDto> incidentLogToDtoList(List<IncidentLog> incidentLogList) {
        List<IncidentLogDto> retVal = new ArrayList<>();
        for (IncidentLog entity : incidentLogList) {
            IncidentLogDto dto = new IncidentLogDto();
            dto.setId(entity.getId());
            dto.setIncidentId(entity.getIncidentId());
            dto.setMessage(entity.getMessage());
            dto.setEventType(entity.getEventType().name());
            dto.setCreateDate(entity.getCreateDate().getEpochSecond());
            dto.setPreviousValue(entity.getPreviousValue());
            dto.setCurrentValue(entity.getCurrentValue());
            retVal.add(dto);
        }
        return retVal;
    }

    public MicroMovementDto mapToMicroMovementDto(MicroMovement model) {
        MicroMovementDto dto = new MicroMovementDto();
        dto.setGuid(model.getGuid());
        dto.setHeading(model.getHeading());
        dto.setSource(MovementSourceType.fromValue(model.getSource().name()));
        dto.setSpeed(model.getSpeed());
        dto.setTimestamp(model.getTimestamp().getEpochSecond());
        dto.setLocation(mapToMovementPointDto(model.getLocation()));
        return dto;
    }

    private MovementPointDto mapToMovementPointDto(MovementPoint model) {
        MovementPointDto dto = new MovementPointDto();
        dto.setLatitude(model.getLatitude());
        dto.setLongitude(model.getLongitude());
        if (model.getAltitude() != null) {
            dto.setAltitude(model.getAltitude());
        }
        return dto;
    }
}
