package outerscience.pulsar.session.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

import outerscience.pulsar.session.Client;
import outerscience.pulsar.session.ServerThread;


/**
 * 
 * Data class containing necessary information for client's connection
 * 
 * @author hawstan
 *
 */
//TODO javadoc
public class ClientConnection
{	
	//TODO make the buffers dynamically allocated and recycled
	//TODO the maximum size of a packet is 2045 bytes + id + 2byte length
	private final ByteBuffer readBuffer = ByteBuffer.allocate(2048);	
	
	private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
	
	
	private final ServerThread _server;
	
	private final Socket _socket;
	
	private final InetAddress _address;
	
	private final int _port;
	
	private final ReadableByteChannel _readableByteChannel;
	
	private final WritableByteChannel _writableByteChannel;
	
	private final SelectionKey _selectionKey;
	
	private Client _client = null;
	
	private boolean closed = false;
	
	private boolean _pendingClose = false;
	
	private final PacketQueue<SendablePacket> outgoingQueue = new PacketQueue<SendablePacket>();	

	public ClientConnection(final ServerThread server, final Socket socket, final SelectionKey key)
	{
		_server = server;
		_socket = socket;
		_address = socket.getInetAddress();
		_readableByteChannel = socket.getChannel();
		_writableByteChannel = socket.getChannel();
		_port = socket.getPort();
		_selectionKey = key;
		
		try
		{
			_socket.setTcpNoDelay(true);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		
		// write buffer must be in read mode by default
		writeBuffer.flip();
	}

	public SelectionKey getSelectionKey()
	{
		return _selectionKey;
	}
	
	public Client getClient()
	{
		return _client;
	}

	public InetAddress getAddress()
	{
		return _address;
	}
	
	public int getPort()
	{
		return _port;
	}

	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}
	
	public ByteBuffer getWriteBuffer()
	{
		return writeBuffer;
	}
	
	public void setClient(final Client client)
	{
		if(_client != null)
			return;
		
		_client = client;
	}	
	
	public void close() throws IOException
	{
		if(closed)
			return;
		
		closed = true;
		
		_socket.close();
	}

	public boolean isClosed()
	{
		return closed;
	}
	
	public int read(final ByteBuffer buffer) throws IOException
	{
		return _readableByteChannel.read(buffer);
	}
	
	public int write(final ByteBuffer buffer) throws IOException
	{
		return _writableByteChannel.write(buffer);
	}
	
	
	public void close(SendablePacket closePacket)
	{

		if(_pendingClose)
		{
			return;
		}

		_pendingClose = true;
		
		outgoingQueue.clear();
		
		outgoingQueue.addLast(closePacket);
		
		try
		{
			_selectionKey.interestOps(_selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
		}
		catch (CancelledKeyException e)
		{
		}
		
		_server.addConnectionClosePending(this);
	}
	
	public synchronized void sendPacket(SendablePacket packet)
	{
		if(_pendingClose)
		{
			return;
		}
		
		if(packet != null)
		{
			packet.setClient(_client);
			
			outgoingQueue.addLast(packet);
			
			_selectionKey.interestOps(_selectionKey.interestOps() | SelectionKey.OP_WRITE);
		}
	}
	
	public boolean hasPendingOutgoingPackets()
	{
		return !outgoingQueue.isEmpty();
	}
	
	public PacketQueue<SendablePacket> getOutgoingQueue()
	{
		return outgoingQueue;
	}
	
	public boolean hasPendingWriteBuffer()
	{
		return writeBuffer.remaining() > 0;
	}

	public void copyWriteBufferTo(final ByteBuffer destinationBuffer)
	{
		destinationBuffer.clear();
		
		destinationBuffer.put(writeBuffer);
		
		destinationBuffer.flip();		
	}
	
	public void copyIntoWriteBuffer(final ByteBuffer sourceBuffer)
	{
		writeBuffer.clear();
		
		writeBuffer.put(sourceBuffer);
		
		writeBuffer.flip();
	}
}
