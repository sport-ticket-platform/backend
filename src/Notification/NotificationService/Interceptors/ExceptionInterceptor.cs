using Grpc.Core;
using Grpc.Core.Interceptors;
using NotificationService.Exceptions;

namespace NotificationService.Interceptors;

public class ExceptionInterceptor : Interceptor 
{
    private readonly ILogger<ExceptionInterceptor> _logger;

    public ExceptionInterceptor(ILogger<ExceptionInterceptor> logger)
    {
        _logger = logger;
    }

    public override async Task<TResponse> UnaryServerHandler<TRequest, TResponse>(
        TRequest request,
        ServerCallContext context,
        UnaryServerMethod<TRequest, TResponse> continuation)
    {
        try
        {
            return await continuation(request, context);
        }
        catch (RpcException)
        {
            throw;
        }
        catch (EmailDeliveryException ex)
        {
            _logger.LogError(ex, "Email delivery failed");
            throw new RpcException(new Status(StatusCode.Unavailable, ex.Message));
        }
        catch (NotificationValidationException ex)
        {
            _logger.LogWarning(ex, "Notification validation failed");
            throw new RpcException(new Status(StatusCode.InvalidArgument, ex.Message));
        }
        catch (NotFoundException ex)
        {
            _logger.LogWarning(ex, "Resource not found");
            throw new RpcException(new Status(StatusCode.NotFound, ex.Message));
        }
        catch (OperationCanceledException ex) when (context.CancellationToken.IsCancellationRequested)
        {
            _logger.LogInformation(ex, "gRPC call cancelled by caller");
            throw new RpcException(new Status(StatusCode.Cancelled, "Request was cancelled."));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unhandled exception in gRPC call");
            throw new RpcException(new Status(StatusCode.Internal, "An unexpected error occurred."));
        }
    }
}