package outerscience.pulsar.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import outerscience.pulsar.DatabasePool;

public final class PulsarSessionServer
{
	private static final Logger _log = Logger.getLogger(PulsarSessionServer.class.getName());
	
	private static ServerThread listener;
	
	private static ClientManager clientManager;
	
	private static PacketHandler packetHandler;
	
	public static void main(String[] args)
	{
		// set up logging

		final String LOG_FOLDER = "log"; // Name of folder for log file
		final String LOG_CONFIG   = "config/log.cfg"; // Name of log file 
		
		new File(LOG_FOLDER).mkdirs();
		
		try(InputStream is = new FileInputStream(new File(LOG_CONFIG)))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch(FileNotFoundException e)
		{
			System.err.println("Log configuration file was not found at '"+LOG_CONFIG+"'.");
			e.printStackTrace();
			return;			
		}
		catch(IOException e)
		{
			System.err.println("Could not load log configuration.");
			e.printStackTrace();
			return;
		}		

		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		
		// load config		
		Config.load();
		
		// connect the database
		DatabasePool.initialise("config/hikari.properties");
		//TODO verify database connection here
		
		clientManager = new ClientManager();
		
		packetHandler = new PacketHandler(); 
		
		try
		{
			listener = new ServerThread(clientManager, packetHandler);
		}
		catch(IOException e)
		{
			_log.log(Level.SEVERE, "Failed to initialize listener",e);
			shutdown();
		}
		
		listener.start();
	}
	
	public static void shutdown()
	{
		_log.log(Level.INFO, "Stopping the Session Server...");
		
		if(listener != null)
			listener.shutdown();
		
		clientManager.shutdown();
		
		DatabasePool.shutdown();
		System.exit(0);
	}
}
