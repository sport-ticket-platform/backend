using System.Reflection;
using System.Security.Cryptography;
using EventService.Events.API.AuthorizationPolicies.Requirements;
using EventService.Events.API.Grpc.Interceptors;
using EventService.Events.API.Middlewares;
using EventService.Events.Application.Common.Behaviors;
using EventService.Events.Application.Interfaces;
using EventService.Events.Application.Queries.GetSeatsByConfig;
using EventService.Events.Domain.Repositories;
using EventService.Events.Infrastructure.DbContext;
using EventService.Events.Infrastructure;
using EventService.Events.Infrastructure.Grpc;
using EventService.Events.Infrastructure.Repositories;
using EventService.Reservations.Grpc;
using FluentValidation;
using MediatR;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using UserService.Users.Domain.Enums;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(Assembly.GetExecutingAssembly()));
builder.Services.AddScoped<ApplicationDbContext>();

builder.Services.AddValidatorsFromAssembly(typeof(GetSeatsByConfigQueryValidator).Assembly);
builder.Services.AddScoped(typeof(IPipelineBehavior<,>), typeof(ValidationBehavior<,>));
builder.Services.AddScoped<IReservationServiceClient, GrpcReservationServiceClient>();
builder.Services.AddScoped<IWriteRepository, WriteRepository>();

builder.Services.AddGrpcClient<ReservationService.ReservationServiceClient>(o =>
{
    o.Address = new Uri(builder.Configuration["ReservationService:GrpcUrl"]!);
}).Services.AddScoped<ExceptionInterceptor>();

var publicKey = builder.Configuration["Jwt:PublicKey"];
var audience = builder.Configuration["Jwt:Audience"];
var issuer = builder.Configuration["Jwt:Issuer"];

const string serviceName = "UserService";
const string serviceVersion = "1.0.0";
var otlpEndpoint = builder.Configuration["OpenTelemetry:OtlpEndpoint"] ?? "http://localhost:4317";


var rsa = RSA.Create();
rsa.ImportFromPem(publicKey);


builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.MapInboundClaims = false;

        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = issuer,

            ValidateAudience = true,
            ValidAudience = audience,

            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(1),

            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new RsaSecurityKey(rsa)
        };
    });


builder.Services.AddAuthorization(options =>
{
    options.AddPolicy("RequireUser", policy =>
        policy.Requirements.Add(new RoleRequirement(Role.USER)));

    options.AddPolicy("RequireAdmin", policy =>
        policy.Requirements.Add(new RoleRequirement(Role.ADMIN)));

    options.AddPolicy("RequireSupportOrAdmin", policy =>
        policy.Requirements.Add(new RoleRequirement(Role.SUPPORT, Role.ADMIN)));
});


// var resourceBuilder = ResourceBuilder.CreateDefault()
//     .AddService(serviceName: serviceName, serviceVersion: serviceVersion)
//     .AddAttributes(new Dictionary<string, object>
//     {
//         ["deployment.environment"] = builder.Environment.EnvironmentName
//     });
//
// // ---------- Traces ----------
// builder.Services.AddOpenTelemetry()
//     .WithTracing(tracing => tracing
//         .SetResourceBuilder(resourceBuilder)
//         .AddAspNetCoreInstrumentation(options =>
//         {
//             options.RecordException = true;
//         })
//         .AddHttpClientInstrumentation() // traces outbound calls (e.g., Auth → User service)
//         .AddNpgsql()                    // traces Postgres queries via Npgsql
//         .AddOtlpExporter(otlp =>
//         {
//             otlp.Endpoint = new Uri(otlpEndpoint);
//             otlp.Protocol = OpenTelemetry.Exporter.OtlpExportProtocol.Grpc;
//         }))
//
//     // ---------- Metrics ----------
//     .WithMetrics(metrics => metrics
//         .SetResourceBuilder(resourceBuilder)
//         .AddAspNetCoreInstrumentation()
//         .AddHttpClientInstrumentation()
//         .AddRuntimeInstrumentation()   // GC, thread pool, memory
//         .AddOtlpExporter(otlp =>
//         {
//             otlp.Endpoint = new Uri(otlpEndpoint);
//             otlp.Protocol = OpenTelemetry.Exporter.OtlpExportProtocol.Grpc;
//         }));
//
// // ---------- Logging ----------
// builder.Logging.ClearProviders();
// builder.Logging.AddOpenTelemetry(logging =>
// {
//     logging.SetResourceBuilder(resourceBuilder);
//     logging.IncludeFormattedMessage = true;
//     logging.IncludeScopes = true;
//     logging.ParseStateValues = true;
//
//     logging.AddOtlpExporter(otlp =>
//     {
//         otlp.Endpoint = new Uri(otlpEndpoint);
//         otlp.Protocol = OpenTelemetry.Exporter.OtlpExportProtocol.Grpc;
//     });
// });



var app = builder.Build();

app.UseMiddleware<ExceptionHandlingMiddleware>();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
app.Run();