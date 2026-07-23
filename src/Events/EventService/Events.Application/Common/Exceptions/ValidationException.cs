namespace EventService.Events.Application.Common.Exceptions;


public class ValidationException : Exception
{
    public IEnumerable<string> Errors { get; }

    public ValidationException(IEnumerable<string> errors)
        : base("One or more validation failures occurred.")
    {
        Errors = errors;
    }

    public ValidationException(string error)
        : this(new[] { error })
    {
    }
}