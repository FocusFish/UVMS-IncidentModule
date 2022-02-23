package fish.focus.uvms.incident.service.message;

import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.commons.message.api.MessageConstants;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.incident.service.bean.IncidentServiceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_TYPE_STR, propertyValue = MessageConstants.DESTINATION_TYPE_QUEUE),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_LOOKUP_STR, propertyValue = MessageConstants.QUEUE_INCIDENT)
})
public class IncidentConsumer implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(IncidentConsumer.class);

    @Inject
    private IncidentServiceBean incidentServiceBean;

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage tm = (TextMessage) message;
            String json = tm.getBody(String.class);
            IncidentTicketDto ticket = jsonb.fromJson(json, IncidentTicketDto.class);
            String eventType = message.getStringProperty("eventName");
            switch (eventType) {
                case "Incident":
                    incidentServiceBean.createIncident(ticket);
                    break;
                case "IncidentUpdate":
                    incidentServiceBean.updateIncident(ticket);
                    break;
            }
        } catch (Exception e) {
            LOG.error("Error while reading from Incident Queue", e);
            throw new EJBException(e);
        }
    }
}
