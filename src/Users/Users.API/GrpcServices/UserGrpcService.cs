using Grpc.Core;
using UserService.Grpc;
using UserService.Users.Application.Services;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;
using UserService.Users.Domain.Models;

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

    public override async Task<UserLoginInfoResponse> GetUserByEmail(GetUserLoginInfoByEmailRequest request,
        ServerCallContext context)
    {
        _logger.LogInformation("fetching user by email");

        if (string.IsNullOrWhiteSpace(request.Email))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Email is required."));

        var ct = context.CancellationToken;

        var user = await _userService.GetUserByEmail(request.Email, ct);
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

    public override async Task<UserLoginInfoResponse> GetUserByPhone(GetUserLoginInfoByPhoneRequest request,
        ServerCallContext context)
    {
        _logger.LogInformation("fetching user by phone number");

        if (string.IsNullOrWhiteSpace(request.Phone))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "phone number is required."));

        var ct = context.CancellationToken;
        var user = await _userService.GetUserByPhone(request.Phone, ct);

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

    public override async Task<UserLoginInfoResponse> GetUserById(GetUserLoginInfoByIdRequest request,
        ServerCallContext context)
    {
        _logger.LogInformation("fetching user by ID");

        if (long.IsNegative(request.Id))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "user ID must be a positive number."));

        var ct = context.CancellationToken;
        var user = await _userService.GetUserById(request.Id, ct);

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

    public override async Task<EmailExistsResponse> CheckEmailExists(CheckEmailExistsRequest request,
        ServerCallContext context)
    {
        _logger.LogInformation("checking whether the email exists");

        if (string.IsNullOrWhiteSpace(request.Email))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Email is required."));

        var ct = context.CancellationToken;
        var doesEmailExist = await _userService.CheckEmailExists(request.Email, ct);

        return new EmailExistsResponse()
        {
            Exists = doesEmailExist
        };
    }

    public override async Task<CreateUserResponse> CreateUser(CreateUserRequest request, ServerCallContext context)
    {
        _logger.LogInformation("creating new user");

        if (string.IsNullOrWhiteSpace(request.Email))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "Email is required."));

        if (string.IsNullOrWhiteSpace(request.FirstName))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "first name is required."));

        if (string.IsNullOrWhiteSpace(request.LastName))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "last name is required."));

        if (string.IsNullOrWhiteSpace(request.Password))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "password is required."));

        var ct = context.CancellationToken;

        var user = await _userService.CreateUser(User.Create(request.FirstName, request.LastName, Role.USER, request.Email,
                passwordHash: request.Password)
            , ct);
        return new CreateUserResponse()
        {
            UserId = user.UserId,
            Success = true
        };
    }

    public override async Task<ResetPasswordResponse> ChangeUserPassword(ResetPasswordRequest request,
        ServerCallContext context)
    {
        _logger.LogInformation("creating new user");

        if (string.IsNullOrWhiteSpace(request.NewPassword))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "new password is required."));

        if (long.IsNegative(request.Id))
            throw new RpcException(new Status(StatusCode.InvalidArgument, "user ID must be a positive number."));

        var ct = context.CancellationToken;

        await _userService.ChangePassword(request.Id, request.NewPassword,ct);

        return new ResetPasswordResponse()
        {
            Success = true
        };

    }
}