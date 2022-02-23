package fish.focus.uvms.incident.rest.filters;

public class AppError {

    public Integer code;
    public String  description;

    public AppError() {
        this.code = Integer.MIN_VALUE;
        this.description = "";

    }


    public AppError(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
