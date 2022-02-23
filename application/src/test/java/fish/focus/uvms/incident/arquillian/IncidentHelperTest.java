package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.incident.TransactionalTests;
import fish.focus.uvms.incident.helper.TicketHelper;
import fish.focus.uvms.incident.model.dto.IncidentDto;
import fish.focus.uvms.incident.model.dto.KeyValuePair;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.helper.IncidentHelper;
import fish.focus.uvms.incident.service.helper.IncidentLogData;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentHelperTest extends TransactionalTests {

    @Inject
    IncidentHelper incidentHelper;

    @Test
    @OperateOnDeployment("incident")
    public void settingManualModeIntoParkedStatus() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.PARKED);
        try {
            incidentHelper.checkIfUpdateIsAllowed(incident, null);
            fail();
        }catch (Exception e){
            assertTrue(e.getMessage().contains("does not support being placed in status"));
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void givingAssetNotSendingAnExpiryDate() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.ASSET_NOT_SENDING);
        incident.setStatus(StatusEnum.INCIDENT_CREATED);
        incident.setExpiryDate(Instant.now());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertNull(incident.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void setManualModeAsResolved() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RESOLVED);
        incident.setExpiryDate(Instant.now());

       incidentHelper.setCorrectValuesForIncidentType(incident);
       assertEquals(StatusEnum.RESOLVED, incident.getStatus());
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWoOldIncident() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.MANUAL_POSITION_MODE);
        Instant now = Instant.now();
        incident.setExpiryDate(now);

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertNotEquals(now, incident.getExpiryDate());
        assertTrue(now.plus(65, ChronoUnit.MINUTES).isBefore(incident.getExpiryDate()));
        assertTrue(now.plus(66, ChronoUnit.MINUTES).isAfter(incident.getExpiryDate()));
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWithRecentMovement() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        Instant now = Instant.now();
        incident.setExpiryDate(now);
        incident.setMovementId(UUID.randomUUID());

        Instant movementTimestamp = Instant.now().minus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES - 30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        System.setProperty("MOVEMENT_MOCK_TIMESTAMP", "" + movementTimestamp.toEpochMilli());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertEquals(StatusEnum.MANUAL_POSITION_MODE, incident.getStatus());
        assertNotEquals(now, incident.getExpiryDate());
        assertEquals(movementTimestamp.plus(65, ChronoUnit.MINUTES), incident.getExpiryDate());

        System.clearProperty("MOVEMENT_MOCK_TIMESTAMP");
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkManualIncidentWithoutRecentMovement() {
        IncidentDto dto = TicketHelper.createBasicIncidentDto();
        Incident incident = incidentHelper.incidentDtoToIncident(dto);

        incident.setType(IncidentType.MANUAL_POSITION_MODE);
        incident.setStatus(StatusEnum.RECEIVING_VMS_POSITIONS);
        Instant now = Instant.now();
        incident.setExpiryDate(now);
        incident.setMovementId(UUID.randomUUID());

        Instant movementTimestamp = Instant.now().minus(ServiceConstants.MAX_DELAY_BETWEEN_MANUAL_POSITIONS_IN_MINUTES + 30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        System.setProperty("MOVEMENT_MOCK_TIMESTAMP", "" + movementTimestamp.toEpochMilli());

        incidentHelper.setCorrectValuesForIncidentType(incident);

        assertEquals(StatusEnum.MANUAL_POSITION_LATE, incident.getStatus());
        assertNotEquals(now, incident.getExpiryDate());
        assertEquals(movementTimestamp.plus(65, ChronoUnit.MINUTES), incident.getExpiryDate());

        System.clearProperty("MOVEMENT_MOCK_TIMESTAMP");
    }

    @Test
    @OperateOnDeployment("incident")
    public void createJsonStringListTest() {
        IncidentLogData data = new IncidentLogData();
        data.setExpiry(Instant.now());
        data.setUser("Instant.now().toEpochMilli()");
        data.setFrom(IncidentType.MANUAL_POSITION_MODE.name());
        data.setErrorMessage("Error message");
        String json = incidentHelper.createJsonString(data);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        assertTrue(json.contains("expiry\":"));
        assertFalse(json.contains("expiry\":\""));
        assertTrue(json.contains("user\":\"Instant.now().toEpochMilli()\""));
        assertTrue(json.contains("from\":\"MANUAL_POSITION_MODE\""));
        assertTrue(json.contains("errorMessage\":\"Error message\""));

    }
}
