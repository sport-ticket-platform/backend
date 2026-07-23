using EventService.Events.Infrastructure.Exceptions;

namespace EventService.Events.Application.Common.Behaviors;

using MediatR;
using Npgsql;

public class DbExceptionHandlingBehavior<TRequest, TResponse> : IPipelineBehavior<TRequest, TResponse>
    where TRequest : IRequest<TResponse>
{
   
}