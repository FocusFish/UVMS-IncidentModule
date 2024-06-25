package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.incident.TransactionalTests;
import fish.focus.uvms.incident.helper.JMSHelper;
import fish.focus.uvms.incident.helper.TicketHelper;
import fish.focus.uvms.incident.model.dto.IncidentDto;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.incident.model.dto.OpenAndRecentlyResolvedIncidentsDto;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.MovementSourceType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.bean.IncidentServiceBean;
import fish.focus.uvms.incident.service.dao.IncidentDao;
import fish.focus.uvms.incident.service.dao.IncidentLogDao;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import fish.focus.uvms.incident.service.helper.IncidentHelper;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentServiceBeanTest extends TransactionalTests {

    @Inject
    private JMSHelper jmsHelper;

    @Inject
    private IncidentServiceBean incidentService;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    @Inject
    IncidentHelper incidentHelper;

    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Before
    public void resetTestSystemProperties() {
        System.clearProperty("AssetPollExceptionMessage");
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertEquals(before.getUnresolved().size() + 1, after.getUnresolved().size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsInactiveAssetTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        System.setProperty("AssetPollExceptionMessage", assetId + " is inactive");

        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertThat("No new incidents should've been created", before.getUnresolved().size(), is(equalTo(after.getUnresolved().size())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsParkedAssetTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        System.setProperty("AssetPollExceptionMessage", assetId + " is parked");

        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertThat("No new incidents should've been created", before.getUnresolved().size(), is(equalTo(after.getUnresolved().size())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsSeveralIncidentTypesTest() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        IncidentTicketDto ticket = TicketHelper.createTicket(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        ticket = TicketHelper.createTicket(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ticket.setType(IncidentType.PARKED);
        asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        OpenAndRecentlyResolvedIncidentsDto after = incidentService.getAllOpenAndRecentlyResolvedIncidents();
        assertEquals(before.getUnresolved().size() + 2, after.getUnresolved().size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void ignoreMessageWOOpenIncidentAndWoType() throws Exception {
        OpenAndRecentlyResolvedIncidentsDto before = incidentService.getAllOpenAndRecentlyResolvedIncidents();

        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

        LockSupport.parkNanos(2000000000L);

        List<Incident> incidents = incidentDao.findByAssetId(assetId);
        assertTrue(incidents.isEmpty());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentByTicketIdTest() throws Exception {
        UUID ticketId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        ticket.setId(ticketId);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

        LockSupport.parkNanos(2000000000L);

        Incident incident = incidentService.findByTicketId(ticketId);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(movementId, incident.getMovementId());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setIncidentTypeToManual() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(IncidentType.MANUAL_POSITION_MODE, incident.getType());
    }

    @Test
    @OperateOnDeployment("incident")
    public void olderManualMovementArrives() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        Instant positionTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES);
        Instant correctExpiry = positionTime.plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES);
        ticket.setPositionTime(positionTime);
        ticket.setType(null);
        String correctMoveId = UUID.randomUUID().toString();
        ticket.setMovementId(correctMoveId);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        ticket.setPositionTime(positionTime.minus(10, ChronoUnit.MINUTES));
        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertEquals(correctMoveId, incident.getMovementId().toString());
        assertEquals(correctExpiry, incident.getExpiryDate());

    }

    @Test
    @OperateOnDeployment("incident")
    public void newerManualMovementArrives() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        Instant positionTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.MINUTES);
        Instant correctExpiry = positionTime.plus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES, ChronoUnit.MINUTES);
        ticket.setPositionTime(positionTime.minus(10, ChronoUnit.MINUTES));
        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        ticket.setPositionTime(positionTime);
        ticket.setType(null);
        String correctMoveId = UUID.randomUUID().toString();
        ticket.setMovementId(correctMoveId);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertEquals(correctMoveId, incident.getMovementId().toString());
        assertEquals(correctExpiry, incident.getExpiryDate());

    }

    @Test
    @OperateOnDeployment("incident")
    public void sendManualPositionToLateManualStatus() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.MANUAL_POSITION_MODE);
        ticket.setStatus(StatusEnum.MANUAL_POSITION_LATE.name());
        incidentService.createIncident(ticket);

        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertEquals(assetId, incident.getAssetId());
        assertEquals(IncidentType.MANUAL_POSITION_MODE, incident.getType());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, incident.getStatus());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setIncidentTypeToManualCheckLog() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        incidentService.createIncident(ticket);

        ticket.setType(null);
        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.MANUAL);
        incidentService.updateIncident(ticket);

        Incident incident = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(incident);
        assertTrue(incident.getExpiryDate().isAfter(Instant.now().plus(64, ChronoUnit.MINUTES)));
        assertTrue(incident.getExpiryDate().isBefore(Instant.now().plus(66, ChronoUnit.MINUTES)));

        assertTrue(incidentLogDao.checkIfMovementAlreadyExistsForIncident(incident.getId(), UUID.fromString(ticket.getMovementId())));
        List<IncidentLog> incidentLog = incidentLogDao.findAllByIncidentId(incident.getId());

        IncidentLog manualMovementLog = incidentLog.stream().filter(log -> UUID.fromString(ticket.getMovementId()).equals(log.getRelatedObjectId())).findAny().get();
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, manualMovementLog.getIncidentStatus());
        assertEquals(EventTypeEnum.MANUAL_POSITION, manualMovementLog.getEventType());
    }


    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentTest() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        String asString = jsonb.toJson(ticket);
        jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

        LockSupport.parkNanos(2000000000L);

        Incident created = incidentDao.findOpenByAsset(assetId).get(0);
        assertNotNull(created);
        assertEquals(StatusEnum.INCIDENT_CREATED, created.getStatus());

        IncidentDto updatedDto = incidentService.updateIncidentStatus(created.getId(), StatusEnum.RESOLVED, "Test user");

        Incident updated = incidentDao.findById(created.getId());
        assertEquals(updated.getStatus(), StatusEnum.RESOLVED);

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(updated.getId());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CLOSED)));

    }

    @Test
    @OperateOnDeployment("incident")
    public void createAssetSendingDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);
        assertEquals(movementId, openByAssetAndType.getMovementId());
        assertEquals(mobTermId, openByAssetAndType.getMobileTerminalId());
        assertNull(openByAssetAndType.getTicketId());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingAisDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);

        UUID updatedMovement = new UUID(0l, movementId.getMostSignificantBits());
        ticket.setMovementId(updatedMovement.toString());
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());
        ticket.setType(null);

        incidentService.updateIncident(ticket);

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getData().contains(ticket.getRuleGuid())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndCloseAssetSendingVmsDespiteParkedTest() {
        UUID assetId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movementId, mobTermId);
        ticket.setRuleName("Asset sending ais despite parked");
        ticket.setRuleGuid("Asset sending ais despite parked");
        ticket.setType(IncidentType.PARKED);

        incidentService.createIncident(ticket);

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(assetId, IncidentType.PARKED);

        assertNotNull(openByAssetAndType);

        UUID updatedMovement = new UUID(0l, movementId.getMostSignificantBits());
        ticket.setMovementId(updatedMovement.toString());
        ticket.setMovementSource(MovementSourceType.NAF);
        ticket.setUpdated(Instant.now());
        ticket.setType(null);

        incidentService.updateIncident(ticket);
        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RESOLVED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CLOSED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingAisDespiteOwnerTransferTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.NOT_RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().allMatch(log -> log.getIncidentStatus().equals(StatusEnum.NOT_RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingVmsDespiteOwnerTransferTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void dontUpdateOwnerTransferMovementOnOldMovement() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        UUID firstMove = UUID.randomUUID();
        UUID secondMove = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementId(firstMove.toString());
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementId(secondMove.toString());
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());
        assertEquals(firstMove, updatedIncident.getMovementId());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getRelatedObjectId() != null && log.getRelatedObjectId().equals(firstMove)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getRelatedObjectId() != null && log.getRelatedObjectId().equals(secondMove)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndUpdateAssetSendingAisDespiteSeasonalFishingTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.SEASONAL_FISHING);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_AIS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createAndCloseAssetSendingVmsDespiteSeasonalFishingTest() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.SEASONAL_FISHING);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.IRIDIUM);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RESOLVED, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RESOLVED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesManualPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.MANUAL_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesAssetNotSendingShouldDoNothing() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);
        ticket.setMovementSource(MovementSourceType.MANUAL);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(1, incidentLogs.size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesAisPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesSeveralAisPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_MODE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesSeveralAisPositionWith62MinutesBetweenThem() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.AIS);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.MANUAL_POSITION_MODE, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        IncidentLog incidentLog = incidentLogs.stream().filter(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)).findAny().get();
        incidentLog.setCreateDate(Instant.now().minus(62, ChronoUnit.MINUTES));
        incidentLogDao.update(incidentLog);

        ticket.setMovementId(UUID.randomUUID().toString());
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(3, incidentLogs.size());
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualIncidentReceivesVmsPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.MANUAL_POSITION_MODE);
        incidentDto.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.MANUAL_POSITION_MODE);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.OTHER);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void ownershipTransferIncidentReceivesVmsPosition() {
        UUID movementId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);

        assertNotNull(openByAssetAndType);

        IncidentTicketDto ticket = TicketHelper.createTicket(incidentDto.getAssetId(), movementId, mobTermId);
        ticket.setMovementSource(MovementSourceType.INMARSAT_C);
        ticket.setUpdated(Instant.now());

        incidentService.updateIncident(ticket);

        Incident updatedIncident = incidentDao.findById(openByAssetAndType.getId());
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, updatedIncident.getStatus());
        assertEquals(movementId, updatedIncident.getMovementId());
    }
}
