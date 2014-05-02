package outerscience.pulsar.session;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import outerscience.pulsar.DatabasePool;
import outerscience.pulsar.session.network.ClientConnection;
import outerscience.pulsar.session.network.serverpackets.Disconnect;
import outerscience.pulsar.session.network.serverpackets.SessionId;
import outerscience.pulsar.session.network.serverpackets.Disconnect.DisconnectReason;

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

	public void onForceDisconnect()
	{
		// cancel session
		if(_sessionId != null)
		{
			try(PreparedStatement stat = DatabasePool.getConnection().prepareStatement("DELETE FROM `sessions` WHERE `sessid` = ? LIMIT 1;");)
			{
				stat.setString(1, _sessionId);
				stat.execute();
			}
			catch(Exception e)
			{
				_log.log(Level.SEVERE, "Error occured while removing session on disconnect", e);
			}
		}
		
		_manager.clientDisconnect(this);
	}
	
	public void onDisconnect()
	{
		onForceDisconnect();
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

	public void establishSession(long id, String username)
	{
		_id = id;
		_username = username;
		
		//TODO this is really not very good sessionid
		_sessionId = System.currentTimeMillis() + username + getConnection().getAddress().toString();
		try(PreparedStatement stat = DatabasePool.getConnection().prepareStatement("INSERT INTO `sessions` (`sessid`, `user_id`, `established`, `last_activity`, `ip_address`) VALUES(?, ?, ?, ?, ?);");)
		{
			stat.setString(1, _sessionId);
			stat.setLong(2, _id);
			stat.setLong(3, System.currentTimeMillis()); // created
			stat.setLong(4, System.currentTimeMillis()); // last validation
			stat.setString(5, getConnection().getAddress().toString());
			stat.execute();
		}
		catch (SQLException e)
		{
			_log.log(Level.SEVERE, "Failed to create session", e);
			
			getConnection().close(new Disconnect(DisconnectReason.SERVER_ERROR));
			
			return;
		}
		
		_log.info("Session for " + _username + " is " + _sessionId);
		
		_state = ClientState.AUTHENTICATED;
		
		getConnection().sendPacket(new SessionId(_sessionId));
	}

	/**
	 * Decrypts data in the buffer.
	 * 
	 * @param buffer the buffer with the data
	 * @param dataLength length of the data
	 */
	public void decryptData(final ByteBuffer buffer, final int dataLength)
	{
		//TODO decrypt data
	}
	
	/**
	 * Encrypts data stored in the buffer.
	 * 
	 * @param buffer the buffer with the data
	 * @param dataLength the length of the data in the buffer
	 */
	public void encryptData(final ByteBuffer buffer, final int dataLength)
	{
		//TODO encrypt data
	}	
}
