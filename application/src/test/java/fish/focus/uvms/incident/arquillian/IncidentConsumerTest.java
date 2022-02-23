package fish.focus.uvms.incident.arquillian;

import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.incident.BuildIncidentTestDeployment;
import fish.focus.uvms.spatial.model.schemas.AreaType;
import fish.focus.uvms.incident.helper.JMSHelper;
import fish.focus.uvms.incident.helper.TicketHelper;
import fish.focus.uvms.incident.helper.TopicListener;
import fish.focus.uvms.incident.model.dto.IncidentDto;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.RiskLevel;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class IncidentConsumerTest extends BuildIncidentTestDeployment {

    @Inject
    private JMSHelper jmsHelper;


    private Jsonb jsonb;

    {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Before
    public void clearExchangeQueue() throws Exception {
        jmsHelper.clearQueue(jmsHelper.QUEUE_NAME);
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueue() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        IncidentTicketDto ticket = TicketHelper.createTicket( assetId, movId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString, "IncidentUpdate");

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = jsonb.fromJson(text, IncidentDto.class);
            assertEquals(assetId, incident.getAssetId());
        }
    }

    @Test
    @OperateOnDeployment("incident")
    public void consumeIncidentQueueWoTicket() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        System.setProperty("SPATIAL_AREA_TYPE", AreaType.EEZ.value());
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = jsonb.fromJson(text, IncidentDto.class);
            assertNotNull(incident.getId());
            assertEquals(assetId, incident.getAssetId());
            assertEquals(RiskLevel.HIGH, incident.getRisk());
        }
        System.clearProperty("SPATIAL_AREA_TYPE");
    }

    @Test
    @OperateOnDeployment("incident")
    public void assetNotSendingWithAssetInPortArea() throws Exception {
        UUID assetId = UUID.randomUUID();
        UUID movId = UUID.randomUUID();
        UUID mobTermId = UUID.randomUUID();
        System.setProperty("SPATIAL_AREA_TYPE", AreaType.PORTAREA.value());
        IncidentTicketDto ticket = TicketHelper.createTicket(assetId, movId, mobTermId);
        ticket.setType(IncidentType.ASSET_NOT_SENDING);

        try (TopicListener listener = new TopicListener(jmsHelper.EVENT_STREAM, "")) {
            String asString = jsonb.toJson(ticket);
            jmsHelper.sendMessageToIncidentQueue(asString, "Incident");

            Message message = listener.listenOnEventBus();
            TextMessage textMessage = (TextMessage) message;

            String text = textMessage.getText();
            IncidentDto incident = jsonb.fromJson(text, IncidentDto.class);
            assertNotNull(incident.getId());
            assertEquals(assetId, incident.getAssetId());
            assertEquals(RiskLevel.MEDIUM, incident.getRisk());
        }
        System.clearProperty("SPATIAL_AREA_TYPE");
    }
}
