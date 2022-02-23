package fish.focus.uvms.incident.service.dao;

import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Stateless
public class IncidentDao {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentDao.class);

    @PersistenceContext
    private EntityManager em;


    public Incident save(Incident entity) {
        this.em.persist(entity);
        LOG.info("New Incident created with ID: " + entity.getId());
        return entity;
    }
    public Incident findById(long id) {
        Incident incident = this.em.find(Incident.class, id);
        if (incident == null) {
            throw new EntityNotFoundException("Can't find Incident for Id: " + id);
        }
        return incident;
    }

    public List<Incident> findUnresolvedIncidents(IncidentType type) {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_ALL_EXCLUDE_STATUS, Incident.class);
        query.setParameter("type", type);
        query.setParameter("status", ServiceConstants.RESOLVED_STATUS_LIST);
        return query.getResultList();
    }

    public List<Incident> findByStatusAndUpdatedSince12Hours(List<IncidentType> type) {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_STATUS_AND_UPDATED_SINCE, Incident.class);
        query.setParameter("type", type);
        query.setParameter("status", ServiceConstants.RESOLVED_STATUS_LIST);
        query.setParameter("updatedSince", Instant.now().minus(12, ChronoUnit.HOURS));
        return query.getResultList();
    }

    public List<Incident> findByAssetId(UUID assetId) {
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_ASSET_ID, Incident.class);
        query.setParameter("assetId", assetId);
        return query.getResultList();
    }

    public Incident findByTicketId(UUID ticketId) {
        try {
            TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_TICKET_ID, Incident.class);
            query.setParameter("ticketId", ticketId);
            return query.getSingleResult();
        } catch (Exception e) {
            LOG.warn("No Ticket found to be updated. Ticket ID: {}", ticketId);
            return null;
        }
    }

    public Incident findOpenByAssetAndType(UUID assetId, IncidentType type){
        try {

            TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_ASSET_TYPE_AND_EXCLUDE_STATUS, Incident.class);
            query.setParameter("type", type);
            query.setParameter("status", ServiceConstants.RESOLVED_STATUS_LIST);
            query.setParameter("assetId", assetId);
            return query.getSingleResult();
        }catch (NoResultException e) {
            LOG.debug("No open incident found for asset {} and type {}", assetId, type);
            return null;
        }
    }

    public List<Incident> findOpenByTypes(List<IncidentType> type){
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_TYPES_AND_EXCLUDE_STATUS, Incident.class);
        query.setParameter("type", type);
        query.setParameter("status", ServiceConstants.RESOLVED_STATUS_LIST);
        return query.getResultList();
    }

    public List<Incident> findOpenByAsset(UUID assetId){
        try {
            TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_ASSET_AND_EXCLUDE_STATUS, Incident.class);
            query.setParameter("status", ServiceConstants.RESOLVED_STATUS_LIST);
            query.setParameter("assetId", assetId);
            return query.getResultList();
        }catch (NoResultException e) {
            LOG.debug("No open incident found for asset {}", assetId);
            return null;
        }
    }

    public List<Incident> findByStatus(StatusEnum status){
        TypedQuery<Incident> query = em.createNamedQuery(Incident.FIND_BY_STATUS, Incident.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public Incident update(Incident entity) {
        entity.setUpdateDate(Instant.now());
        return em.merge(entity);
    }
}
