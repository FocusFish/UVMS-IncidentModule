package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.incident.TransactionalTests;
import fish.focus.uvms.incident.helper.TicketHelper;
import fish.focus.uvms.incident.model.dto.IncidentDto;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.bean.IncidentLogServiceBean;
import fish.focus.uvms.incident.service.bean.IncidentServiceBean;
import fish.focus.uvms.incident.service.bean.IncidentTimerBean;
import fish.focus.uvms.incident.service.dao.IncidentDao;
import fish.focus.uvms.incident.service.dao.IncidentLogDao;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentTimerBeanTest extends TransactionalTests {

    @Inject
    private IncidentServiceBean incidentService;

    @Inject
    private IncidentTimerBean timerBean;

    @Inject
    private IncidentDao incidentDao;

    @Inject
    private IncidentLogDao incidentLogDao;

    @Inject
    private IncidentLogServiceBean logServiceBean;

    @Test
    @OperateOnDeployment("incident")
    public void parkedIncidentIsOverdue() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.PARKED);
        incidentDto.setStatus(StatusEnum.PARKED);
        incidentDto.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.parkedOverdueTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.PARKED);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.OVERDUE, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.OVERDUE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_STATUS)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void seasonalIncidentIsOverdue() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.SEASONAL_FISHING);
        incidentDto.setStatus(StatusEnum.PARKED);
        incidentDto.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.parkedOverdueTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.OVERDUE, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.OVERDUE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_STATUS)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void parkedIncidentWoExpiry() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.PARKED);
        incidentDto.setStatus(StatusEnum.PARKED);
        incidentDto.setExpiryDate(null);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.parkedOverdueTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.PARKED);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.PARKED, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(1, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CREATED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void parkedIncidentIsOverdueAndAlreadyInOverdueStatus() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.PARKED);
        incidentDto.setStatus(StatusEnum.OVERDUE);
        incidentDto.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.parkedOverdueTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.PARKED);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.OVERDUE, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(1, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.OVERDUE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CREATED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void parkedIncidentExpiryDateIsInFuture() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.PARKED);
        incidentDto.setStatus(StatusEnum.PARKED);
        incidentDto.setExpiryDate(Instant.now().plus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.parkedOverdueTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.PARKED);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.PARKED, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(1, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_CREATED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void manualPositionIsLate() {
        IncidentTicketDto ticket = TicketHelper.createTicket(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        ticket.setType(IncidentType.MANUAL_POSITION_MODE);
        ticket.setStatus(StatusEnum.MANUAL_POSITION_MODE.name());
        ticket.setRuleGuid(null);

        incidentService.createIncident(ticket);
        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(UUID.fromString(ticket.getAssetId()), IncidentType.MANUAL_POSITION_MODE);
        openByAssetAndType.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));
        openByAssetAndType.setStatus(StatusEnum.MANUAL_POSITION_MODE);

        timerBean.manualPositionsTimer();

        openByAssetAndType = incidentDao.findOpenByAssetAndType(UUID.fromString(ticket.getAssetId()), IncidentType.MANUAL_POSITION_MODE);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.MANUAL_POSITION_LATE, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.MANUAL_POSITION_LATE)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.MANUAL_POSITION_LATE)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void parkedIncidentWasReceivingAis() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.PARKED);
        incidentDto.setStatus(StatusEnum.RECEIVING_AIS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.recentAisTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.PARKED);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.PARKED, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.PARKED)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_STATUS)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void seasonalFishingIncidentHasRecentAis() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.SEASONAL_FISHING);
        incidentDto.setStatus(StatusEnum.RECEIVING_AIS_POSITIONS);

        incidentDto = incidentService.createIncident(incidentDto, "Tester");
        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);

        logServiceBean.createIncidentLogForStatus(openByAssetAndType, EventTypeEnum.RECEIVED_AIS_POSITION, null, "test");

        timerBean.recentAisTimer();

        openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.SEASONAL_FISHING);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.RECEIVING_AIS_POSITIONS, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.RECEIVING_AIS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_AIS_POSITION)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void ownerIncidentIsNotReceivingVmsAnyMore() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        incidentDto.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");

        timerBean.ownerTransferRecentVmsTimer();

        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.NOT_RECEIVING_VMS_POSITIONS, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getIncidentStatus().equals(StatusEnum.NOT_RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_STATUS)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void ownerIncidentHasRecentVms() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();

        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        incidentDto.setExpiryDate(Instant.now().minus(1, ChronoUnit.MINUTES));

        incidentDto = incidentService.createIncident(incidentDto, "Tester");
        Incident openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);
        logServiceBean.createIncidentLogForStatus(openByAssetAndType, EventTypeEnum.RECEIVED_VMS_POSITION, null, "test");

        timerBean.ownerTransferRecentVmsTimer();

        openByAssetAndType = incidentDao.findOpenByAssetAndType(incidentDto.getAssetId(), IncidentType.OWNERSHIP_TRANSFER);
        assertNotNull(openByAssetAndType);
        assertEquals(StatusEnum.RECEIVING_VMS_POSITIONS, openByAssetAndType.getStatus());

        List<IncidentLog> incidentLogs = incidentLogDao.findAllByIncidentId(openByAssetAndType.getId());
        assertFalse(incidentLogs.isEmpty());
        assertEquals(2, incidentLogs.size());
        assertTrue(incidentLogs.stream().allMatch(log -> log.getIncidentStatus().equals(StatusEnum.RECEIVING_VMS_POSITIONS)));
        assertTrue(incidentLogs.stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.RECEIVED_VMS_POSITION)));
    }
}
