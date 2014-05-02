package outerscience.pulsar.session.network;


/**
 *
 * Class for managing packet statistics
 * 
 * @author hawstan
 *
 */
public class ConnectionStats
{
	private long sent = 0;
	
	private long received = 0;
	
	private long lastSentTime = 0;
	
	private long lastReceivedTime = 0;
	
	public ConnectionStats()
	{	
	}
	
	public synchronized void onSent()
	{
		lastSentTime = System.currentTimeMillis();
		sent++;
	}
	
	public synchronized void onReceived()
	{
		lastReceivedTime  = System.currentTimeMillis();
		received++;
	}
	
	public synchronized long getSent()
	{
		return sent;
	}
	
	public synchronized long getReceived()
	{
		return received;
	}
	
	public synchronized long getLastSent()
	{
		return lastSentTime;
	}
	
	public synchronized long getLastReceived()
	{
		return lastReceivedTime;
	}

	public long getTimeSinceLastPacket()
	{
		return System.currentTimeMillis() - Math.max(lastSentTime, lastReceivedTime);
	}
}
