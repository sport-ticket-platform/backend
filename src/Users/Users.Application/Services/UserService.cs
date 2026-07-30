using Microsoft.AspNetCore.Mvc;
using UserService.Users.Application.Exceptions;
using UserService.Users.Application.Requests;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;
using UserService.Users.Domain.Repositories;

namespace UserService.Users.Application.Services;

public class UserService : IUserService
{
    private readonly ILogger<UserService> _logger;
    private readonly IUserRepository _userRepo;

    public UserService(ILogger<UserService> logger, IUserRepository userRepo)
    {
        _logger = logger;
        _userRepo = userRepo;
    }

    public async Task<UserProfile> GetUserProfileById(long userId, CancellationToken ct)
    {
        _logger.LogInformation("Fetching user's profile with user ID {userId}", userId);
        var userProfile = await _userRepo.GetUserProfileById(userId, ct) ??
                          throw new NotFoundException("The User not found");

        return userProfile;
    }

    public async Task ChangePassword(long userId, string newPasswordHash, CancellationToken ct)
    {
        _logger.LogInformation("Changing password for user ID {userId}", userId);

        var user = await _userRepo.GetUserById(userId, ct) ??
                   throw new NotFoundException("User not found");

        user.ChangePassword(newPasswordHash);
        await _userRepo.UpdateUser(user, ct);
    }

    public async Task UpdateUserProfile(UpdateProfileRequest updateRequest, CancellationToken ct)
    {
        _logger.LogInformation("Updating user's profile with user ID {userId}", updateRequest.UserId);

        var user = await _userRepo.GetUserById(updateRequest.UserId, ct);
        if (user is null)
            throw new NotFoundException("User not found");


        int? cityId = null;
        if(updateRequest.City is not null) 
            cityId = await _userRepo.GetCityIdByName(updateRequest.City, ct) ??
                                        throw new NotFoundException("The city not found");

        _logger.LogInformation("{cityName} has ID {cityId}",updateRequest.City,cityId);

        user.Update(updateRequest.FirstName, updateRequest.LastName, updateRequest.Email, updateRequest.PhoneNumber,
            cityId);
        await _userRepo.UpdateUser(user, ct);
    }

    public async Task<User> GetUserById(long userId, CancellationToken ct)
    {
        _logger.LogInformation("Fetching user with ID {userId}", userId);
        var user = await _userRepo.GetUserById(userId, ct) ??
                   throw new NotFoundException("User not found");

        return user;
    }

    public async Task<User> GetUserByEmail(string email, CancellationToken ct)
    {
        _logger.LogInformation("Fetching user by email {email}", email);
        var user = await _userRepo.GetUserByEmail(email, ct) ??
                   throw new NotFoundException("User not found");

        return user;
    }

    public async Task<User> GetUserByPhone(string phone, CancellationToken ct)
    {
        _logger.LogInformation("Fetching user by phone {phone}", phone);
        var user = await _userRepo.GetUserByPhone(phone, ct) ??
                   throw new NotFoundException("User not found");

        return user;
    }

    public async Task<bool> CheckEmailExists(string email, CancellationToken ct)
    {
        _logger.LogInformation("Checking whether email {email} exists", email);
        return await _userRepo.CheckEmailExists(email, ct);
    }

    public async Task<User> CreateUser(User user, CancellationToken ct)
    {
        _logger.LogInformation("Creating new user with email {email}", user.Email);

        if (await _userRepo.CheckEmailExists(user.Email, ct))
            throw new DomainException("A user with this email already exists");

        return await _userRepo.CreateUser(user, ct);
    }
    
    public async Task<int> CreateReport(long userId, string requestContent, ReportType type, CancellationToken ct)
    {
        _logger.LogInformation("creating a new report for user {userId}", userId);
        var report = Report.Create(userId, type, requestContent);

        var reportId = await _userRepo.CreateReport(report.UserId, report.Request, report.Type, ct);
        return reportId;
    }

    public async Task<List<UserReports>> GetAllReports(long userId, CancellationToken ct)
    {
        _logger.LogInformation("getting the the reports created by {userId}", userId);
        var reports = await _userRepo.GetAllReports(userId, ct);
        return reports;
    }

    public async Task<Report> GetReportDetails(long reportId, CancellationToken ct)
    {
        _logger.LogInformation("fetching report details {reportId}", reportId);
        var report = await _userRepo.GetReportDetails(reportId, ct);
        if (report is null)
            throw new ArgumentException("An invalid report ID was supplied.");
        return report;
    }

    public async Task<List<City>> SearchCities(string? searchTerm, int limit, int offset, CancellationToken ct)
    {
        _logger.LogInformation("fetching cities like {searchTerm}", searchTerm);
        return await _userRepo.SearchCities(searchTerm, limit, offset, ct);
    }
}