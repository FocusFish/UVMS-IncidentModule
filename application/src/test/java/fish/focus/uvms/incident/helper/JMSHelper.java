/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.incident.helper;

import fish.focus.uvms.commons.message.api.MessageConstants;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Stateless
public class JMSHelper {

    @Inject
    public JMSContext context;

    public final String QUEUE_NAME = "IncidentEvent";
    public final String EVENT_STREAM = "jms.topic.EventStream";

    @Resource(mappedName = "java:/" + MessageConstants.QUEUE_INCIDENT)
    private Destination destination;

    public void clearQueue(String queue) throws Exception {
        try (Connection connection = getConnectionFactory().createConnection("test", "test")) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue responseQueue = session.createQueue(queue);
            MessageConsumer consumer = session.createConsumer(responseQueue);
            while (consumer.receive(10L) != null) ;
        }
    }

    public void sendMessageToIncidentQueue(String message, String eventName) throws JMSException {
        TextMessage textMessage = context.createTextMessage(message);
        textMessage.setStringProperty("eventName", eventName);
        context.createProducer().send(destination, textMessage);
    }

    private ConnectionFactory getConnectionFactory() {
        Map<String, Object> params = new HashMap<>();
        params.put("host", "localhost");
        params.put("port", 5445);
        TransportConfiguration transportConfiguration =
                new TransportConfiguration(NettyConnectorFactory.class.getName(), params);
        return ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
    }
}
