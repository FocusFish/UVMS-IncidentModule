package fish.focus.uvms.incident.mock;

import fish.focus.uvms.asset.client.model.AssetBO;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.asset.client.model.CreatePollResultDto;
import fish.focus.uvms.asset.client.model.SimpleCreatePoll;
import fish.focus.uvms.rest.security.RequiresFeature;
import fish.focus.uvms.rest.security.UnionVMSFeature;

import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Path("asset/rest/internal")
@Consumes(value = {MediaType.APPLICATION_JSON})
@Produces(value = {MediaType.APPLICATION_JSON})
@Stateless
public class AssetMock {

    @GET
    @Path("asset/guid/{id}")
    public Response getMicroMovement(@PathParam("id") UUID id) {
        System.setProperty("GET_ASSET_REACHED", "true");
        AssetDTO asset = new AssetDTO();
        asset.setId(id);
        asset.setName("Asset");
        asset.setIrcs("Ircs");

        return Response.ok(asset).build();
    }

    @POST
    @Path("/createPollForAsset/{id}")
    public Response createPoll(@PathParam("id") String assetId, @QueryParam("username") String username, SimpleCreatePoll createPoll) {
        System.setProperty("AssetPollEndpointReached", "True");

        String assetPollExceptionMessage = System.getProperty("AssetPollExceptionMessage");
        if (assetPollExceptionMessage != null && !assetPollExceptionMessage.isEmpty()) {
            return Response.status(500).entity(assetPollExceptionMessage).build();
        }

        CreatePollResultDto result = new CreatePollResultDto();
        result.setUnsentPoll(false);
        result.setUnsentPolls(Collections.emptyList());
        List<String> sentPolls = List.of(UUID.randomUUID().toString());
        result.setSentPolls(sentPolls);

        return Response.ok().entity(result).build();
    }

    @POST
    @Path("/asset")
    @RequiresFeature(UnionVMSFeature.manageInternalRest)
    public Response upsertAsset(AssetBO assetBo) {
        System.setProperty("UPDATE_ASSET_REACHED", "true");
        return Response.ok(assetBo).build();
    }
}
