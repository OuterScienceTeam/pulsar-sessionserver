package outerscience.pulsar.session;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.PacketIDs;
import outerscience.pulsar.session.network.ReceivablePacket;
import outerscience.pulsar.session.network.clientpackets.LoginRequest;

//TODO javadoc
public class PacketHandler
{	
	public ReceivablePacket parsePacket(final int dataLength, final ByteBuffer buffer, final Client client)
	{		
		client.decryptData(buffer, dataLength);
		
		final int packetId = buffer.get() & 0xFF;
		
		ReceivablePacket packet = null;
		
		switch(packetId)
		{
			case PacketIDs.LOGIN_REQUEST:
				packet = new LoginRequest();
			break;
		
			default: // unknown packet, omit
				return null;
		}
		
		packet.initialize(client, buffer);
		
		packet.readBuffer();
		
		return packet;
	}

}
