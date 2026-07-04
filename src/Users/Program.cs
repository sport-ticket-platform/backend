using UserService.Users.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddTransient<ApplicationDbContext>();


var app = builder.Build();


app.Run();

