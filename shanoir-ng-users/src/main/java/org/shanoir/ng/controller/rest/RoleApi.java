package org.shanoir.ng.controller.rest;

import java.util.List;

import org.shanoir.ng.model.Role;
import org.shanoir.ng.model.error.ErrorModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "role", description = "the role API")
public interface RoleApi {

	@ApiOperation(value = "", notes = "Returns all the roles", response = Role.class, tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "founded roles", response = Role.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "no role founded", response = Void.class),
			@ApiResponse(code = 500, message = "unexpected error", response = ErrorModel.class) })
	@RequestMapping(value = "/role/all", produces = { "application/json" }, method = RequestMethod.GET)
	ResponseEntity<List<Role>> findRoles();

}
