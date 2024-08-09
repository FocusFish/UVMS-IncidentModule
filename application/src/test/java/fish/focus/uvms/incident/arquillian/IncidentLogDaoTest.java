package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.incident.TransactionalTests;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.service.dao.IncidentLogDao;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class IncidentLogDaoTest extends TransactionalTests {

    @Inject
    IncidentLogDao incidentLogDao;

    @Test
    @OperateOnDeployment("incident")
    public void checkIfMovementAlreadyExistsTest() {
        UUID movementId = UUID.randomUUID();
        Long incidentId = (long) (Math.random() * 10000d);

        IncidentLog incidentLog = new IncidentLog();
        incidentLog.setRelatedObjectId(movementId);
        incidentLog.setIncidentId(incidentId);
        incidentLog.setCreateDate(Instant.now());
        incidentLog.setMessage("test message");
        incidentLog.setEventType(EventTypeEnum.MANUAL_POSITION);

        incidentLogDao.save(incidentLog);

        assertTrue(incidentLogDao.checkIfMovementAlreadyExistsForIncident(incidentId, movementId));
    }
}
