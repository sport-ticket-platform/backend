using Microsoft.AspNetCore.Http.HttpResults;

namespace UserService.Users.Application.Exceptions;

public class NotFoundException : Exception
{
    public NotFoundException(string message) : base(message){}
}