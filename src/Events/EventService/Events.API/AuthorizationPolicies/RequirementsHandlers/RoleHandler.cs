using System.Security.Claims;
using EventService.Events.API.AuthorizationPolicies.Requirements;
using Microsoft.AspNetCore.Authorization;
using UserService.Users.Domain.Enums;

namespace UserService.Users.API.AuthorizationPolicies.RequirementsHandlers;

public class RoleHandler : AuthorizationHandler<RoleRequirement>
{
    protected override Task HandleRequirementAsync(
        AuthorizationHandlerContext context,
        RoleRequirement requirement)
    {
        var userRoles = GetRolesFromClaims(context.User);

        if (userRoles.Any(role => requirement.AllowedRoles.Contains(role)))
            context.Succeed(requirement);

        return Task.CompletedTask;
    }

    private static IEnumerable<Role> GetRolesFromClaims(ClaimsPrincipal user)
    {
        var claims = user.FindAll("roles").ToList();

        foreach (var claim in claims)
        {
            if (Enum.TryParse<Role>(claim.Value, ignoreCase: true, out var role)
                && Enum.IsDefined(typeof(Role), role))
            {
                yield return role;
            }
        }
    }
}