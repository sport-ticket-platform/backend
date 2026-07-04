using System.Data;
using Npgsql;

namespace UserService.Users.Infrastructure;

public class ApplicationDbContext : IDisposable
{
    public IDbConnection DbConnection { get; private set; }

    public ApplicationDbContext(IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("PostgresConnection");
        DbConnection = new NpgsqlConnection(connectionString);
    }

    public void Dispose()
    {
        DbConnection?.Dispose();
    }
}