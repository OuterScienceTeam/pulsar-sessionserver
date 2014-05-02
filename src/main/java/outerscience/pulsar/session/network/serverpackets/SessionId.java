package outerscience.pulsar.session.network.serverpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.PacketIDs;
import outerscience.pulsar.session.network.SendablePacket;

/**
 * 
 * Packet representing session ID to be sent back to the client
 *  after their session has been established.
 * 
 * @author hawstan
 *
 */
public class SessionId extends SendablePacket
{
	private final String _sessionId;
	
	public SessionId(String sessionId)
	{
		_sessionId = sessionId; 
	}

	@Override
	public void write(final ByteBuffer buffer)
	{
		buffer.put(PacketIDs.SESSION_ID);
		SendablePacket.writeUTFString(_sessionId, buffer);
	}
}
