package outerscience.pulsar.session.network;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.Client;

public abstract class Packet
{
	protected Client _client;
	
	protected ByteBuffer _buffer;
	
	public Client getClient()
	{
		return _client;
	}

}
