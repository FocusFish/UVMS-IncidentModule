package fish.focus.uvms.incident.mock;

import fish.focus.schema.movement.v1.MovementPoint;
import fish.focus.schema.movement.v1.MovementSourceType;
import fish.focus.uvms.commons.date.DateUtils;
import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.movement.model.dto.MovementDto;
import fish.focus.uvms.rest.security.RequiresFeature;
import fish.focus.uvms.rest.security.UnionVMSFeature;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.bind.Jsonb;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("movement/rest/internal")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
public class MovementMock {

    private Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb = new JsonBConfigurator().getContext(null);
    }

    @GET
    @Path("getMovement/{id}")
    public Response getMovement(@PathParam("id") UUID id) {
        MovementDto movement = new MovementDto();
        if (id != null) {
            movement.setId(id);
            MovementPoint point = new MovementPoint();
            point.setLatitude(123d);
            point.setLongitude(123d);
            point.setAltitude(0d);
            movement.setLocation(point);

            movement.setSource(id.getMostSignificantBits() == 0l ? MovementSourceType.NAF : MovementSourceType.MANUAL);
            Instant timestamp = DateUtils.stringToDate(System.getProperty("MOVEMENT_MOCK_TIMESTAMP", "" + Instant.now().toEpochMilli()));
            movement.setTimestamp(timestamp);
            movement.setSpeed(122f);
            movement.setHeading(123f);
        }
        String response = jsonb.toJson(movement);
        return Response.ok(response).build();
    }

    @POST
    @Path("/getMovementList")
    @RequiresFeature(UnionVMSFeature.manageInternalRest)
    public Response getMovementByIdList(List<UUID> moveIds) {
        List<MovementDto> responseList = new ArrayList<>();
        for (UUID uuid : moveIds) {
            if (uuid == null) {
                continue;
            }
            MovementDto movement = new MovementDto();
            movement.setId(uuid);
            MovementPoint point = new MovementPoint();
            point.setLatitude(123d);
            point.setLongitude(123d);
            point.setAltitude(0d);
            movement.setLocation(point);

            movement.setSource(uuid.getMostSignificantBits() == 0l ? MovementSourceType.NAF : MovementSourceType.MANUAL);
            movement.setTimestamp(Instant.now());
            movement.setSpeed((float) 122d);
            movement.setHeading((float) 123d);

            responseList.add(movement);
        }
        String response = jsonb.toJson(responseList);
        return Response.ok(response).build();
    }
}
