namespace NotificationService.Exceptions;

public class NotificationValidationException : Exception
{
    public NotificationValidationException(string message) : base(message) { }
}