package outerscience.pulsar.session.network;

/**
 * 
 * This class contains ID numbers of all packet used for communication
 * with clients.
 * 
 */
public class PacketIDs
{
	// client packets
	public static final byte LOGIN_REQUEST = 2;
	
	// server packets
	public static final byte SESSION_ID = 3;

	public static final byte DISCONNECT = 5; 

}
