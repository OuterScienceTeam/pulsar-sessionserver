package outerscience.pulsar.session.network.serverpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.SendablePacket;

/**
 * 
 * Packet sent to the user in case he is being disconnected 
 * 
 * @author hawstan
 * 
 */
public class Packet_006_Disconnect extends SendablePacket
{
	public enum DisconnectReason
	{
		BAD_LOGIN(1),
		PROTOCOL_VIOLATION(2),
		SERVER_ERROR(3),
		SERVER_SHUTDOWN(4);
		
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
	
	
	public Packet_006_Disconnect(final DisconnectReason reason)
	{
		_reason = reason;
	}
	
	@Override
	public void write(ByteBuffer buffer)
	{
		buffer.put((byte) 0x06);
		
		buffer.put(_reason.getCode());
	}

}
