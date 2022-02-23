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
package fish.focus.uvms.incident.service.bean;

import fish.focus.uvms.incident.model.dto.KeyValuePair;
import fish.focus.uvms.incident.model.dto.enums.EventTypeEnum;
import fish.focus.uvms.incident.model.dto.enums.IncidentType;
import fish.focus.uvms.incident.model.dto.enums.StatusEnum;
import fish.focus.uvms.incident.service.ServiceConstants;
import fish.focus.uvms.incident.service.dao.IncidentDao;
import fish.focus.uvms.incident.service.domain.entities.Incident;
import fish.focus.uvms.incident.service.domain.entities.IncidentLog;
import fish.focus.uvms.incident.service.domain.interfaces.IncidentUpdate;
import fish.focus.uvms.incident.service.helper.IncidentHelper;
import fish.focus.uvms.incident.service.helper.IncidentLogData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Startup
@Singleton
public class IncidentTimerBean {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentTimerBean.class);
    
    @Inject
    IncidentDao incidentDao;

    @Inject
    IncidentLogServiceBean incidentLogServiceBean;

    @Inject
    IncidentHelper incidentHelper;

    @Inject
    @IncidentUpdate
    private Event<Incident> updatedIncident;

    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void manualPositionsTimer() {
        try {
            List<Incident> manualPositionIncidents = incidentDao.findByStatus(StatusEnum.MANUAL_POSITION_MODE);
            for (Incident incident : manualPositionIncidents) {
                if(incident.getExpiryDate().isBefore(Instant.now())){
                    incident.setStatus(StatusEnum.MANUAL_POSITION_LATE);
                    incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.MANUAL_POSITION_LATE, null, null);
                    updatedIncident.fire(incident);
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running manualPositionsTimer. ] {}", e);
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void parkedOverdueTimer() {
        try {
            List<Incident> parkedIncidents = incidentDao.findOpenByTypes(Arrays.asList(IncidentType.SEASONAL_FISHING, IncidentType.PARKED));
            for (Incident incident : parkedIncidents) {
                if( !incident.getStatus().equals(StatusEnum.OVERDUE)
                        && incident.getExpiryDate() != null
                        && incident.getExpiryDate().isBefore(Instant.now())){

                    StatusEnum oldStatus = incident.getStatus();
                    incident.setStatus(StatusEnum.OVERDUE);

                    IncidentLogData data = new IncidentLogData();
                    data.setUser("Overdue timer");
                    data.setFrom(oldStatus.name());
                    data.setTo(incident.getStatus().name());
                    String json = incidentHelper.createJsonString(data);
                    incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.INCIDENT_STATUS, null, json);
                    updatedIncident.fire(incident);
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running parkedOverdueTimer. ] {}", e);
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void recentAisTimer() {
        try {
            List<Incident> incidentsThatReactOnRecentAIS = incidentDao.findOpenByTypes(ServiceConstants.REACT_ON_RECENT_AIS);
            for (Incident incident : incidentsThatReactOnRecentAIS) {
                if (incident.getStatus().equals(StatusEnum.RECEIVING_AIS_POSITIONS)) {
                    IncidentLog recentAisLog = incidentLogServiceBean.findLogWithTypeEntryFromTheLastHour(incident.getId(), EventTypeEnum.RECEIVED_AIS_POSITION);
                    if (recentAisLog == null) {
                        StatusEnum oldStatus = incident.getStatus();
                        incident.setStatus(incident.getType().getValidStatuses().get(0));

                        IncidentLogData data = new IncidentLogData();
                        data.setUser("Recent AIS timer");
                        data.setFrom(oldStatus.name());
                        data.setTo(incident.getStatus().name());
                        String json = incidentHelper.createJsonString(data);
                        incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.INCIDENT_STATUS, null, json);
                        updatedIncident.fire(incident);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running recentAisTimer. ] {}", e);
        }
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void ownerTransferRecentVmsTimer() {
        try {
            List<Incident> incidentsThatReactOnRecentVMS = incidentDao.findOpenByTypes(Arrays.asList(IncidentType.OWNERSHIP_TRANSFER));
            for (Incident incident : incidentsThatReactOnRecentVMS) {
                if (incident.getStatus().equals(StatusEnum.RECEIVING_VMS_POSITIONS)) {
                    IncidentLog recentVmsLog = incidentLogServiceBean.findLogWithTypeEntryFromTheLastDay(incident.getId(), EventTypeEnum.RECEIVED_VMS_POSITION);
                    if (recentVmsLog == null) {
                        StatusEnum oldStatus = incident.getStatus();
                        incident.setStatus(StatusEnum.NOT_RECEIVING_VMS_POSITIONS);

                        IncidentLogData data = new IncidentLogData();
                        data.setUser("Recent VMS timer");
                        data.setFrom(oldStatus.name());
                        data.setTo(incident.getStatus().name());
                        String json = incidentHelper.createJsonString(data);
                        incidentLogServiceBean.createIncidentLogForStatus(incident, EventTypeEnum.INCIDENT_STATUS, null, json);
                        updatedIncident.fire(incident);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[ Error when running ownerTransferRecentVmsTimer. ] {}", e);
        }
    }

}
