using System.Reflection;
using EventService.Events.Application.Common.Behaviors;
using EventService.Events.Application.Queries.GetSeatsByConfig;
using EventService.Events.Infrastructure.DbContext;
using FluentValidation;
using MediatR;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(Assembly.GetExecutingAssembly()));
builder.Services.AddScoped<ApplicationDbContext>();

builder.Services.AddValidatorsFromAssembly(typeof(GetSeatsByConfigQueryValidator).Assembly);
builder.Services.AddScoped(typeof(IPipelineBehavior<,>), typeof(ValidationBehavior<,>));

var app = builder.Build();


app.Run();