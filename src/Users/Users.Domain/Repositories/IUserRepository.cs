using UserService.Users.Domain.Models;

namespace UserService.Users.Domain.Repositories;

public interface IUserRepository
{
    public void RegisterUser(User user);
    public void UpdateProfile(User user);
}