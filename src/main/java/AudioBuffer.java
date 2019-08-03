
import java.util.concurrent.SynchronousQueue;

//TODO Maybe rename to chain buffers?
public class AudioBuffer {
	private int bufferSize = 1024;
	private int maxNumOfBuffers = 10;
	private SynchronousQueue<byte[]> byteBuffers;
	private int curBufferIndex = 0;
	byte[] curBuffer;
	
	AudioBuffer(int bufferSize, int maxNumOfBuffers)
	{
		this.setBufferSize(bufferSize);
		this.setMaxNumOfBuffers(maxNumOfBuffers); 
		byteBuffers = new SynchronousQueue<byte[]>();
		
	}
	
	/**
	 * Fills data in the buffer chain. If one buffer runs out of room, another one is added to the queue and the data is sent to that buffer.
	 * @param data The byte array containing the data you wish to fill.
	 * @return If the maximum number of buffers has been hit, -1 is returned. If all goes well, 0 is returned.
	 * @throws InterruptedException
	 */
	public int fillBuffer(byte[] data) throws InterruptedException
	{
		int dataLen = data.length;
		int offset = curBufferIndex;
		for (int i = 0; i < dataLen; ++i)
		{
			offset = curBufferIndex + i;
			if (offset >= bufferSize)
			{
				if (byteBuffers.size() >= maxNumOfBuffers)
					return -1;
				
				byteBuffers.put(curBuffer);
				curBuffer = new byte[bufferSize];
				initBuffer(curBuffer);
				curBufferIndex = 0;
				curBuffer[offset] = data[i];
			}
			else
			{
				curBuffer[offset] = data[i];
			}
		}
		return 0;
	}
	
	public int pushBuffer(byte[] buffer)
	{
		if (byteBuffers.size() < maxNumOfBuffers)
		{
			byteBuffers.add(buffer);
			return 0;
		} else
			return -1;
	}
	
	public int createBuffer()
	{
		if (byteBuffers.size() < maxNumOfBuffers)
		{
			byte[] buffer = new byte[bufferSize];
			initBuffer(buffer);
			byteBuffers.add(buffer);
			return 0;
		} else
			return -1;
	}
	
	public void popBuffer()
	{
		byteBuffers.remove();
	}
	/**
	 * 
	 * @param buffer
	 * @param len
	 */
	public void initBuffer(byte[] buffer)
	{
	
		int len = buffer.length;
		for (int i = 0; i < len; ++i)
			buffer[i] = 0;
	}
	/**
	 * Gets the Maximum number of buffers
	 * @return
	 */
	public int getMaxNumOfBuffers() {
		return maxNumOfBuffers;
	}
	public void setMaxNumOfBuffers(int maxNumOfBuffers) {
		this.maxNumOfBuffers = maxNumOfBuffers < 1 ? 1: maxNumOfBuffers;
	}
	public int getBufferSize() {
		return bufferSize;
	}
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize < 1024 ? 1024: bufferSize;
	}
	public int getBufferCount() {
		return byteBuffers.size();
	}
	
	public SynchronousQueue<byte[]> getByteBuffers() {
		return byteBuffers;
	}

	public int getCurBufferIndex() {
		return curBufferIndex;
	}

	public void setCurBufferIndex(int curBufferIndex) {
		this.curBufferIndex = curBufferIndex;
	}
	
}