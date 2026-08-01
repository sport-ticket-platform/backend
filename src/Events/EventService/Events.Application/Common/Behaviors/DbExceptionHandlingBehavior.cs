using EventService.Events.Infrastructure.Exceptions;

namespace EventService.Events.Application.Common.Behaviors;

using MediatR;
using Npgsql;

public class DbExceptionHandlingBehavior<TRequest, TResponse> : IPipelineBehavior<TRequest, TResponse>
    where TRequest : IRequest<TResponse>
{
    private ILogger<DbExceptionHandlingBehavior<TRequest, TResponse>> _logger;

    public DbExceptionHandlingBehavior(ILogger<DbExceptionHandlingBehavior<TRequest, TResponse>>
        logger)
    {
        _logger = logger;
    }

    public async Task<TResponse> Handle(
        TRequest request,
        RequestHandlerDelegate<TResponse> next,
        CancellationToken cancellationToken)
    {
        try
        {
            return await next();
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query.");
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}