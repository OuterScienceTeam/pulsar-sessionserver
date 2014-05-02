package outerscience.pulsar.session.network.clientpackets;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import outerscience.pulsar.DatabasePool;
import outerscience.pulsar.session.Client.ClientState;
import outerscience.pulsar.session.network.ReceivablePacket;
import outerscience.pulsar.session.network.serverpackets.Disconnect;
import outerscience.pulsar.session.network.serverpackets.Disconnect.DisconnectReason;

public class LoginRequest extends ReceivablePacket
{
	private static final Logger _log = Logger.getLogger(LoginRequest.class.getName());
	
	private String username;
	
	private String password;
	
	public LoginRequest(){}

	@Override
	public void readBuffer()
	{		
		username = ReceivablePacket.readUTFString(_buffer);
		password = ReceivablePacket.readUTFString(_buffer);
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getPassword()
	{
		return password;
	}

	@Override
	public void run()
	{
		if(getClient().getClientState() == ClientState.AUTHENTICATED)
		{
			return;
		}
		
		String username = null;
		long id = 0;
		
		try(PreparedStatement stat = DatabasePool.getConnection().prepareStatement("SELECT `user_id`,`username` FROM accounts WHERE LOWER(`username`) = ? AND `password` = SHA1(?) LIMIT 1"))
		{
			stat.setString(1, getUsername().toLowerCase());
			stat.setString(2, getPassword());
			try(ResultSet rset = stat.executeQuery())
			{
				if(rset.next())
				{
					// have case-correct username from now on
					username = rset.getString("username");
					id = rset.getLong("user_id");	
				}
			}
		}
		catch(SQLException e)
		{
			_log.log(Level.SEVERE, "Failed to check login credentials", e);
			getClient().getConnection().close(new Disconnect(DisconnectReason.SERVER_ERROR));
		}
		
		// user ID was not set, therefore login is invalid
		if(id == 0)
		{
			getClient().getConnection().close(new Disconnect(DisconnectReason.BAD_LOGIN));
			return;
		}

		
		_log.log(Level.INFO, "User "+username+" authenticated");
		
		getClient().establishSession(id, username);
		
		return;
	}
}
