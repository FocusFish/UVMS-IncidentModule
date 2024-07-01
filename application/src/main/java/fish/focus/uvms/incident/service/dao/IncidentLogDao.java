package fish.focus.uvms.incident.service.dao;

import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentLogDao {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentLogDao.class);

    @PersistenceContext
    private EntityManager em;


    public void save(IncidentLog entity) {
        this.em.persist(entity);
        LOG.info("New Incident Log created with ID: " + entity.getId());
    }

    public void update(IncidentLog log) {
        em.merge(log);
    }

    public List<IncidentLog> findAllByIncidentId(long incidentId) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.FIND_ALL_BY_INCIDENT_ID, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        return query.getResultList();
    }

    public boolean checkIfMovementAlreadyExistsForIncident(long incidentId, UUID movementId) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.CHECK_IF_INCIDENT_ALREADY_HAS_MOVEMENT, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        query.setParameter("movementId", movementId);
        return !query.getResultList().isEmpty();
    }

    public List<IncidentLog> findLogWithEventTypeAfter(long incidentId, EventTypeEnum eventType, Instant date) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.FIND_LOG_WITH_EVENT_TYPE_AFTER, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        query.setParameter("eventType", eventType);
        query.setParameter("date", date);
        return query.getResultList();
    }

    public List<IncidentLog> findAllByIncidentId(List<Long> incidentId) {
        TypedQuery<IncidentLog> query = em.createNamedQuery(IncidentLog.FIND_ALL_BY_INCIDENT_ID, IncidentLog.class);
        query.setParameter("incidentId", incidentId);
        return query.getResultList();
    }
}
