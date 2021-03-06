package com.shanjupay.merchant.controller;

import com.shanjupay.user.api.AuthorizationService;
import com.shanjupay.user.api.dto.authorization.RoleDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author gaoruan
 */
@Api(value = "商户平台-租户内自管理", tags = "商户平台-租户管理", description = "商户平台-租户账号角色相关的自管理")
@RestController
public class UserController {

	@DubboReference
	AuthorizationService authService;

	@ApiOperation("查询某租户下角色(不包含权限)")
	@ApiImplicitParam(name = "tenantId", value = "租户id", required = true, dataType = "Long", paramType = "path")
	@GetMapping("/my/roles/tenants/{tenantId}")
	public List<RoleDTO> queryRole(@PathVariable Long tenantId){
		return authService.queryRole(tenantId);
	}


}
