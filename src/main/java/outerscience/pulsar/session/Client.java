package outerscience.pulsar.session;

import java.util.logging.Logger;

import outerscience.pulsar.session.network.ClientConnection;

/**
 * Class representing a client 
 */
public class Client
{	
	private static final Logger _log = Logger.getLogger(Client.class.getName());
	
	
	public enum ClientState
	{
		/**
		 * The client has connected, but is not yet authenticated,
		 *  doesn't have a session established yet.
		 */
		CONNECTED,
		
		/**
		 * The session of this client has been established.
		 */
		AUTHENTICATED,
	}
	
	
	private final ClientManager _manager;
	
	private ClientState _state;
	
	private final ClientConnection _connection;
	
	private long _id = 0;
	
	private String _username = null;
	
	private String _sessionId = null;
	
	
	Client(final ClientManager manager, final ClientConnection connection)
	{
		_manager = manager;
		_connection = connection;
		_state = ClientState.CONNECTED;
	}

	public void onDisconnect()
	{

	}

	public String getName()
	{
		return _username;
	}
	
	public synchronized ClientState getClientState()
	{
		return _state;
	}
	
	public ClientConnection getConnection()
	{
		return _connection;
	}
}
