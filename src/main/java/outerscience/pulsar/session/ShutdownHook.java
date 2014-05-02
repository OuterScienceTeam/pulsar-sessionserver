package outerscience.pulsar.session;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Thread run upon server shutdown from the console. <br>
 * Mostly by SIGINT (Control-C) or similar signal. <br>
 * 
s * This thread ensures that the server is stopped properly before the JVM is halted.
 * 
 * @author hawstan
 *
 */
public class ShutdownHook extends Thread
{
	private static final Logger _log = Logger.getLogger(ShutdownHook.class.getName());
	
	public ShutdownHook()
	{
	}
	
	@Override
	public void run()
	{
		_log.log(Level.INFO, "Server shutdown hook has been called");
		
		PulsarSessionServer.shutdown();
	}

}
