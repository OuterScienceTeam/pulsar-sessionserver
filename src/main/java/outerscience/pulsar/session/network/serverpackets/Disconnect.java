package outerscience.pulsar.session.network.serverpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.PacketIDs;
import outerscience.pulsar.session.network.SendablePacket;

/**
 * 
 * Packet sent to the user in case he is being disconnected 
 * 
 * @author hawstan
 * 
 */
public class Disconnect extends SendablePacket
{
	public enum DisconnectReason
	{
		BAD_LOGIN(1),
		
		SERVER_ERROR(2),
		
		SERVER_SHUTDOWN(3);
		
		private byte _code;
		
		private DisconnectReason(int code)
		{
			_code = (byte) code;
		}
		
		public byte getCode()
		{
			return _code;
		}
	};

	private final DisconnectReason _reason;
	
	
	public Disconnect(final DisconnectReason reason)
	{
		_reason = reason;
	}
	
	@Override
	public void write(ByteBuffer buffer)
	{
		buffer.put(PacketIDs.DISCONNECT);
		
		buffer.put(_reason.getCode());
	}

}
