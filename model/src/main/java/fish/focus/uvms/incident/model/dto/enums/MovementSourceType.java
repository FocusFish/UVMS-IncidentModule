package fish.focus.uvms.incident.model.dto.enums;

public enum MovementSourceType {
    INMARSAT_C,
    AIS,
    IRIDIUM,
    MANUAL,
    OTHER,
    NAF,
    FLUX;

    MovementSourceType() {
    }

    public String value() {
        return this.name();
    }

    public static MovementSourceType fromValue(String v) {
        return valueOf(v);
    }
}