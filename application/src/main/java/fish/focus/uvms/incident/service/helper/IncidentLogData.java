package fish.focus.uvms.incident.service.helper;

import java.time.Instant;

public class IncidentLogData {

    String user;

    Instant expiry;

    String from;

    String to;

    String errorMessage;


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public void setExpiry(Instant expiry) {
        this.expiry = expiry;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
