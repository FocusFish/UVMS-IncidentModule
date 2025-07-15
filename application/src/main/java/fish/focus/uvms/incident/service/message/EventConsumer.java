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
package fish.focus.uvms.incident.service.message;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.bean.IncidentServiceBean;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import java.util.List;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/topic/EventStream"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "incident"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "incident"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "event='Updated Asset'")
})
public class EventConsumer implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(EventConsumer.class);

    @Inject
    private IncidentServiceBean incidentService;

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            AssetDTO asset = jsonb.fromJson(textMessage.getText(), AssetDTO.class);
            if (Boolean.FALSE.equals(asset.getActive())) {
                List<Incident> incidents = incidentService.findOpenByAssetId(asset.getId());
                for (Incident incident : incidents) {
                    LOG.info("Asset {} have been inactivated, resolving incident {}", asset.getId(), incident.getId());
                    incidentService.updateIncidentStatus(incident.getId(), StatusEnum.RESOLVED, "UVMS");
                }
            }
        } catch (JMSException e) {
            LOG.error("Could not handle event message", e);
        }
    }
}
