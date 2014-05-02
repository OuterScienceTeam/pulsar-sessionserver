package outerscience.pulsar.session.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import outerscience.pulsar.session.Client;

public abstract class ReceivablePacket extends Packet implements Runnable
{
	protected ReceivablePacket()
	{
	
	}
	
	@Override
	public abstract void run();
	
	public void initialize(Client client, ByteBuffer buffer)
	{
		_client = client;
		_buffer = buffer;
	}
	
	public abstract void readBuffer();

	public static String readUTFString(ByteBuffer buffer)
	{
		short length = buffer.getShort();
		byte[] string_bytes = new byte[length];
		buffer.get(string_bytes);
		return new String(string_bytes, StandardCharsets.UTF_8);
	}
}
