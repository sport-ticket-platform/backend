using NotificationService.Services.EmailService;

namespace NotificationService.Services.RabbitmqService;

using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

public class EmailQueueConsumer : BackgroundService
{
    private readonly RabbitMqOptions _options;
    private readonly ILogger<EmailQueueConsumer> _logger;
    private IConnection? _connection;
    private IChannel? _channel;
    private readonly IEmailService _emailService;

    public EmailQueueConsumer(
        IOptions<RabbitMqOptions> options,
        ILogger<EmailQueueConsumer> logger,
        IEmailService emailService)
    {
        _options = options.Value;
        _logger = logger;
        _emailService = emailService;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var factory = new ConnectionFactory
        {
            HostName = _options.HostName,
            Port = _options.Port,
            UserName = _options.UserName,
            Password = _options.Password,
            VirtualHost = _options.VirtualHost
        };

        _connection = await factory.CreateConnectionAsync(stoppingToken);
        _channel = await _connection.CreateChannelAsync(cancellationToken: stoppingToken);

        await _channel.QueueDeclareAsync(
            queue: _options.EmailQueueName,
            durable: true,
            exclusive: false,
            autoDelete: false,
            arguments: null,
            cancellationToken: stoppingToken);

        await _channel.BasicQosAsync(prefetchSize: 0, prefetchCount: 1, global: false, cancellationToken: stoppingToken);

        var consumer = new AsyncEventingBasicConsumer(_channel);

        consumer.ReceivedAsync += async (sender, eventArgs) =>
        {
            var body = eventArgs.Body.ToArray();
            var json = Encoding.UTF8.GetString(body);

            try
            {
                var message = JsonSerializer.Deserialize<EmailMessage>(json);

                if (message is null)
                {
                    _logger.LogWarning("Received null/invalid email message, discarding.");
                    await _channel.BasicAckAsync(eventArgs.DeliveryTag, multiple: false);
                    return;
                }

                await HandleEmailMessageAsync(message, stoppingToken);

                await _channel.BasicAckAsync(eventArgs.DeliveryTag, multiple: false);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to process message from {Queue}. Requeuing: {Payload}",
                    _options.EmailQueueName, json);

                await _channel.BasicNackAsync(eventArgs.DeliveryTag, multiple: false, requeue: false);
            }
        };

        await _channel.BasicConsumeAsync(
            queue: _options.EmailQueueName,
            autoAck: false,
            consumer: consumer,
            cancellationToken: stoppingToken);

        await Task.Delay(Timeout.Infinite, stoppingToken).ContinueWith(_ => { });
    }

    private async Task HandleEmailMessageAsync(EmailMessage message, CancellationToken cancellationToken)
    {
        _logger.LogInformation("Processing email to {To} with subject '{Subject}'", message.To, message.Subject);
        
        await _emailService.SendHtmlEmail(message.To,message.Subject,message.Body,cancellationToken);
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        if (_channel is not null) await _channel.CloseAsync(cancellationToken);
        if (_connection is not null) await _connection.CloseAsync(cancellationToken);

        await base.StopAsync(cancellationToken);
    }
}