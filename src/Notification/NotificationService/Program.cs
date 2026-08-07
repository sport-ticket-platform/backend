using NotificationService.Services.EmailService;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddHttpClient<IEmailService, EmailService>();

var app = builder.Build();


app.MapGet("/", () => "Hello World!");

app.Run();