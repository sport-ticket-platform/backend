using System.Data;
using Npgsql;

namespace EventService.Events.Infrastructure.DbContext;

public class ApplicationDbContext : IAsyncDisposable
{
    public IDbConnection DbConnection { get; private set; }

    public ApplicationDbContext(IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("PostgresConnection");
        DbConnection = new NpgsqlConnection(connectionString);
    }


    public async ValueTask DisposeAsync()
    {
        if (DbConnection is IAsyncDisposable dbConnectionAsyncDisposable)
            await dbConnectionAsyncDisposable.DisposeAsync();
        else
            DbConnection.Dispose();
    }
}