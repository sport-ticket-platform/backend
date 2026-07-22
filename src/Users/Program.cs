using System.Security.Cryptography;
using System.Text;
using FluentValidation;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.IdentityModel.Tokens;
using Npgsql;
using OpenTelemetry.Logs;
using OpenTelemetry.Metrics;
using OpenTelemetry.Resources;
using OpenTelemetry.Trace;
using UserService.Users.API.ActionFilters;
using UserService.Users.API.AuthorizationPolicies.Requirements;
using UserService.Users.API.AuthorizationPolicies.RequirementsHandlers;
using UserService.Users.API.GrpcServices;
using UserService.Users.API.Middlewares;
using UserService.Users.API.Validators;
using UserService.Users.Application.Services;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Repositories;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddScoped<ApplicationDbContext>();
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<IUserService, UserService.Users.Application.Services.UserService>();
builder.Services.AddScoped<IAuthorizationHandler, RoleHandler>();

builder.Services.AddGrpc();

builder.Services.AddValidatorsFromAssemblyContaining<UserProfileDtoValidator>(); 
builder.Services.AddControllers(options =>
{
    options.Filters.Add<GlobalValidationFilter>();
});

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
app.MapGrpcService<UserGrpcService>();
app.MapControllers();
app.UseAuthentication();
app.UseAuthorization();
app.Run();

