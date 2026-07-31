using System.Net.Sockets;
using Dapper;
using Npgsql;
using UserService.Users.Application.Exceptions;
using UserService.Users.Domain.Enums;
using UserService.Users.Domain.Exceptions;
using UserService.Users.Domain.Models;
using UserService.Users.Domain.ReadModels;
using UserService.Users.Domain.Repositories;
using UserService.Users.Infrastructure.DbContext;
using UserService.Users.Infrastructure.Exceptions;
using UserService.Users.Infrastructure.Persistence.Models;

namespace UserService.Users.Infrastructure.Repositories;

public class UserRepository : IUserRepository
{
    private ApplicationDbContext _dbContext;
    private ILogger<UserRepository> _logger;

    public UserRepository(ApplicationDbContext dbContext, ILogger<UserRepository> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    public async Task UpdateUser(User user, CancellationToken ct)
    {
        try
        {
            const string sql = @"
            UPDATE users
            SET
                first_name         = @FirstName,
                last_name           = @LastName,
                role                = @Role::user_role,
                email               = @Email,
                email_verified      = @IsEmailVerified,
                phone_number        = @PhoneNumber,
                phone_verified      = @IsPhoneNumberVerified,
                registration_date   = @RegistrationDate,
                password            = @PasswordHash,
                balance             = @Balance,
                city_id             = @CityId,
                is_active           = @IsActive,
                two_factor_enabled  = @IsTwoFactorEnabled
            WHERE user_id = @UserId;
        ";
            var parameters = new
            {
                UserId = user.UserId,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Role = user.Role.ToString(),
                Email = user.Email,
                IsEmailVerified = user.IsEmailVerified,
                PhoneNumber = user.PhoneNumber,
                IsPhoneNumberVerified = user.IsPhoneNumberVerified,
                RegistrationDate = user.RegistrationDate,
                PasswordHash = user.PasswordHash,
                Balance = user.Balance,
                CityId = user.CityId,
                IsActive = user.IsActive,
                IsTwoFactorEnabled = user.IsTwoFactorEnabled
            };

            var command = new CommandDefinition(sql, parameters, cancellationToken: ct);
            await _dbContext.DbConnection.ExecuteAsync(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                user.UserId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database operation timed out while fetching user {UserId}", user.UserId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
        {
            if (ex.ConstraintName is not null &&
                ex.ConstraintName.Contains("email", StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogWarning(ex, "Unique constraint violated while updating user with email {Email}.",
                    user.Email);
                throw new DomainException("A user with this email already exists.");
            }

            if (ex.ConstraintName is not null &&
                ex.ConstraintName.Contains("phone", StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogWarning(ex, "Unique constraint violated while updating user with phone number {Phone}.",
                    user.PhoneNumber);
                throw new DomainException("A user with this phone number already exists.");
            }

            _logger.LogWarning(ex, "Unique constraint violated: {ConstraintName}", ex.ConstraintName);
            throw new DomainException("A unique field conflict occurred.");
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", user.UserId,
                ex.SqlState);
            throw new InfrastructureException("DataBase operation failed", ex);
        }
    }

    public async Task<User?> GetUserById(long userId, CancellationToken ct)
    {
        try
        {
            const string sql = @"
            SELECT
                user_id            AS ""UserId"",
                first_name         AS ""FirstName"",
                last_name          AS ""LastName"",
                role               AS ""Role"",
                email              AS ""Email"",
                email_verified     AS ""IsEmailVerified"",
                phone_number       AS ""PhoneNumber"",
                phone_verified     AS ""IsPhoneNumberVerified"",
                registration_date  AS ""RegistrationDate"",
                password           AS ""PasswordHash"",
                balance            AS ""Balance"",
                city_id            AS ""CityId"",
                is_active          AS ""IsActive"",
                two_factor_enabled AS ""IsTwoFactorEnabled""
            FROM users
            WHERE user_id = @UserId;
            ";
            var command = new CommandDefinition(
                sql,
                new { UserId = userId },
                cancellationToken: ct
            );
            var user = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<UserPersistenceModel?>(command);

            if (user is null)
                return null;

            return User.Create(user.UserId, user.Firstname, user.LastName, user.Role, user.Email, user.PhoneNumber,
                user.RegistrationDate, user.PasswordHash, user.CityId, user.IsEmailVerified,
                user.IsPhoneNumberVerified);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                userId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user {UserId}", userId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", userId,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<int?> GetCityIdByName(string name, CancellationToken ct)
    {
        _logger.LogInformation("fetching city {name}", name);

        try
        {
            const string sql = @"
            SELECT city_id AS ""CityId""
            FROM city
            WHERE name = @Name;
            ";
            var command = new CommandDefinition(
                sql,
                new { Name = name },
                cancellationToken: ct
            );
            int? cityId = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<int?>(command);
            return cityId;
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the city {name}.",
                name);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching city {name}", name);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the city {name}.{state}", name,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<UserProfile?> GetUserProfileById(long userId, CancellationToken ct)
    {
        _logger.LogInformation("fetching user {userId}", userId);

        try
        {
            const string sql = @"
            SELECT
              u.first_name  AS ""FirstName"",
              u.last_name   AS ""LastName"",
              u.email       AS ""Email"",
              u.phone_number AS ""PhoneNumber"",
              c.name        AS ""City""
              FROM users u
              JOIN city c ON c.city_id = u.city_id
              WHERE u.user_id = @UserId;
              ";
            var command = new CommandDefinition(
                sql,
                new { UserId = userId },
                cancellationToken: ct
            );
            var userProfile = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<UserProfile?>(command);
            return userProfile;
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user {userId}.",
                userId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user {UserId}", userId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user {userId}.{state}", userId,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<User?> GetUserByEmail(string email, CancellationToken ct)
    {
        _logger.LogInformation("fetching user by email {email}", email);

        try
        {
            const string sql = @"
        SELECT
            user_id            AS ""UserId"",
            first_name         AS ""FirstName"",
            last_name          AS ""LastName"",
            role               AS ""Role"",
            email              AS ""Email"",
            email_verified     AS ""IsEmailVerified"",
            phone_number       AS ""PhoneNumber"",
            phone_verified     AS ""IsPhoneNumberVerified"",
            registration_date  AS ""RegistrationDate"",
            password           AS ""PasswordHash"",
            balance            AS ""Balance"",
            city_id            AS ""CityId"",
            is_active          AS ""IsActive"",
            two_factor_enabled AS ""IsTwoFactorEnabled""
        FROM users
        WHERE email = @email;
        ";
            var command = new CommandDefinition(
                sql,
                email,
                cancellationToken: ct
            );
            var user = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<UserPersistenceModel?>(command);

            if (user is null)
                return null;

            return User.Create(user.UserId, user.Firstname, user.LastName, user.Role, user.Email, user.PhoneNumber,
                user.RegistrationDate, user.PasswordHash, user.CityId, user.IsEmailVerified,
                user.IsPhoneNumberVerified);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user by email {email}.", email);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user by email {email}", email);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user by email {email}.{state}", email,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<User?> GetUserByPhone(string phone, CancellationToken ct)
    {
        _logger.LogInformation("fetching user by phone {phone}", phone);

        try
        {
            const string sql = @"
        SELECT
            user_id            AS ""UserId"",
            first_name         AS ""FirstName"",
            last_name          AS ""LastName"",
            role               AS ""Role"",
            email              AS ""Email"",
            email_verified     AS ""IsEmailVerified"",
            phone_number       AS ""PhoneNumber"",
            phone_verified     AS ""IsPhoneNumberVerified"",
            registration_date  AS ""RegistrationDate"",
            password           AS ""PasswordHash"",
            balance            AS ""Balance"",
            city_id            AS ""CityId"",
            is_active             AS ""IsActive"",
            two_factor_enabled AS ""IsTwoFactorEnabled""
        FROM users
        WHERE phone_number = @phone;
        ";
            var command = new CommandDefinition(
                sql,
                phone,
                cancellationToken: ct
            );
            var user = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<UserPersistenceModel?>(command);

            if (user is null)
                return null;

            return User.Create(user.UserId, user.Firstname, user.LastName, user.Role, user.Email, user.PhoneNumber,
                user.RegistrationDate, user.PasswordHash, user.CityId, user.IsEmailVerified,
                user.IsPhoneNumberVerified);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching the user by phone {phone}.", phone);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching user by phone {phone}", phone);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching the user by phone {phone}.{state}", phone,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<bool> CheckEmailExists(string email, CancellationToken ct)
    {
        _logger.LogInformation("checking whether email {email} exists", email);

        try
        {
            const string sql = @"SELECT EXISTS(SELECT 1 FROM users WHERE email = @Email);";
            var command = new CommandDefinition(
                sql,
                new { Email = email },
                cancellationToken: ct
            );
            return await _dbContext.DbConnection.QuerySingleAsync<bool>(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while checking email {email}.", email);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while checking email {email}", email);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while checking email {email}.{state}", email,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<bool> CheckPhoneExists(string phone, CancellationToken ct)
    {
        _logger.LogInformation("checking whether phone {phone} exists", phone);

        try
        {
            const string sql = @"SELECT EXISTS(SELECT 1 FROM users WHERE phone_number = @Phone);";

            var command = new CommandDefinition(sql,
                new { Phone = phone },
                cancellationToken: ct
            );
            return await _dbContext.DbConnection.QuerySingleAsync<bool>(command);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while checking phone {phone}.", phone);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while checking phone {phone}", phone);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while checking phone {phone}.{state}", phone,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<User> CreateUser(User user, CancellationToken ct)
    {
        _logger.LogInformation("creating user with email {email}", user.Email);

        try
        {
            const string sql = @"
        INSERT INTO users
            (first_name, last_name, role, email, email_verified, phone_number,
             phone_verified, registration_date, password, balance, city_id, status, two_factor_enabled)
        VALUES
            (@FirstName, @LastName, @Role::user_role, @Email, @IsEmailVerified, @PhoneNumber,
             @IsPhoneNumberVerified, @RegistrationDate, @PasswordHash, @Balance, @CityId, @Status, @IsTwoFactorEnabled)
        RETURNING
            user_id            AS ""UserId"",
            first_name         AS ""FirstName"",
            last_name          AS ""LastName"",
            role               AS ""Role"",
            email              AS ""Email"",
            email_verified     AS ""IsEmailVerified"",
            phone_number       AS ""PhoneNumber"",
            phone_verified     AS ""IsPhoneNumberVerified"",
            registration_date  AS ""RegistrationDate"",
            password           AS ""PasswordHash"",
            balance            AS ""Balance"",
            city_id            AS ""CityId"",
            is_active          AS ""IsActive"",
            two_factor_enabled AS ""IsTwoFactorEnabled"";
        ";
            var parameters = new
            {
                UserId = user.UserId,
                FirstName = user.FirstName,
                LastName = user.LastName,
                Role = user.Role.ToString(),
                Email = user.Email,
                IsEmailVerified = user.IsEmailVerified,
                PhoneNumber = user.PhoneNumber,
                IsPhoneNumberVerified = user.IsPhoneNumberVerified,
                RegistrationDate = user.RegistrationDate,
                PasswordHash = user.PasswordHash,
                Balance = user.Balance,
                CityId = user.CityId,
                IsActive = user.IsActive,
                IsTwoFactorEnabled = user.IsTwoFactorEnabled
            };

            var command = new CommandDefinition(
                sql,
                parameters,
                cancellationToken: ct
            );
            var persistedUser = await _dbContext.DbConnection.QuerySingleAsync<UserPersistenceModel>(command);

            return User.Create(persistedUser.UserId, persistedUser.Firstname, persistedUser.LastName,
                persistedUser.Role,
                persistedUser.Email, persistedUser.PhoneNumber,
                persistedUser.RegistrationDate, persistedUser.PasswordHash, persistedUser.CityId,
                persistedUser.IsEmailVerified,
                persistedUser.IsPhoneNumberVerified);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while creating user with email {email}.", user.Email);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database operation timed out while creating user with email {email}", user.Email);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex) when (ex.SqlState == PostgresErrorCodes.UniqueViolation)
        {
            _logger.LogWarning(ex, "Unique constraint violated while creating user with email {email}.", user.Email);
            throw new DomainException("A user with this email already exists");
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while creating user with email {email}.{state}",
                user.Email, ex.SqlState);
            throw new InfrastructureException("DataBase operation failed", ex);
        }
    }

    public async Task<int> CreateReport(long userId, string requestContent, ReportType type, CancellationToken ct)
    {
        const string sql = @"
        INSERT INTO report (user_id, type, request, status)
        VALUES (@UserId, @Type::report_type, @Request, @Status::report_status)
        RETURNING report_id;";

        try
        {
            var reportId = await _dbContext.DbConnection.QuerySingleAsync<int>(
                new CommandDefinition(sql, new
                {
                    UserId = userId,
                    Type = type.ToString(),
                    Request = requestContent,
                    Status = ReportStatus.OPEN.ToString()
                }, cancellationToken: ct));

            _logger.LogInformation("Created report {ReportId} for user {UserId}", reportId, userId);
            return reportId;
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while creating a new report.");
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while creating a new report.");
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex) when (ex.SqlState is PostgresErrorCodes.ForeignKeyViolation)
        {
            _logger.LogError(ex, "User referenced entity dose not exist.");
            throw new NotFoundException(
                "User referenced entity dose not exist.");
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while creating a new report.{state}", ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }


    public async Task<List<UserReports>> GetAllReports(long userId, CancellationToken ct)
    {
        const string sql = @"
            SELECT 
                report_id AS ReportId,
                status AS Status,
                reported_at AS ReportedAt
            FROM report
            WHERE user_id = @UserId";

        try
        {
            var command = new CommandDefinition(
                sql,
                new { UserId = userId },
                cancellationToken: ct
            );
            var reports = await _dbContext.DbConnection.QueryAsync<UserReports>(command);
            return reports.ToList();
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while getting all reports for user {userId}.", userId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while getting all reports for user {userId}.", userId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while getting all reports for user {userId}.{state}",
                userId, ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }

    public async Task<Report?> GetReportDetails(long reportId, CancellationToken ct)
    {
        const string sql = @"
        SELECT 
            report_id    AS ""ReportId"",
            user_id      AS ""UserId"",
            type         AS ""Type"",
            reported_at  AS ""ReportedAt"",
            request      AS ""Request"",
            response     AS ""Response"",
            responded_at AS ""RespondedAt"",
            status       AS ""Status""
        FROM report WHERE report_id = @ReportId";
        try
        {
            var command = new CommandDefinition(
                sql,
                new { ReportId = reportId },
                cancellationToken: ct
            );
            var report = await _dbContext.DbConnection.QueryFirstOrDefaultAsync<ReportPersistenceModel>(command);

            if (report is null)
                return null;

            return Report.Create(report.ReportId, report.UserId, report.Type, report.ReportedAt, report.Request,
                report.Response, report.RespondedAt, report.Status);
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching report {reportId}.", reportId);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching report {reportId}.", reportId);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while fetching report {reportId}.{state}",
                reportId, ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }


    public async Task<List<City>> SearchCities(string? searchTerm, int limit, int offset, CancellationToken ct)
    {
        string sql = "";
        object parameters;
        if (searchTerm is not null)
        {
            sql = @"
            SELECT
                city_id AS ""CityId"",
                name    AS ""Name""
            FROM city
            WHERE name ILIKE @SearchTerm
            ORDER BY name
            LIMIT @Limit OFFSET @Offset;";
            parameters = new { SearchTerm = $"%{searchTerm}%", Limit = limit, Offset = offset };
        }
        else
        {
            sql = @"
            SELECT 
            city_id AS CityId,
            name AS Name
            FROM city
            LIMIT @Limit OFFSET @Offset;";
            parameters = new { Limit = limit, Offset = offset };
        }

        try
        {
            var cities = await _dbContext.DbConnection.QueryAsync<CityPersistenceModel>(
                new CommandDefinition(sql, parameters, cancellationToken: ct));

            return cities
                .Select(c => City.Create(c.CityId, c.Name))
                .ToList();
        }
        catch (NpgsqlException ex) when (ex.InnerException is IOException)
        {
            _logger.LogCritical(ex, "Database connection failed while fetching cities {searchTerm}.", searchTerm);
            throw new InfrastructureException("Unable to reach the database", ex);
        }
        catch (NpgsqlException ex) when (ex.InnerException is TimeoutException)
        {
            _logger.LogError(ex, "Database query timed out while fetching the cities {searchTerm}.", searchTerm);
            throw new InfrastructureException("Database operation timed out.", ex);
        }
        catch (PostgresException ex)
        {
            _logger.LogError(ex, "Database rejected the query while the cities {searchTerm}.{state}", searchTerm,
                ex.SqlState);
            throw new InfrastructureException("DataBase query failed", ex);
        }
    }
}