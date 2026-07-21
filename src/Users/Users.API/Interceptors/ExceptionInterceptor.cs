using Grpc.Core;
using Grpc.Core.Interceptors;
using UserService.Users.Application.Exceptions;
using UserService.Users.Domain.Exceptions;
using UserService.Users.Infrastructure.Exceptions;

namespace UserService.Users.API.Interceptors ;


// exception handling for Grpc pipeline
public class ExceptionInterceptor : Interceptor
{
    private readonly ILogger<ExceptionInterceptor> _logger;

    public ExceptionInterceptor(ILogger<ExceptionInterceptor> logger) => _logger = logger;

    public override async Task<TResponse> UnaryServerHandler<TRequest, TResponse>(
        TRequest request, ServerCallContext context, UnaryServerMethod<TRequest, TResponse> continuation)
    {
        try
        {
            return await continuation(request, context);
        }
        catch (DomainException ex)
        {
            _logger.LogWarning(ex, "Domain validation failed");
            throw new RpcException(new Status(StatusCode.InvalidArgument, ex.Message));
        }
        catch (NotFoundException ex)
        {
            _logger.LogWarning(ex, "Resource not found");
            throw new RpcException(new Status(StatusCode.NotFound, ex.Message));
        }
        catch (InfrastructureException ex)
        {
            _logger.LogError(ex, "Infrastructure failure");
            throw new RpcException(new Status(StatusCode.Unavailable, "Service temporarily unavailable."));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unhandled exception in gRPC call");
            throw new RpcException(new Status(StatusCode.Internal, "An unexpected error occurred."));
        }
    }
}