package fish.focus.uvms.incident.service.bean;

import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.asset.client.model.*;
import fish.focus.uvms.commons.date.DateUtils;
import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.incident.model.dto.IncidentTicketDto;
import fish.focus.uvms.rest.security.InternalRestTokenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Stateless
public class AssetCommunicationBean {

    private static final Logger LOG = LoggerFactory.getLogger(AssetCommunicationBean.class);
    private static final Pattern ASSET_IS_ACTIVE_OR_PARKED = Pattern.compile(".*is (inactive|parked)");

    @Resource(name = "java:global/asset_endpoint")
    private String assetEndpoint;

    @Inject
    private InternalRestTokenHandler tokenHandler;

    @Inject
    private AssetClient assetClient;

    public Optional<String> createPollInternal(IncidentTicketDto dto) {
        try {
            AssetDTO assetById = assetClient.getAssetById(AssetIdentifier.GUID, dto.getAssetId());
            String username = "Triggered by asset not sending";
            String comment = "This poll was triggered by asset not sending on: " + DateUtils.dateToHumanReadableString(Instant.now())
                    + " on Asset: " + assetById.getName() + " (" + assetById.getIrcs() + ")";

            SimpleCreatePoll createPoll = new SimpleCreatePoll();
            createPoll.setComment(comment);
            createPoll.setPollType(PollType.AUTOMATIC_POLL);

            CreatePollResultDto createPollResultDto;
            try (Response createdPollResponse = getWebTarget()
                    .path("internal/createPollForAsset")
                    .path(dto.getAssetId())
                    .queryParam("username", username)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, tokenHandler.createAndFetchToken("user"))
                    .post(Entity.json(createPoll), Response.class)) {

                if (createdPollResponse.getStatus() != 200) {
                    String exceptionMessage = stripExceptionFromResponseString(createdPollResponse.readEntity(String.class));
                    if (ASSET_IS_ACTIVE_OR_PARKED.matcher(exceptionMessage).matches()) {
                        return Optional.empty();
                    }
                    return Optional.of(exceptionMessage);
                }

                createPollResultDto = createdPollResponse.readEntity(CreatePollResultDto.class);
            }

            if (!createPollResultDto.isUnsentPoll()) {
                return Optional.of(createPollResultDto.getSentPolls().get(0));
            } else {
                return Optional.of(createPollResultDto.getUnsentPolls().get(0));
            }
        } catch (Exception e) {
            LOG.error("Error while sending rule-triggered poll: ", e);
            return Optional.of("NOK " + e.getMessage());
        }
    }

    public void setAssetParkedStatus(UUID assetId, boolean parked) {
        AssetDTO asset = assetClient.getAssetById(AssetIdentifier.GUID, assetId.toString());
        asset.setParked(parked);
        asset.setComment("Changing parked variable to " + parked);
        asset.setUpdatedBy("Incident module");
        AssetBO assetBO = new AssetBO();
        assetBO.setAsset(asset);
        assetClient.upsertAsset(assetBO);
    }

    private WebTarget getWebTarget() {
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build().register(JsonBConfigurator.class);
        return client.target(assetEndpoint);
    }

    private String stripExceptionFromResponseString(String errorString) {
        if (!errorString.contains("Exception")) {
            return errorString;
        }
        int exceptionEndIndex = errorString.indexOf("Exception:") + 10;
        return errorString.length() > exceptionEndIndex
                ? errorString.substring(exceptionEndIndex).trim() : "";
    }
}
