package fish.focus.uvms.incident.rest;

import fish.focus.uvms.incident.BuildIncidentTestDeployment;
import fish.focus.uvms.incident.helper.JMSHelper;
import fish.focus.uvms.incident.helper.TicketHelper;
import fish.focus.uvms.incident.model.dto.*;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.rest.filters.AppError;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class IncidentRestResourceTest extends BuildIncidentTestDeployment {

    private static Jsonb jsonb;

    @Inject
    private JMSHelper jmsHelper;

    @Test
    @OperateOnDeployment("incident")
    public void getValidStatusForTypes() {
        Map<IncidentType, List<StatusEnum>> response = getWebTarget()
                .path("incident/validStatusForTypes")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<IncidentType, List<StatusEnum>>>() {
                });
        assertNotNull(response);
        for (IncidentType value : IncidentType.values()) {
            assertTrue(value.name(), value.getValidStatuses().equals(response.get(value)));
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentTypes() {
        List<IncidentType> response = getWebTarget()
                .path("incident/incidentTypes")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<List<IncidentType>>() {
                });
        assertNotNull(response);
        assertEquals(Arrays.asList(IncidentType.values()), response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        Instant expiryDate = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        incidentDto.setExpiryDate(expiryDate);

        IncidentDto createdIncident = createIncident(incidentDto);

        assertNotNull(createdIncident.getId());
        assertEquals(incidentDto.getAssetId(), createdIncident.getAssetId());
        assertEquals(incidentDto.getType(), createdIncident.getType());
        assertEquals(expiryDate, createdIncident.getExpiryDate());
        assertNotNull(createdIncident.getUpdateDate());
        assertNotNull(createdIncident.getCreateDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentWithInvalidStatus() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.PARKED);

        AppError error = getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incidentDto), AppError.class);

        assertTrue(error.description, error.description.contains("does not support being placed in status"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentThatShouldNotHaveAnEndDateWithOne() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.INCIDENT_CREATED);
        incidentDto.setExpiryDate(Instant.now());

        IncidentDto created = createIncident(incidentDto);
        assertNull(created.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void createIncidentLogCreatedTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);
        assertNotNull(createdIncident);

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(1, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getData().contains(BuildIncidentTestDeployment.USER_NAME)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void addNoteCreatedEventToIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);

        EventCreationDto creationDto = new EventCreationDto(EventTypeEnum.NOTE_CREATED, UUID.randomUUID());

        Response response = getWebTarget()
                .path("incident/addEventToIncident")
                .path(createdIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(creationDto), Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(2, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getEventType().equals(creationDto.getEventType())));
        assertTrue(logs.values().stream()
                .anyMatch(log -> creationDto.getRelatedObjectId().equals(log.getRelatedObjectId())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void addPollCreatedEventToIncidentTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        IncidentDto createdIncident = createIncident(incidentDto);

        EventCreationDto creationDto = new EventCreationDto(EventTypeEnum.POLL_CREATED, UUID.randomUUID());

        Response response = getWebTarget()
                .path("incident/addEventToIncident")
                .path(createdIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(creationDto), Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> logs = getIncidentLogForIncident(createdIncident);

        assertEquals(2, logs.size());
        assertTrue(logs.values().stream()
                .anyMatch(log -> log.getEventType().equals(creationDto.getEventType())));
        assertTrue(logs.values().stream()
                .anyMatch(log -> creationDto.getRelatedObjectId().equals(log.getRelatedObjectId())));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentTypeTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(null);
        IncidentDto createdIncident = createIncident(incidentDto);

        assertEquals(StatusEnum.INCIDENT_CREATED, createdIncident.getStatus());

        UpdateIncidentDto updateDto = new UpdateIncidentDto();
        Instant expiryDate = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        updateDto.setExpiryDate(expiryDate);
        updateDto.setType(IncidentType.PARKED);
        updateDto.setIncidentId(createdIncident.getId());

        IncidentDto updatedIncident = getWebTarget()
                .path("incident")
                .path("updateType")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(updateDto), IncidentDto.class);

        assertNotNull(updatedIncident.getId());
        assertEquals(incidentDto.getAssetId(), updatedIncident.getAssetId());
        assertEquals(IncidentType.PARKED, updatedIncident.getType());
        assertEquals(StatusEnum.PARKED, updatedIncident.getStatus());
        assertEquals(expiryDate, updatedIncident.getExpiryDate());
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentToAnInvalidStatus() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.ATTEMPTED_CONTACT);
        IncidentDto createdIncident = createIncident(incidentDto);

        UpdateIncidentDto updateDto = new UpdateIncidentDto();
        updateDto.setStatus(StatusEnum.PARKED);
        updateDto.setIncidentId(createdIncident.getId());

        AppError error = getWebTarget()
                .path("incident")
                .path("updateStatus")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(updateDto), AppError.class);

        assertNotNull(error);
        assertTrue(error.description, error.description.contains("does not support being placed in status"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentInvalidTypeForExpiryDate() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.ASSET_NOT_SENDING);
        incidentDto.setStatus(StatusEnum.ATTEMPTED_CONTACT);
        IncidentDto createdIncident = createIncident(incidentDto);

        UpdateIncidentDto updateDto = new UpdateIncidentDto();
        updateDto.setExpiryDate(Instant.now());
        updateDto.setIncidentId(createdIncident.getId());

        AppError error = getWebTarget()
                .path("incident")
                .path("updateExpiry")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(updateDto), AppError.class);

        assertNotNull(error);
        assertTrue(error.description, error.description.contains("does not support having an expiry date"));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentLogCreatedTest() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);
        IncidentDto createdIncident = createIncident(incidentDto);

        Instant expiryDate = Instant.now().plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);

        UpdateIncidentDto updateDto = new UpdateIncidentDto();
        updateDto.setExpiryDate(expiryDate);
        updateDto.setType(IncidentType.PARKED);
        updateDto.setIncidentId(createdIncident.getId());
        IncidentDto updatedIncident = getWebTarget()
                .path("incident")
                .path("updateType")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(updateDto), IncidentDto.class);

        Map<Long, IncidentLogDto> logs = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path(updatedIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<Long, IncidentLogDto>>() {
                });

        assertEquals(3, logs.size());
        assertTrue(logs.values().stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_TYPE)));
        assertTrue(logs.values().stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.EXPIRY_UPDATED)));
    }

    @Test
    @OperateOnDeployment("incident")
    public void updateIncidentToParkedWithExpiryInThePast() {
        IncidentDto incidentDto = TicketHelper.createBasicIncidentDto();
        incidentDto.setType(IncidentType.OWNERSHIP_TRANSFER);
        incidentDto.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);
        IncidentDto createdIncident = createIncident(incidentDto);

        Instant expiryDate = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);

        UpdateIncidentDto updateDto = new UpdateIncidentDto();
        updateDto.setExpiryDate(expiryDate);
        updateDto.setType(IncidentType.PARKED);
        updateDto.setIncidentId(createdIncident.getId());
        IncidentDto updatedIncident = getWebTarget()
                .path("incident")
                .path("updateType")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(updateDto), IncidentDto.class);

        assertEquals(StatusEnum.OVERDUE, updatedIncident.getStatus());

        Map<Long, IncidentLogDto> logs = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path(updatedIncident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<Long, IncidentLogDto>>() {
                });

        assertEquals(4, logs.size());
        assertTrue(logs.values().stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.INCIDENT_TYPE)));
        assertTrue(logs.values().stream().anyMatch(log -> log.getEventType().equals(EventTypeEnum.EXPIRY_UPDATED)));
    }


    @Test
    @OperateOnDeployment("incident")
    public void openIncidentsTest() {
        OpenAndRecentlyResolvedIncidentsDto response = getWebTarget()
                .path("incident/allOpenIncidents")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(OpenAndRecentlyResolvedIncidentsDto.class);
        assertNotNull(response);
    }

    @Test
    @OperateOnDeployment("incident")
    public void noAuthTest() {
        Response response = getWebTarget()
                .path("incident/allOpenIncidents")
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);
        assertNotNull(response);
        assertEquals(403, response.getStatus());
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogForIncidentTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogForIncident")
                .path("1")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentLogDto>>() {
        });
        assertNotNull(responseLogs);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentLogsForAssetTest() {
        Response response = getWebTarget()
                .path("incident/incidentLogsForAssetId")
                .path(UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentLogDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentLogDto>>() {
        });
        assertNotNull(responseLogs);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getIncidentsForAssetTest() {
        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(UUID.randomUUID().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);
        assertEquals(200, response.getStatus());

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {
        });
        assertNotNull(responseLogs);
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllIncidentsForAsset() {
        IncidentDto incidentDto1 = TicketHelper.createBasicIncidentDto();
        UUID assetId = incidentDto1.getAssetId();
        IncidentDto createdIncident1 = createIncident(incidentDto1);

        UpdateIncidentDto update = new UpdateIncidentDto();
        update.setStatus(StatusEnum.RESOLVED);
        update.setIncidentId(createdIncident1.getId());
        IncidentDto closedIncident = updateIncidentStatus(update);

        IncidentDto incidentDto2 = TicketHelper.createBasicIncidentDto();
        incidentDto2.setAssetId(assetId);
        IncidentDto createdIncident2 = createIncident(incidentDto2);

        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(assetId.toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {
        });
        assertNotNull(responseLogs);
        assertEquals(2, responseLogs.size());
        assertTrue(responseLogs.containsKey(closedIncident.getId()));
        assertTrue(responseLogs.containsKey(createdIncident2.getId()));
    }

    @Test
    @OperateOnDeployment("incident")
    public void getAllOpenIncidentsForAsset() {
        IncidentDto incidentDto1 = TicketHelper.createBasicIncidentDto();
        UUID assetId = incidentDto1.getAssetId();
        IncidentDto createdIncident1 = createIncident(incidentDto1);

        UpdateIncidentDto update = new UpdateIncidentDto();
        update.setStatus(StatusEnum.RESOLVED);
        update.setIncidentId(createdIncident1.getId());
        IncidentDto closedIncident = updateIncidentStatus(update);

        IncidentDto incidentDto2 = TicketHelper.createBasicIncidentDto();
        incidentDto2.setAssetId(assetId);
        IncidentDto createdIncident2 = createIncident(incidentDto2);

        Response response = getWebTarget()
                .path("incident/incidentsForAssetId")
                .path(assetId.toString())
                .queryParam("onlyOpen", "true")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(Response.class);

        Map<Long, IncidentDto> responseLogs = response.readEntity(new GenericType<Map<Long, IncidentDto>>() {
        });
        assertNotNull(responseLogs);
        assertEquals(1, responseLogs.size());
        assertFalse(responseLogs.containsKey(closedIncident.getId()));
        assertTrue(responseLogs.containsKey(createdIncident2.getId()));
    }

    private IncidentDto createIncident(IncidentDto incident) {
        return getWebTarget()
                .path("incident")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .post(Entity.json(incident), IncidentDto.class);
    }

    private IncidentDto updateIncidentStatus(UpdateIncidentDto update) {
        return getWebTarget()
                .path("incident")
                .path("updateStatus")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .put(Entity.json(update), IncidentDto.class);
    }

    private Map<Long, IncidentLogDto> getIncidentLogForIncident(IncidentDto incident) {
        return getWebTarget()
                .path("incident/incidentLogForIncident")
                .path(incident.getId().toString())
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .get(new GenericType<Map<Long, IncidentLogDto>>() {
                });
    }
}
