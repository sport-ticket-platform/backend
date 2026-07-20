using Microsoft.AspNetCore.Authorization;
using UserService.Users.Domain.Enums;

namespace UserService.Users.API.AuthorizationPolicies.Requirements;

public class RoleRequirement : IAuthorizationRequirement
{
    public Role[] AllowedRoles { get; }
    public RoleRequirement(params Role[] allowedRoles) => AllowedRoles = allowedRoles;
}