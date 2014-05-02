package outerscience.pulsar;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This class is a singleton wrapper for HikariCP
 * 
 */
public class DatabasePool
{
	private static HikariDataSource hikariDatabasePool = null; 
	
	private DatabasePool(){}
	
	/**
	 * Initialises database pool
	 * 
	 * @param configFile path to the configuration file for HikariCP
	 */
	public static synchronized void initialise(String configFile)
	{
		if(hikariDatabasePool != null)
			return;
		
		HikariConfig config = new HikariConfig(configFile);
		hikariDatabasePool = new HikariDataSource(config);
	}

	public static Connection getConnection() throws SQLException
	{
		// This is a bit faster
		//  because the pool is not expected to be null most of the time
		try
		{
			return hikariDatabasePool.getConnection();
		}
		catch(NullPointerException e)
		{
			throw new SQLException("Database pool not initialized.");
		}
	}
	
	public static synchronized void shutdown()
	{
		if(hikariDatabasePool == null)
			return;
		
		hikariDatabasePool.shutdown();
		hikariDatabasePool = null;
	}
}
