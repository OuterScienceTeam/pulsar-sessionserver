package outerscience.pulsar.session.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class ReceivablePacket extends Packet implements Runnable
{
	protected ClientConnection _connection;
	
	protected ReceivablePacket()
	{
	}
	
	public void initialize(int packetId, ClientConnection connection)
	{
		_id = packetId;
		_connection = connection;
	}
	
	@Override
	public abstract void run();
	
	public abstract void readBuffer(ByteBuffer buffer);
	
	public ClientConnection getConnection()
	{
		return _connection;
	}
	
	public static String readUTFString(ByteBuffer buffer)
	{
		short length = buffer.getShort();
		byte[] string_bytes = new byte[length];
		buffer.get(string_bytes);
		return new String(string_bytes, StandardCharsets.UTF_8);
	}
}
