package outerscience.pulsar.session.network.clientpackets;

import java.nio.ByteBuffer;

import outerscience.pulsar.session.network.ReceivablePacket;

public class Packet_004_Login extends ReceivablePacket
{
	private String username;
	
	private String password;
		
	@Override
	public void readBuffer(ByteBuffer buffer)
	{
		username = readUTFString(buffer);
		password = readUTFString(buffer);		
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
	}
}
