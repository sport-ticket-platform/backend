using FluentValidation;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using ValidationException = UserService.Users.Application.Exceptions.ValidationException;

namespace UserService.Users.API.ActionFilters;

public class GlobalValidationFilter : IAsyncActionFilter
{
    private readonly IServiceProvider _serviceProvider;

    public GlobalValidationFilter(IServiceProvider serviceProvider) => _serviceProvider = serviceProvider;

    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        foreach (var argument in context.ActionArguments.Values)
        {
            if (argument is null) continue;

            var validatorType = typeof(IValidator<>).MakeGenericType(argument.GetType());
            var validator = _serviceProvider.GetService(validatorType) as IValidator;

            if (validator is null) continue; 

            var validationContext = new ValidationContext<object>(argument);
            var result = await validator.ValidateAsync(validationContext);

            if (!result.IsValid)
            {
                var errors = result.Errors.Select(v => v.ErrorMessage).ToList();
                throw new ValidationException(errors);
            }
        }

        await next();
    }
}