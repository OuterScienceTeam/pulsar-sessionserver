package outerscience.pulsar.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import outerscience.pulsar.session.network.ClientConnection;
import javolution.util.FastTable;

/**
 * 
 * ServerThread manages network communication with the clients
 * 
 * @author hawstan
 *
 */
//TODO javadoc
//TODO buffers should be allocated dynamically
public final class ServerThread extends Thread
{
	private static final Logger _log = Logger.getLogger(ServerThread.class.getName());

	private static final long SLEEP_TIME = 10;
	
	private ServerSocket _socket;
	
	private Selector _selector;

	private volatile boolean _shutdown = false;
	
	private ClientManager _clientManager;

	private FastTable<ClientConnection> _pendingClose = new FastTable<ClientConnection>().shared();
	
	
	public ServerThread(ClientManager clientManager) throws IOException
	{
		super("ServerThread");	
		
		_selector = Selector.open();
	
		_clientManager = clientManager;
		
		openServerSocket();		
		
		_log.log(Level.INFO, "Listening at "+_socket.getInetAddress().toString()+":"+_socket.getLocalPort());
	}
	
	public final void openServerSocket() throws IOException
	{
		ServerSocketChannel soscketChannel = ServerSocketChannel.open();
		soscketChannel.configureBlocking(false);
		
		_socket = soscketChannel.socket();
		
		if (Config.server_address.equals("*"))
		{
			_socket.bind(new InetSocketAddress(Config.server_port), Config.socket_backlog);
		}
		else
		{
			_socket.bind(new InetSocketAddress(Config.server_address, Config.server_port), Config.socket_backlog);
		}
		
		soscketChannel.register(_selector, SelectionKey.OP_ACCEPT);
	}
	
	@Override
	public void run()
	{
		_log.log(Level.INFO, "Selector running");
		
		int selectedKeysCount = 0;
		
		SelectionKey key;
		
		ClientConnection con;
				
		while(!_shutdown)
		{
			try
			{
				selectedKeysCount = _selector.selectNow();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			
			if(selectedKeysCount > 0)
			{
				Iterator<SelectionKey> selectedKeys = _selector.selectedKeys().iterator();
				
				while(selectedKeys.hasNext())
				{
					key = selectedKeys.next();
					selectedKeys.remove();
					
					con = (ClientConnection) key.attachment();
					
					
					switch (key.readyOps())
					{
						case SelectionKey.OP_CONNECT:
							finishConnection(key, con);
							break;
						case SelectionKey.OP_ACCEPT:
							acceptConnection(key, con);
							break;
						case SelectionKey.OP_READ:
							readPacket(key, con);
							break;
						case SelectionKey.OP_WRITE:
							writePacket(key, con);
							break;
						case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
							writePacket(key, con);
							if (key.isValid())
							{
								readPacket(key, con);
							}
							break;
					}
				}
			}
			
			while(!_pendingClose.isEmpty())
			{
				con = _pendingClose.removeFirst();
				
				closeConnection(con.getSelectionKey(), con);
			}
			
			try
			{
				Thread.sleep(SLEEP_TIME);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		closeSelector();
	}

	public void addConnectionClosePending(ClientConnection con)
	{
		_pendingClose.addLast(con);
	}
	
	private void closeConnection(SelectionKey key, ClientConnection con)
	{
		_log.log(Level.FINEST, "Connection closed");
		try
		{
			con.writePendingPackets();
			
			// close the connection socket
			con.close();
		}
		catch (IOException e)
		{
		}
		finally
		{
			// clear attachment
			key.attach(null);
			// cancel key
			key.cancel();
		}
	}

	private void writePacket(SelectionKey key, ClientConnection con)
	{
		if(!con.writePacket())
		{
			closeConnection(key, con);
		}
	}

	private void readPacket(SelectionKey key, ClientConnection con)
	{
		if(!con.isClosed())
		{
			if(!con.readPacket())
			{
				closeConnection(key, con);
			}
		}
	}

	private void acceptConnection(SelectionKey key, ClientConnection con)
	{	
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc;
		
		try
		{
			while ((sc = ssc.accept()) != null)
			{
				//TODO IP address bans
				
				sc.configureBlocking(false);
				SelectionKey clientKey = sc.register(_selector, SelectionKey.OP_READ);
				con = new ClientConnection(this, sc.socket(), clientKey);
				con.setClient(_clientManager.create(con));
				clientKey.attach(con);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}	
	}

	private void finishConnection(SelectionKey key, ClientConnection con)
	{
		_log.log(Level.SEVERE, "Finish connection called");
	}

	public void shutdown()
	{
		_shutdown = true;
	}
	
	protected void closeSelector()
	{
		for (final SelectionKey key : _selector.keys())
		{
			try
			{
				key.channel().close();
			}
			catch (IOException e){}
		}
		
		try
		{
			_selector.close();
		}
		catch (IOException e){}
	}
}
