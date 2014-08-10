package outerscience.pulsar.session.network.serverpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.SendablePacket;

public class Packet_001_Hello extends SendablePacket
{
	@Override
	public void write(ByteBuffer buffer)
	{
		buffer.put((byte) 1);
		
		//TODO take this from config
		writeUTFString("0.0.0", buffer);
		writeUTFString("Pulsar alpha server", buffer);
	}
}
