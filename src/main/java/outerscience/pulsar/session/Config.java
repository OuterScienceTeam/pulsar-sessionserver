package outerscience.pulsar.session;


public class Config
{
	public static int server_port = 453;
	public static String server_address = "127.0.0.1";
	
	public static int socket_backlog = 10;

	/**
	 * For how many milliseconds the connection has to be silent for a KeepAlive packet to be sent 
	 */
	public static long maximum_keepalive_delay = 2000; 
	
	public static void load()
	{
		//TODO load configuration form a file
	}
}
