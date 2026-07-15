using UserService.Users.Infrastructure;
using UserService.Users.Infrastructure.DbContext;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddScoped<ApplicationDbContext>();


var app = builder.Build();


app.Run();

