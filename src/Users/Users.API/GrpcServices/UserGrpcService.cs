using Grpc.Core;
using UserService.Grpc;
using UserService.Users.Application.Services;
using UserService.Users.Domain.Exceptions;

namespace UserService.Users.API.GrpcServices;

public class UserGrpcService : Grpc.UserService.UserServiceBase
{
    private readonly ILogger<UserGrpcService> _logger;
    private readonly IUserService _userService;

    public UserGrpcService(ILogger<UserGrpcService> logger, IUserService userService)
    {
        _logger = logger;
        _userService = userService;
    }
    public override async Task<UserLoginInfoResponse> GetUserByEmail(GetUserLoginInfoByEmailRequest request, ServerCallContext context)
    {
        _logger.LogInformation("fetching user by email");
        
        if (string.IsNullOrWhiteSpace(request.Email))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Email is required."));
        
        var ct = context.CancellationToken;

        try
        {
            var user = await _userService.GetUserByEmail(request.Email,ct);
            return new UserLoginInfoResponse()
            {
                Id = user.UserId,
                Email = user.Email,
                Phone = user.PhoneNumber,
                Password = user.PasswordHash,
                Role = user.Role.ToString(),
                IsTwoFactorEnabled = user.IsTwoFactorEnabled,
                Status = user.IsActive
            };
        }
        catch(DomainException ex)
        {
            
        }
        
        
        throw new NotImplementedException();
    }

    public override Task<UserLoginInfoResponse> GetUserByPhone(GetUserLoginInfoByPhoneRequest request, ServerCallContext context)
    {
        return base.GetUserByPhone(request, context);
    }

    public override Task<UserLoginInfoResponse> GetUserById(GetUserLoginInfoByIdRequest request, ServerCallContext context)
    {
        return base.GetUserById(request, context);
    }

    public override Task<EmailExistsResponse> CheckEmailExists(CheckEmailExistsRequest request, ServerCallContext context)
    {
        return base.CheckEmailExists(request, context);
    }

    public override Task<CreateUserResponse> CreateUser(CreateUserRequest request, ServerCallContext context)
    {
        return base.CreateUser(request, context);
    }

    public override Task<ResetPasswordResponse> ChangeUserPassword(ResetPasswordRequest request, ServerCallContext context)
    {
        return base.ChangeUserPassword(request, context);
    }
}