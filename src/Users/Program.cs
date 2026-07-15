using UserService.Users.Application;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Repositories;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddScoped<ApplicationDbContext>();
builder.Services.AddScoped<IUserRepository, UserRepository>();
builder.Services.AddScoped<IUserService, UserService.Users.Application.UserService>();


var app = builder.Build();


app.Run();

