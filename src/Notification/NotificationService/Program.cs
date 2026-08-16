using NotificationService.Interceptors;
using NotificationService.Middlewares;
using NotificationService.Services.EmailService;
using NotificationService.Services.RabbitmqService;
using Resend;

var builder = WebApplication.CreateBuilder(args);




var appPasswordFile = builder.Configuration["Email:AppPasswordFile"];
if (!string.IsNullOrEmpty(appPasswordFile) && File.Exists(appPasswordFile))
{
    var appPassword = (await File.ReadAllTextAsync(appPasswordFile)).Trim();
    builder.Configuration["Gmail:AppPassword"] = appPassword;
}

builder.Services.AddScoped<IEmailService, EmailService>();
builder.Services.Configure<EmailSenderOptions>(builder.Configuration.GetSection("Gmail"));
builder.Services.AddGrpc(options =>
{
    options.Interceptors.Add<ExceptionInterceptor>();
});


builder.Services.Configure<RabbitMqOptions>(builder.Configuration.GetSection("RabbitMq"));
builder.Services.AddHostedService<EmailQueueConsumer>();

builder.Services.AddScoped<ExceptionInterceptor>();


var app = builder.Build();

app.UseMiddleware<ExceptionHandlingMiddleware>();
app.MapGrpcService<NotificationService.Services.GrpcService.NotificationService>();
app.Run();