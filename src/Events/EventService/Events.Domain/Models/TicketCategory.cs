using EventService.Events.Domain.Exceptions;

namespace EventService.Events.Domain.Models;

public class TicketCategory
{
    private TicketCategory(int categoryId, string name)
    {
        CategoryId = categoryId;
        Name = name;
    }

    public int CategoryId { get; private set; }
    public string Name { get; private set; }

    public TicketCategory Create(int categoryId, string name)
    {
        if (int.IsNegative(categoryId))
            throw new DomainException("The category ID must be positive");

        if (string.IsNullOrWhiteSpace(name))
            throw new DomainException("The category's name supplied is not a valid name");

        return new TicketCategory(categoryId, name);
    }
}