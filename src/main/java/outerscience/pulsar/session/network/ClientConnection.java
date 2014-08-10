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
import outerscience.pulsar.session.network.clientpackets.Packet_002_ClientPublicKey;
import outerscience.pulsar.session.network.serverpackets.Packet_006_Disconnect;
import outerscience.pulsar.session.network.serverpackets.Packet_006_Disconnect.DisconnectReason;


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
	private static final int PACKET_HEADER_SIZE = 2;
	
	private static final int MAX_READ_PER_PASS = 5;
	
	enum ConnectionStatus
	{
		CONNECTED,
		ENCRYPTED,
		CLIENT;
	}
		
	
	//TODO make the buffers dynamically allocated and recycled
	//TODO the maximum size of a packet is 2045 bytes + id + 2byte length
	private final ByteBuffer readBuffer = ByteBuffer.allocate(2048);	
	
	private ByteBuffer writeBuffer = ByteBuffer.allocate(2048);
	
	
	private final ServerThread _server;
	
	private final Socket _socket;
	
	private final ReadableByteChannel _readableByteChannel;
	
	private final WritableByteChannel _writableByteChannel;
	
	private final SelectionKey _selectionKey;
	
	private Client _client = null;
	
	private boolean closed = false;
	
	private boolean _pendingClose = false;
	
	private ConnectionStatus status = ConnectionStatus.CONNECTED;
	
	private final PacketQueue<SendablePacket> outgoingQueue = new PacketQueue<SendablePacket>();	
	
	public ClientConnection(final ServerThread server, final Socket socket, final SelectionKey key)
	{
		_server = server;
		_socket = socket;
		_readableByteChannel = socket.getChannel();
		_writableByteChannel = socket.getChannel();
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
		return _socket.getInetAddress();
	}
	
	public int getPort()
	{
		return _socket.getPort();
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
		
		if(_client != null)
		{
			_client.onDisconnect();
		}
		
		closed = true;		
		
		_socket.close();
	}
	
	public void writePendingPackets()
	{
		SendablePacket p = null;
		while( (p = outgoingQueue.removeFirst()) != null)
		{
			putPacketIntoWriteBuffer(p);
		}
	}

	public boolean isClosed()
	{
		return closed;
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
			outgoingQueue.addLast(packet);
			
			_selectionKey.interestOps(_selectionKey.interestOps() | SelectionKey.OP_WRITE);
		}
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
	

	/**
	 * @return false on error, true otherwise
	 */
	public boolean readPacket()
	{
		int read = -1;
		try
		{
			read = _readableByteChannel.read(readBuffer);
		}
		catch (IOException e)
		{
		}
		
		if(read > 0)
		{
			readBuffer.flip();
			
			for(int i=0; i < MAX_READ_PER_PASS; i++)
			{
				int ret = tryReadPacket();

				if(ret == 0)
					break;
				else if(ret == -1)
					return false;
			}			
			
			readBuffer.compact();
			return true;
		}
		else // an error must have occurred
		{
			return false;
		}
	}
	
	/**
	 * 
	 * @return 0 if packet was not read
	 * @return 1 if packet was read
	 * @return -1 on error
	 */
	private int tryReadPacket()
	{		
		if(readBuffer.remaining() == 0) // the buffer is empty
		{
			return 0; 
		}
		else if(readBuffer.remaining() < PACKET_HEADER_SIZE) // not enough data for the header
		{
			_selectionKey.interestOps(_selectionKey.interestOps() | SelectionKey.OP_READ);
			return 0;
		}
		else
		{
			final int packetSize = readBuffer.getShort();
			
			// TODO dynamic / magical buffer
			if(packetSize > readBuffer.capacity() - 2)
			{
				//_log.log(Level.SEVERE, "The buffer is too small. The connection will be closed");
				return -1;
			}
			
			// an entire packet was read
			if(readBuffer.remaining() >= packetSize)
			{
				// skip packet without body
				if(packetSize == 0)
				{
					return 1;
				}
				
				parsePacket(packetSize);
				
				return 1;
			}
			
			// read because there isn't enough data
			_selectionKey.interestOps(_selectionKey.interestOps() | SelectionKey.OP_READ);
			
			//move the position back before header
			readBuffer.position(readBuffer.position() - PACKET_HEADER_SIZE);
			readBuffer.compact();
			return 0;
		}
	}
	
	private void parsePacket(int packetSize)
	{
		decryptData(readBuffer, packetSize);
		
		final int packetId = readBuffer.get() & 0xFF;
		
		ReceivablePacket packet = null;
		
		switch(packetId)
		{
			default: // unknown packet, fail
				close(new Packet_006_Disconnect(DisconnectReason.PROTOCOL_VIOLATION));
				return;
			
			case 0:
				// KeepAlive packet is empty
			break;
			
			case 2: //client public key
				if(status != ConnectionStatus.CONNECTED)
					close(new Packet_006_Disconnect(DisconnectReason.PROTOCOL_VIOLATION));
				packet = new Packet_002_ClientPublicKey(); 
			break;
		}
		
		packet.initialize(packetId, this);
		
		packet.readBuffer(readBuffer);
		
		packet.run();
	}

	
	public boolean writePacket()
	{
		if(!prepareWriteBuffer())
		{
			_selectionKey.interestOps(_selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
			return true;
		}

		int dataSize = writeBuffer.remaining(); 
		
		// write buffer
		int written = -1;
		try
		{			
			written = _writableByteChannel.write(writeBuffer);
		}
		catch(IOException e)
		{
		}
		
		//_log.log(Level.FINEST, "written " + written + " out of "+dataSize);
		
		if(written >= 0)
		{			
			if(written == dataSize)
			{
				if(outgoingQueue.isEmpty())
				{
					_selectionKey.interestOps(_selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private boolean prepareWriteBuffer()
	{
		if(writeBuffer.remaining() > 0)
		{			
			return true;
		}
		
		if(!outgoingQueue.isEmpty())
		{
			SendablePacket packet = outgoingQueue.removeFirst();

			//TODO only one packet can be in the buffer at a time
			if(packet != null)
			{	
				putPacketIntoWriteBuffer(packet);
				
				return true;
			}
		}
		return false;
	}
	
	private void putPacketIntoWriteBuffer(SendablePacket packet)
	{
		final int headerStart = writeBuffer.position();
		final int dataStart = headerStart + PACKET_HEADER_SIZE;
		
		// write data
		writeBuffer.position(dataStart);
		packet.write(writeBuffer);
		
		final int dataLength = writeBuffer.position() - dataStart;
		
		// write header
		writeBuffer.position(headerStart);
		writeBuffer.putShort((short) dataLength);
		
		// encrypt packet data
		encryptData(writeBuffer, dataLength);
		
		writeBuffer.position(dataStart + dataLength);
		writeBuffer.flip();
	}
}
