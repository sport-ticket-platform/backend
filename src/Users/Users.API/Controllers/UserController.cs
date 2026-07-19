using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace UserService.Users.API.Controllers;


[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UserController : ControllerBase
{

    [HttpGet("/profile")]
    public async Task<IActionResult> GetUserProfile()
    {
        var userIdClaim = User.FindFirst("sub")?.Value;
        
        if (!long.TryParse(userIdClaim, out var userId))
            return Unauthorized();
        throw new NotImplementedException();
    }
    
}