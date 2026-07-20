using System.Security.Cryptography;
using System.Text;
using FluentValidation;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.IdentityModel.Tokens;
using UserService.Users.API.ActionFilters;
using UserService.Users.API.AuthorizationPolicies.Requirements;
using UserService.Users.API.AuthorizationPolicies.RequirementsHandlers;
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
builder.Services.AddTransient<ExceptionHandlingMiddleware>(); 
builder.Services.AddScoped<IAuthorizationHandler, RoleHandler>();


builder.Services.AddValidatorsFromAssemblyContaining<UserProfileDtoValidator>(); 
builder.Services.AddControllers(options =>
{
    options.Filters.Add<GlobalValidationFilter>();
});

var publicKey = builder.Configuration["Jwt:PublicKey"];
var audience = builder.Configuration["Jwt:Audience"];
var issuer = builder.Configuration["Jwt:Issuer"];

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


var app = builder.Build();

app.UseMiddleware<ExceptionHandlingMiddleware>();
app.UseAuthentication();
app.UseAuthorization();
app.Run();

