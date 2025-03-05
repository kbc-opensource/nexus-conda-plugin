package be.kbc.eap.nexus.rest;

import be.kbc.eap.nexus.CondaFormat;
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
import org.sonatype.nexus.repository.rest.api.AbstractProxyRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.FormatAndType;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;
import org.sonatype.nexus.validation.Validate;

import static org.sonatype.nexus.rest.ApiDocConstants.*;

@Named
@Singleton
@Path(CondaProxyRepositoriesApiResource.RESOURCE_URI)
@Api(value = API_REPOSITORY_MANAGEMENT)
public class CondaProxyRepositoriesApiResource extends AbstractProxyRepositoriesApiResource<CondaProxyRepositoryApiRequest> {
    static final String RESOURCE_URI = "/v1/repositories/conda/proxy";

    @ApiOperation("Create conda proxy repository")
    @ApiResponses({@ApiResponse(code = 201, message = REPOSITORY_CREATED), @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED), @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS), @ApiResponse(code = 405, message = DISABLED_IN_HIGH_AVAILABILITY)})
    @POST
    @RequiresAuthentication
    @Validate
    public Response createRepository(CondaProxyRepositoryApiRequest request) {
        return super.createRepository(request);
    }

    @ApiOperation("Update conda proxy repository")
    @ApiResponses({@ApiResponse(code = 204, message = REPOSITORY_UPDATED), @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED), @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)})
    @PUT
    @Path("/{repositoryName}")
    @RequiresAuthentication
    @Validate
    public Response updateRepository(CondaProxyRepositoryApiRequest request, @ApiParam("Name of the repository to update") @PathParam("repositoryName") String repositoryName) {
        return super.updateRepository(request, repositoryName);
    }

    @GET
    @Path("/{repositoryName}")
    @RequiresAuthentication
    @Validate
    @ApiOperation("Get conda proxy repository")
    @Override
    public AbstractApiRepository getRepository(@ApiParam(hidden = true) @BeanParam final FormatAndType formatAndType,
                                               @PathParam("repositoryName") final String repositoryName) {
        return super.getRepository(formatAndType, repositoryName);
    }

    public boolean isApiEnabled() {
        return this.highAvailabilitySupportChecker.isSupported(CondaFormat.NAME);
    }
}
