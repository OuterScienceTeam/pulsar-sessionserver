package outerscience.pulsar.session.network.clientpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.ReceivablePacket;

public class Packet_002_ClientPublicKey extends ReceivablePacket
{
	private byte[] _key;

	public void readBuffer(ByteBuffer buffer)
	{
		int keyLength = buffer.getShort();
		_key = new byte[keyLength];
		buffer.get(_key);
	}
	
	@Override
	public void run()
	{
		//TODO auto-generated method stub
	}
}
