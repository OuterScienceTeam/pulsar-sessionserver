package outerscience.pulsar.session;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import outerscience.pulsar.session.network.ClientConnection;
import outerscience.pulsar.session.network.serverpackets.Disconnect;
import outerscience.pulsar.session.network.serverpackets.Disconnect.DisconnectReason;
import javolution.util.FastTable;

//TODO javadoc
public class ClientManager
{
	private static final Logger _log = Logger.getLogger(ClientManager.class.getName());
	
	private FastTable<Client> clients = new FastTable<Client>().shared();

	public Client create(ClientConnection con)
	{
		Client client = new Client(this, con);
		
		clients.add(client);
		
		return client;
	}

	public void clientDisconnect(Client client)
	{
		_log.log(Level.INFO, "Client was cancelled");
		clients.remove(client);		
	}

	public void shutdown()
	{
		Iterator<Client> iterator = clients.iterator();
		while(iterator.hasNext())
		{
			iterator.next().getConnection().sendPacket(new Disconnect(DisconnectReason.SERVER_SHUTDOWN));
		}
	}
}
