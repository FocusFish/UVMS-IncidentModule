package fish.focus.uvms.incident.service.message;

import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.commons.message.api.MessageConstants;
import fish.focus.uvms.commons.message.context.MappedDiagnosticContext;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.interfaces.IncidentCreate;
import fish.focus.uvms.incident.service.domain.interfaces.IncidentUpdate;
import fish.focus.uvms.incident.service.helper.IncidentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;

@Stateless
public class IncidentProducer {

    private final static Logger LOG = LoggerFactory.getLogger(IncidentProducer.class);
    @Inject
    @JMSConnectionFactory("java:/ConnectionFactory")
    JMSContext context;
    @Resource(mappedName = "java:/" + MessageConstants.EVENT_STREAM_TOPIC)
    private Destination destination;
    @Inject
    private IncidentHelper incidentHelper;

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    public void incidentCreated(@Observes @IncidentCreate Incident incident) {
        try {
            sendEvent(incident, "Incident");
        } catch (Exception e) {
            LOG.error("Error while posting incident to queue: ", e);
            throw new RuntimeException(e);
        }
    }

    public void incidentUpdated(@Observes @IncidentUpdate Incident incident) {
        try {
            sendEvent(incident, "IncidentUpdate");
        } catch (Exception e) {
            LOG.error("Error while posting incident to queue: ", e);
            throw new RuntimeException(e);
        }
    }

    private void sendEvent(Incident incident, String eventName) {
        try {
            String outgoingJson = jsonb.toJson(incidentHelper.incidentEntityToDto(incident));
            TextMessage message = this.context.createTextMessage(outgoingJson);
            message.setStringProperty(MessageConstants.EVENT_STREAM_EVENT, eventName);
            MappedDiagnosticContext.addThreadMappedDiagnosticContextToMessageProperties(message);
            context.createProducer().setDeliveryMode(1).setTimeToLive(5000L).send(destination, message);
        } catch (Exception e) {
            LOG.error("Error while sending ticket event to event stream topic: ", e);
            throw new RuntimeException(e);
        }
    }
}
