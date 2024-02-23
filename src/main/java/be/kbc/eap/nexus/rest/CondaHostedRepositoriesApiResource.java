package be.kbc.eap.nexus.rest;

import be.kbc.eap.nexus.internal.CondaFormat;
import be.kbc.eap.nexus.internal.api.CondaHostedApiRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.sonatype.nexus.repository.rest.api.AbstractHostedRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.FormatAndType;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.validation.Validate;

import static org.sonatype.nexus.rest.ApiDocConstants.*;

@Named
@Singleton
@Path(CondaHostedRepositoriesApiResource.RESOURCE_URI)
@Api(value = API_REPOSITORY_MANAGEMENT)
public class CondaHostedRepositoriesApiResource extends AbstractHostedRepositoriesApiResource<CondaHostedRepositoryApiRequest> {
    static final String RESOURCE_URI = "/v1/repositories/conda/hosted";

    @ApiOperation("Create conda hosted repository")
    @ApiResponses({@ApiResponse(code = 201, message = REPOSITORY_CREATED), @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED), @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS), @ApiResponse(code = 405, message = DISABLED_IN_HIGH_AVAILABILITY)})
    @POST
    @RequiresAuthentication
    @Validate
    public Response createRepository(CondaHostedRepositoryApiRequest request) {
        return super.createRepository(request);
    }

    @ApiOperation("Update conda hosted repository")
    @ApiResponses({@ApiResponse(code = 204, message = REPOSITORY_UPDATED), @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED), @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)})
    @PUT
    @Path("/{repositoryName}")
    @RequiresAuthentication
    @Validate
    public Response updateRepository(CondaHostedRepositoryApiRequest request, @ApiParam("Name of the repository to update") @PathParam("repositoryName") String repositoryName) {
        return super.updateRepository(request, repositoryName);
    }

    @GET
    @Path("/{repositoryName}")
    @RequiresAuthentication
    @Validate
    @ApiOperation(value = "Get conda hosted repository", response = CondaHostedApiRepository.class)
    @Override
    public AbstractApiRepository getRepository(@ApiParam(hidden = true) @BeanParam final FormatAndType formatAndType,
                                               @PathParam("repositoryName") final String repositoryName) {

        return super.getRepository(formatAndType, repositoryName);
    }

    public boolean isApiEnabled() {
        return this.highAvailabilitySupportChecker.isSupported(CondaFormat.NAME);
    }
}
