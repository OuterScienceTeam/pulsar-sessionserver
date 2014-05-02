package outerscience.pulsar.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import outerscience.pulsar.session.network.ClientConnection;
import outerscience.pulsar.session.network.ReceivablePacket;
import outerscience.pulsar.session.network.SendablePacket;
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

	private static final int HEADER_SIZE = 2;
	
	private static final int MAX_READ_PER_PASS = 5;	
	
	private ServerSocket _socket;
	
	private Selector _selector;

	private volatile boolean _shutdown = false;
	
	private ClientManager _clientManager;

	private PacketHandler _packetHandler;
	
	private FastTable<ClientConnection> _pendingClose = new FastTable<ClientConnection>().shared();

	//TODO dynamic buffer
	private ByteBuffer SHARED_WRITE_BUFFER = ByteBuffer.allocate(2048); 
	
	
	public ServerThread(ClientManager clientManager, PacketHandler packetHandler) throws IOException
	{
		super("ServerThread");	
		
		_selector = Selector.open();
	
		_clientManager = clientManager;
		
		_packetHandler = packetHandler;
		
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
				
				writeClosePacket(con);
				
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
			// notify connection
			con.getClient().onDisconnect();
		}
		finally
		{
			try
			{
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
	}
	
	private void writeClosePacket(ClientConnection con)
	{
		if (con.getOutgoingQueue().isEmpty())
		{
			return;
		}
		
		SendablePacket packet = null;

		while ((packet = con.getOutgoingQueue().removeFirst()) != null)
		{
			SHARED_WRITE_BUFFER.clear();
			
			putPacketIntoBuffer(packet, SHARED_WRITE_BUFFER);
			
			SHARED_WRITE_BUFFER.flip();
			
			try
			{
				con.write(SHARED_WRITE_BUFFER);
			}
			catch (IOException e)
			{
			}
			// it doesn't matter if the packet was written or not
			// the connection is going to be closed anyways and we can't afford to wait
		}
	}

	private void writePacket(SelectionKey key, ClientConnection con)
	{
		//TODO only one packet at a time is written into a buffer
		
		if(!prepareWriteBuffer(con))
		{
			key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
			return;
		}

		int dataSize = SHARED_WRITE_BUFFER.remaining(); 
		
		// write buffer
		int written = -1;
		try
		{			
			written = con.write(SHARED_WRITE_BUFFER);
		}
		catch(IOException e)
		{
		}
		
		_log.log(Level.FINEST, "written " + written + " out of "+dataSize);
		
		if(written >= 0)
		{			
			if(written == dataSize)
			{
				if(!con.hasPendingOutgoingPackets())
				{
					key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
				}
			}
			else
			{
				con.copyIntoWriteBuffer(SHARED_WRITE_BUFFER);
			}
		}
		else
		{
			con.getClient().onForceDisconnect();
			closeConnection(key, con);
		}
	}
	
	private boolean prepareWriteBuffer(ClientConnection con)
	{
		if(con.hasPendingWriteBuffer())
		{
			con.copyWriteBufferTo(SHARED_WRITE_BUFFER);			
			return true;
		}
		
		if(con.hasPendingOutgoingPackets())
		{
			SendablePacket packet = con.getOutgoingQueue().removeFirst();

			//TODO only one packet can be in the buffer at a time
			if(packet != null)
			{	
				putPacketIntoBuffer(packet, SHARED_WRITE_BUFFER);
				
				SHARED_WRITE_BUFFER.flip();				
				return true;
			}
		}
		return false;
	}
	
	//TODO this could go into SendablePacket
	private void putPacketIntoBuffer(final SendablePacket packet, final ByteBuffer buffer)
	{
		final int headerStart = buffer.position();
		final int dataStart = headerStart + HEADER_SIZE;
		
		// write data
		buffer.position(dataStart);
		packet.write(buffer);
		
		final int dataLength = buffer.position() - dataStart;
		
		// write header
		buffer.position(headerStart);
		buffer.putShort((short) dataLength);
		
		// encrypt packet data
		packet.getClient().encryptData(buffer, dataLength);
		
		buffer.position(dataStart + dataLength);
	}

	private void readPacket(SelectionKey key, ClientConnection con)
	{
		if(!con.isClosed())
		{
			ByteBuffer buffer = con.getReadBuffer();
			
			int read = -1;
			try
			{
				read = con.read(buffer);
			}
			catch (IOException e)
			{
			}
			
			if(read > 0)
			{
				buffer.flip();
				
				for(int i=0; i < MAX_READ_PER_PASS; i++)
				{
					if(!tryReadPacket(key, con, buffer))
					{
						return;
					}
				}			
				
				buffer.compact();
			}
			else // an error must have occurred
			{
				switch(read)
				{
					case 0:
					case -1:
						closeConnection(key, con);
						break;
						
					case -2:
						con.getClient().onForceDisconnect();
						closeConnection(key, con);
						break;
				}
			}
		}
	}
	
	private boolean tryReadPacket(SelectionKey key, ClientConnection con, ByteBuffer buffer)
	{		
		if(buffer.remaining() == 0) // the buffer is empty
		{
			return false; 
		}
		else if(buffer.remaining() < HEADER_SIZE) // not enough data for the header
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			return false;
		}
		else
		{
			final int packetSize = buffer.getShort();
			
			// TODO dynamic / magical buffer
			if(packetSize > buffer.capacity() - 2)
			{
				_log.log(Level.SEVERE, "The buffer is too small. The connection will be closed");
				con.getClient().onForceDisconnect();
				closeConnection(key, con);
				return false;
			}
			
			if(buffer.remaining() >= packetSize)
			{
				// skip packet without body
				if(packetSize == 0)
				{
					return true;
				}
				
				ReceivablePacket packet = _packetHandler.parsePacket(packetSize, buffer, con.getClient());

				if(packet != null)
				{
					//TODO this shall be performed by an executor
					packet.run();
				}
				
				return true;
			}
			
			// read because there isn't enough data
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			
			//move the position back before header
			buffer.position(buffer.position() - HEADER_SIZE);
			buffer.compact();
			return false;
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
