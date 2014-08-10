package outerscience.pulsar.session.network.clientpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.ReceivablePacket;

public class Packet_000_KeepAlive extends ReceivablePacket
{
	@Override
	public void readBuffer(ByteBuffer buffer)
	{
		// Do nothing, KEEPALIVE packet is empty
	}
	
	@Override
	public void run()
	{
		// Do nothing, KEEPALIVE packet is empty
	}
}
