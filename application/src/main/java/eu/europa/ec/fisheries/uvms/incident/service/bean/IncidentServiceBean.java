package eu.europa.ec.fisheries.uvms.incident.service.bean;

import eu.europa.ec.fisheries.schema.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketStatusType;
import eu.europa.ec.fisheries.schema.movementrules.ticket.v1.TicketType;
import eu.europa.ec.fisheries.uvms.incident.model.dto.IncidentTicketDto;
import eu.europa.ec.fisheries.uvms.incident.service.dao.IncidentDao;
import eu.europa.ec.fisheries.uvms.incident.model.dto.StatusDto;
import eu.europa.ec.fisheries.uvms.incident.service.domain.entities.Incident;
import eu.europa.ec.fisheries.uvms.incident.service.domain.enums.StatusEnum;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentCreate;
import eu.europa.ec.fisheries.uvms.incident.service.domain.interfaces.IncidentUpdate;
import eu.europa.ec.fisheries.uvms.incident.service.helper.IncidentHelper;
import eu.europa.ec.fisheries.uvms.movement.client.MovementRestClient;
import eu.europa.ec.fisheries.uvms.movement.client.model.MicroMovement;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentServiceBean {

    @Inject
    private IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    private IncidentHelper incidentHelper;

    @EJB
    private MovementRestClient movementClient;

    @Inject
    @IncidentCreate
    private Event<Incident> createdIncident;

    @Inject
    @IncidentUpdate
    private Event<Incident> updatedIncident;

    @Inject
    private IncidentDao incidentDao;

    public List<Incident> getAssetNotSendingList() {
        List<Incident> unresolvedIncidents = incidentDao.findUnresolvedIncidents();
        List<Incident> resolvedSinceLast12Hours = incidentDao.findByStatusAndUpdatedSince();
        unresolvedIncidents.addAll(resolvedSinceLast12Hours);
        return unresolvedIncidents;
    }

    public void createIncident(IncidentTicketDto ticket) {
        MicroMovement movement = movementClient.getMicroMovementById(UUID.fromString(ticket.getMovementId()));
        if ("Asset not sending".equalsIgnoreCase(ticket.getRuleGuid())) {
            Incident incident = incidentHelper.constructIncident(ticket, movement);
            incidentDao.save(incident);
            createdIncident.fire(incident);
        }
    }

    public void updateIncident(IncidentTicketDto ticket) {
        Incident persisted = incidentDao.findByTicketId(ticket.getId());

        if (persisted != null) {
            String incidentStatus = persisted.getStatus().name();

            if (ticket.getStatus().equals(TicketStatusType.CLOSED)) {
                persisted.setStatus(StatusEnum.RESOLVED);
                Incident updated = incidentDao.update(persisted);
                updatedIncident.fire(updated);
                incidentLogServiceBean.createIncidentLogForStatus(incidentStatus, updated);
            } else if (ticket.getMovementId() != null &&
                    !ticket.getMovementId().equals(persisted.getMovementId().toString())) {
                MicroMovement movementFromTicket = movementClient.getMicroMovementById(UUID.fromString(ticket.getMovementId()));
                if (movementFromTicket != null && movementFromTicket.getSource().equals(MovementSourceType.MANUAL)) {
                    MicroMovement latest = movementClient.getMicroMovementById(persisted.getMovementId());
                    persisted.setStatus(StatusEnum.MANUAL_POSITION_MODE);
                    persisted.setMovementId(UUID.fromString(movementFromTicket.getGuid()));
                    Incident updated = incidentDao.update(persisted);
                    updatedIncident.fire(updated);
                    incidentLogServiceBean.createIncidentLogForManualPosition(persisted, movementFromTicket, latest);
                }
            }
        }
    }

    public Incident updateIncidentStatus(long incidentId, StatusDto statusDto) throws Exception {
        Incident persisted = incidentDao.findById(incidentId);
        String status = persisted.getStatus().name();
        persisted.setStatus(StatusEnum.valueOf(statusDto.getStatus()));
        Incident updated = incidentDao.update(persisted);
        updatedIncident.fire(updated);
        incidentLogServiceBean.createIncidentLogForStatus(status, updated);
        return updated;
    }

    public Incident findByTicketId(UUID ticketId) {
        return incidentDao.findByTicketId(ticketId);
    }
}
