package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.incident.TransactionalTests;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.service.bean.IncidentLogServiceBean;
import fish.focus.uvms.incident.service.dao.IncidentLogDao;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Arquillian.class)
public class IncidentLogServiceBeanTest extends TransactionalTests {

    @Inject
    IncidentLogDao incidentLogDao;

    @Inject
    IncidentLogServiceBean incidentLogServiceBean;

    @Test
    @OperateOnDeployment("incident")
    public void checkIfLogHasTypeEntryFromTheLastHour() {
        UUID movementId = UUID.randomUUID();
        Long incidentId = (long) (Math.random() * 100000d);

        IncidentLog incidentLog = new IncidentLog();
        incidentLog.setRelatedObjectId(movementId);
        incidentLog.setIncidentId(incidentId);
        incidentLog.setCreateDate(Instant.now());
        incidentLog.setMessage("test message");
        incidentLog.setEventType(EventTypeEnum.MANUAL_POSITION);

        incidentLogDao.save(incidentLog);

        assertNotNull(incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(incidentId, EventTypeEnum.MANUAL_POSITION));
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkIfLogHasTypeEntryFromTheLastHourWithOlderLog() {
        UUID movementId = UUID.randomUUID();
        Long incidentId = (long) (Math.random() * 100000d);

        IncidentLog incidentLog = new IncidentLog();
        incidentLog.setRelatedObjectId(movementId);
        incidentLog.setIncidentId(incidentId);
        incidentLog.setCreateDate(Instant.now().minus(61, ChronoUnit.HOURS));
        incidentLog.setMessage("test message");
        incidentLog.setEventType(EventTypeEnum.MANUAL_POSITION);

        incidentLogDao.save(incidentLog);

        assertNull(incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(incidentId, EventTypeEnum.MANUAL_POSITION));
    }

    @Test
    @OperateOnDeployment("incident")
    public void checkIfLogHasTypeEntryFromTheLastHourWithOtherLogType() {
        UUID movementId = UUID.randomUUID();
        Long incidentId = (long) (Math.random() * 100000d);

        IncidentLog incidentLog = new IncidentLog();
        incidentLog.setRelatedObjectId(movementId);
        incidentLog.setIncidentId(incidentId);
        incidentLog.setCreateDate(Instant.now());
        incidentLog.setMessage("test message");
        incidentLog.setEventType(EventTypeEnum.INCIDENT_CREATED);

        incidentLogDao.save(incidentLog);

        assertNull(incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(incidentId, EventTypeEnum.MANUAL_POSITION));
    }
}
