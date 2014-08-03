package outerscience.pulsar.session.network;

import java.util.NoSuchElementException;

import javolution.util.FastTable;

/**
 * Thread-safe non-blocking queue primarily designed for packets 
 * 
 * @author hawstan
 *
 * @param <E> Data type for the items of this queue
 */
public class PacketQueue<E>
{
	private FastTable<E> list = new FastTable<E>().shared();
	
	/**
	 * @return the first element of the queue or null if the queue is empty
	 */
	public final E removeFirst()
	{
		try
		{
			return list.removeFirst();
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}
	
	public final E first()
	{
		try
		{
			return list.getFirst();
		}
		catch(NoSuchElementException e)
		{
			return null;
		}
	}
	
	/**
	 * @param element element to be added at the end of the queue 
	 */
	public final void addLast(final E element)
	{
		list.addLast(element);
	}
	
	public final boolean isEmpty()
	{
		return list.isEmpty();
	}

	public final void clear()
	{
		list.clear();
	}
}
