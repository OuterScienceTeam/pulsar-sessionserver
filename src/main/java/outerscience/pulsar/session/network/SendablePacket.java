package outerscience.pulsar.session.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import outerscience.pulsar.session.Client;

public abstract class SendablePacket extends Packet
{

	public static void writeUTFString(String string, ByteBuffer buffer)
	{
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		buffer.putShort((short) bytes.length);
		buffer.put(bytes);
	}

	public abstract void write(ByteBuffer buffer);
	
	public void setClient(final Client client)
	{
		_client = client;
	}
}
