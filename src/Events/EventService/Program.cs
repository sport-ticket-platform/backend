using System.Reflection;
using EventService.Events.Infrastructure.DbContext;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(Assembly.GetExecutingAssembly()));
builder.Services.AddScoped<ApplicationDbContext>();

var app = builder.Build();


app.Run();