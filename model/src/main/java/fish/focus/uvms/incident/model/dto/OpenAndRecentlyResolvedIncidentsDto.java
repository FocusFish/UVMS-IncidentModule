package fish.focus.uvms.incident.model.dto;

import java.util.Map;

public class OpenAndRecentlyResolvedIncidentsDto {

    Map<Long, IncidentDto> unresolved;

    Map<Long, IncidentDto> recentlyResolved;

    public Map<Long, IncidentDto> getUnresolved() {
        return unresolved;
    }

    public void setUnresolved(Map<Long, IncidentDto> unresolved) {
        this.unresolved = unresolved;
    }

    public Map<Long, IncidentDto> getRecentlyResolved() {
        return recentlyResolved;
    }

    public void setRecentlyResolved(Map<Long, IncidentDto> recentlyResolved) {
        this.recentlyResolved = recentlyResolved;
    }
}
