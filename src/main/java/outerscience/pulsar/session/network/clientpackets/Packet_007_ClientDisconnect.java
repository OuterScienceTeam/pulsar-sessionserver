package outerscience.pulsar.session.network.clientpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.ReceivablePacket;

public class Packet_007_ClientDisconnect extends ReceivablePacket
{
	private byte[] farewell_message = new byte[12];
	
	@Override
	public void readBuffer(ByteBuffer buffer)
	{
		buffer.get(farewell_message);		
	}
	
	@Override
	public void run()
	{
		//TODO disconnect connection and close connection
	}
}
