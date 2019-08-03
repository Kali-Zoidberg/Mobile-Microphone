import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * To implement multiple, clientSocket = accept should be on a separate thread which will be blocked until someone connects.
 * Once someone connects, they are added the the synchronous queue.
 * Need to do consumer prodcuer. this means syncrhonization.
 * @author nickj In memoriam Grandis Jack Betzold
 */

public class Server {
	private ServerSocket serverSocket;
	private DatagramSocket dataSocket;
	private DatagramPacket packet;
	private Socket clientSocket;
	private int portNumber;
	private PrintWriter clientOutputStream;
	private FileWriter stringFile = null;
	private BufferedReader clientInputStream;
	private Hashtable<String, Socket> clientTable = new Hashtable<String, Socket>();
	private Hashtable<String, PrintWriter> clientOutputStreams = new Hashtable<String, PrintWriter>();
	private Hashtable<String, BufferedReader> clientInputStreams =  new Hashtable<String, BufferedReader>();
	private LinkedList<Tuple<String, Socket>> clientSocketQueue = new LinkedList<Tuple<String, Socket>>();
	private LinkedList<Tuple<String, BufferedReader>> clientInputQueue =  new LinkedList<Tuple<String, BufferedReader>>();
	private LinkedList<Tuple<String, PrintWriter>> clientOutputQueue =  new LinkedList<Tuple<String, PrintWriter>>();
	private int bufferSize = 8012;
	private AudioPlayThread audioPlayThread;
	private ArrayList<byte[]> audioBuffers = new ArrayList<byte[]>(); 
	private boolean UDPRunning = false;
	private String regex = " ";
	private long timeSinceLastMessage;
	private long clientPing = 0;
	
	private long pingRatio = 2;
	

	
	private AudioFormat clientAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);
	private StartServerThread serverStartThread = new StartServerThread(this);

	
	private boolean isRunning = true;
	
	Server(int port)
	{
		portNumber = port;
		
		try {
			
			serverSocket = new ServerSocket(portNumber);
			audioPlayThread = new AudioPlayThread(Main.cableInputLine, this, clientAudioFormat);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		
		}
	}
	
	
	/**
	 * Creates a client output stream and returns it. Read from this stream to communicate with the server.
	 * @param name The name of the client socket
	 * @return Returns a print writer to which the client writes input
	 * @throws IOException
	 */
	
	public PrintWriter createClientOutputStream(String name) throws IOException
	{
		Socket clientSocket = clientTable.get(name);
	//	if (clientSocket == null)
	//		clientSocket = this.addClient(name);
		PrintWriter clientOutputStream = new PrintWriter(clientSocket.getOutputStream(), true);
		clientOutputStreams.put(name, clientOutputStream);
		return clientOutputStream;
		
	}
	
	
	/**
	 * Creates an InputStream for the client to write to the server.
	 * @param name The name of the client socket.
	 * @return
	 * @throws IOException
	 */
	
	public BufferedReader createClientInputStream(String name) throws IOException
	{
		Socket clientSocket = clientTable.get(name);
	//	if(clientSocket == null)
	//		clientSocket = this.addClient(name);
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		return inputStream;
	}
	
	/**
	 * Closes the UDP server.
	 */
	
	public void closeUDPServer()
	{
		System.out.println("******Closing UDP socket*******");
		dataSocket.close();
		UDPRunning = false;
	}
	
	/**
	 * Closes the server and all client sockets, input streams, and output streams.
	 */
	
	public void closeServer()
	{
		System.out.println("*********Beginning Close Operations*********");
		/*
		 * Legacy code for supporting multiple clients.
		Set<String> clientSocketKeys = clientTable.keySet();
		
		for (String name: clientSocketKeys)
		{
			try {
				clientOutputStreams.get(name).println("Bye.");
				clientTable.get(name).close();
				clientInputStreams.get(name).close();
				
				clientOutputStreams.get(name).close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		*/
		try {
			System.out.println("***********Closing client socket*************");
			this.closeClientConnection();
			stringFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			isRunning = false;
		System.out.println("Ending server....");
	}
	
	
	
	/**
	 * Starts the server on a separate thread.
	 */
	
	public void startServer()
	{
		try {
			 stringFile = new FileWriter("ClientServer.txt");
		
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		serverStartThread.start();
		audioPlayThread.start();
	}
	
	
	public void serverTick(byte[] buffer, DatagramPacket packet)
	{
		
	}
	/**
	 * The main while loop that runs the server. Either start this on a separate thread or call startServer.
	 */
	
	public void runServer()
	{
		System.out.println("******************************************");
		System.out.println("************Server started****************");
		System.out.println("******************************************");
		//AcceptThread acceptClients = new AcceptThread();
		//acceptClients.start();
			try {
//				clientSocket = serverSocket.accept();
//				clientOutputStream = new PrintWriter(clientSocket.getOutputStream(), true);
//	            clientInputStream = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));

				while(isRunning)
		    	{
					 try {
					 	serverTick(clientSocket, clientOutputStream, clientInputStream);
					 } catch (java.net.SocketException e)
					 {
					 	System.out.println("Client closed connection. Ending server");
					 		this.closeServer();
					 }
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			/***Legacy Code for allowing multiple users. No point in spending time implementing
			 * When this is just suppose to be one way communication
           
            try {
				acceptClients.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			closeServer();
			 */
		
	}
	

	/**
	 * Prints the protocol of the server.
	 */
	
	public void printProtocol()
	{
		String commands[] = {"A", "UID", "S", "F"};
		
		Hashtable<String, String> commandDescription = new Hashtable<String, String>();
		Hashtable<String, String> subStringCommands = new Hashtable<String, String>();
		commandDescription.put("A", "Sends audio data using _ as a regex");
		commandDescription.put("UID", "Specifies the user's id so that the Server may verify it");
		commandDescription.put("S", "Specifies server commands.");
		commandDescription.put("F", "Specifies audio format.");
		subStringCommands.put("A", "No commands");
		subStringCommands.put("UID", "No sub commands");
		subStringCommands.put("S", "Sub commands are: \nclose - Closes the server.\ndisconnect - Disconnects the client from the server.");
		subStringCommands.put("F", "Split Audio specifications between spaces. The ordering is {float SampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate");
		
		for (String str: commands)
		{
			System.out.printf("Command format:\n %s.xxxxxxx\n", str);
			System.out.printf("Command %s\n Description: %s \n sub commands %s\n", str, commandDescription.get(str),subStringCommands.get(str));
			
		}
	}
	
	public boolean verifyUID(String uid)
	{
		return true;
	}
	
	
	/**
	 * Analyze's a command set by a user.
	 * Valid commands are:
	 * @param line
	 * @param outputStream
	 * @return
	 */
	
	public boolean analyzeCommand(String line, PrintWriter outputStream)
	{
		
		
		int periodIndex =line.indexOf('.');
		this.adjustPing(System.currentTimeMillis());
		
		if (periodIndex <= 0 || periodIndex == line.length() - 1)
		{
			outputStream.println("Invalid Command: " + line);
			return false;
		}
		
		String command = line.substring(0, periodIndex);
		String subCommand = line.substring(periodIndex + 1, line.length());
		
		if (command.equals("UID"))
		{
			if(verifyUID(subCommand))
			{
				System.out.println("Valid uid");
				outputStream.println("Valid UID. Accepting client connection.");
				return true;
			} else
				return false;
		}
		
		if(command.equals("A"))
		{
			outputStream.println("Thank you for the audio bytes!");
			
			//if the number of buffers is > than the ping ratio, play the audio from one buffer
			
			//if the ping is greater than the audio rate, then create a buffer of twice the size and play from there.
		
			  if (processAudioBytes(subCommand))
			  {
			  		outputStream.println("Those were valid audio bytes!");
			  		return true;
			  } else
			  {
			  		outputStream.println("Those were invalid audio bytes.");
			  		return false;
			  }
			 
		
		}
		
		
		if (command.equals("F"))
		{
			
			String[] specifications = subCommand.split(this.regex);
			
			try {
				
				float sampleRate = Float.parseFloat(specifications[0]);
				int sampleSizeInBits = Integer.parseInt(specifications[1]);
				int channels = Integer.parseInt(specifications[2]);
				int frameSize = Integer.parseInt(specifications[3]);
				float frameRate = Float.parseFloat(specifications[4]);
				clientAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, false);
			
			} catch (NumberFormatException e)
			{
				
				outputStream.println("Invalid number format for one or more audio format specifications");
				e.printStackTrace();
				
			} catch(ArrayIndexOutOfBoundsException e)
			{
				
				outputStream.println("Invalid number of audio format specifications");
			
			}
		}
		
		if (command.equals("S"))
		{
			if(subCommand.equals("disconnect"))
			{
				//reset client stuff
				System.out.println("Closing client connection.");
				this.closeClientConnection();
			}
			
			if (subCommand.equals("close"))
			{
				//closes the server.
				System.out.println("Closing server.");
				this.closeServer();
			}
			
			if(subCommand.equals("udp"))
			{
				System.out.println("Beginning UDP transition.");
				try {
					setupUDP(this.portNumber);
				} catch (SocketException e)
				{
					System.out.println("Could not open socket");
					e.printStackTrace();
					
				}
			}
		}
		return true;
		
	}
	
	/**
	 * Sets up a UDP socket for the server.
	 * @param port
	 * @throws SocketException
	 */
	public void setupUDP(int port) throws SocketException
	{
		dataSocket = new DatagramSocket(port);
		UDPRunning = true;
	}
	

	
	/**
	 * Processes audio bytes and plays them on the audio play thread
	 * @param audioBytes The audio bytes to be played.
	 * @return Returns true if succesful.
	 */
	public boolean processAudioBytes(byte[] audioBytes) {
		
		if (audioPlayThread != null)
		{
			//this will also block the server thread but it will still receive data from the socket.. I think...
			
			
				System.out.println("Putting bytes on audio play thread");
					audioPlayThread.pipeLineBuffer.add(audioBytes);
			//maybe notify thread here? 
			//System.out.println(audioPlayThread.pipeLineBuffer.size());
			//this.pingRatio = (long) Math.ceil((double) ( AudioFunctions.getAudioPlayTime(this.clientAudioFormat, data.length) / this.getPing()));
		
			//Alternative is to block on the audio play thread which may mean that the audio is never actually processed so I believe blocking on the server thread is optimal.
			//here is the alternative anyways

			//audioPlayThread.addAudioToQueue(data);
		}
		else
			System.out.println("Audio Play Thread has not been started. This data is going nowhere.");
		
		return true;
	}
	
	public boolean processAudioBytes(String strBytes) {
		byte[] data = AudioFunctions.getBytesFromString(strBytes, regex);
		float playTime  = AudioFunctions.getAudioPlayTime(this.clientAudioFormat, data.length);
		
		if (audioPlayThread != null)
		{
			//this will also block the server thread but it will still receive data from the socket.. I think...
			
			
				System.out.println("Putting bytes on audio play thread");
					audioPlayThread.pipeLineBuffer.add(data);
			//maybe notify thread here? 
			//System.out.println(audioPlayThread.pipeLineBuffer.size());
			//this.pingRatio = (long) Math.ceil((double) ( AudioFunctions.getAudioPlayTime(this.clientAudioFormat, data.length) / this.getPing()));
		
			//Alternative is to block on the audio play thread which may mean that the audio is never actually processed so I believe blocking on the server thread is optimal.
			//here is the alternative anyways

			//audioPlayThread.addAudioToQueue(data);
		}
		else
			System.out.println("Audio Play Thread has not been started. This data is going nowhere.");
		
		return true;
	}
	
	
		
	/**
	 * Cleans up client connection to allow for another client to connect.
	 */
	
	public void closeClientConnection()
	{
		try {
			clientSocket.close();
			clientInputStream.close();
			clientOutputStream.close();
			clientInputStream = null;
			clientOutputStream = null;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	
	//how do we exit. We don't necessarily want to just make a string as that may take too long.
	public void analyzeUDPCommand(byte[] buffer, DatagramPacket packet)
	{
		String recieved = new String(packet.getData(), 0, packet.getLength());
		if(recieved.equals("end"))
		{
			closeUDPServer();
		} else
		{
			processAudioBytes(packet.getData());
		}
	}
	
	/**
	 * Iterates the server one tick
	 */
	
	public void serverTick(Socket curSocket, PrintWriter curOutputStream, BufferedReader curInputStream) throws java.net.SocketException
	{
		//checkQueues();

		//try {
	
	//	Set<String> names = clientTable.keySet();
		String curLine = null;
	//		for (String name : names)
	//		{
				//System.out.println("Checking clients");
					
			//	Socket curSocket = clientTable.get(name);
			//	PrintWriter curOutputStream = clientOutputStreams.get(name);
			//	BufferedReader curInputStream = clientInputStreams.get(name);
				//if (clientInputStream != null && clientOutputStream != null && !clientSocket.isClosed())
				if (true)
				{
					try {
						System.out.println("*****Waiting to Read line from User*******");
						try {
							if(UDPRunning)
							{
								byte buf[] = new byte[bufferSize];
								DatagramPacket somePacket = new DatagramPacket(buf, buf.length);
								dataSocket.receive(somePacket);
								System.out.println("recieved packet from ");
								analyzeUDPCommand(buf, somePacket);
							} else
								curLine = clientInputStream.readLine();
						} catch (java.net.SocketException e)
						{
							System.out.println("Client reset connection... closing client socket");
							
						}
					//	System.out.println("*******Message from user: " + curLine + " ***************");
						if (curLine != null)
							analyzeCommand(curLine, clientOutputStream);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println("*********Client closed connection. Closing server.***********");
						System.out.println("Exception Caught" + e.getMessage());
						this.closeServer();
						e.printStackTrace();
					}
					//Analyze commands.	
				}
			//}
	//	} catch (NullPointerException e)
	//	{
			
		//}
	}
	
	
	/**
	 * Checks all the queues and adds them to the hashtable after the server has done it's tick.
	 */
	
	public void checkQueues()
	{

			while (clientSocketQueue != null && !clientSocketQueue.isEmpty())
			{
					Tuple<String, Socket> curSocket = clientSocketQueue.remove();
					clientTable.put(curSocket.getObjA(), curSocket.getObjB());
					System.out.println("placed socket on client table");
			}
			
			while (clientInputQueue != null && !clientInputQueue.isEmpty())
			{
				Tuple<String, BufferedReader> curInputStream = clientInputQueue.remove();
				clientInputStreams.put(curInputStream.getObjA(), curInputStream.getObjB());
				
			}
			
			while (clientOutputQueue != null && !clientOutputQueue.isEmpty())
			{
				System.out.println("Taking from outputStream queue");
				Tuple<String, PrintWriter> curOutputStream = clientOutputQueue.remove();
				clientOutputStreams.put(curOutputStream.getObjA(), curOutputStream.getObjB());
				curOutputStream.getObjB().println("Hello");
			}
		
	}
	
	
	/*
	**Legacy code for supporting multiple clients.
	
	
	public Socket addClient(String name, Socket clientSocket) throws IOException
	{
		
		System.out.println("add client accepted");
		PrintWriter outputStream = new PrintWriter(clientSocket.getOutputStream(), true);
		BufferedReader inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientSocketQueue.add(new Tuple<String, Socket>(name, clientSocket));
			clientInputQueue.add(new Tuple<String, BufferedReader>(name, inputStream));
			clientOutputQueue.add(new Tuple<String, PrintWriter>(name, outputStream));
			
		
		
		System.out.println("ClientSocketQueue size" + clientSocketQueue.size());
		System.out.println("Accepted client");
		return clientSocket;

	}
*/
	
	
	/**
	 * Checks if the server is running
	 * @return Returns true if the server is running, false if it is not.
	 */
	
	public boolean isRunning() {
		return isRunning;
	}

	
	/**
	 * Gets the host name of the server. (Net IP)
	 * @return Returns the host name of the server
	 * @throws UnknownHostException Thrown if the server has not been setup.
	 */
	
	public String getHostName() throws UnknownHostException
	{
		
		return InetAddress.getLocalHost().getHostAddress();
		
	}

	
	/**
	 * Retrieves the port number of the udp socket
	 * @return Returns the port number of the udp socket. If the udp socket has not been initialized, it returns -1.
	 */
	
	public int getUDPPort()
	{
		if (dataSocket != null)
			return dataSocket.getPort();
		else 
			return -1;
	}
	
	/**
	 * Returns the server socket.
	 * @return Returns the socket that the server uses to communicate.
	 */
	
	public ServerSocket getServerSocket()
	{
		return serverSocket;
	}
	
	
	/**
	 * 
	 * @param time
	 */
	
	public void adjustPing(long time)
	{
		
		clientPing = time - timeSinceLastMessage;
		timeSinceLastMessage = System.currentTimeMillis();
		
	}
	
	
	/**
	 * Returns the client's ping
	 * @return
	 */
	
	public long getPing()
	{
		
		return clientPing;
		
	}
	
	/**
	 * A class to allow acceptions of clients and adds them to the server queues.
	 * Currently not used as the current application only requires 
	 * @author nickj
	 *
	 */
/*
 * Legacy code for supporting multiple clients.
	class AcceptThread extends Thread
	{
		public void run()
		{
			try {
				while (isRunning)
				{
				Socket clientSocket = serverSocket.accept();
				addClient(clientSocket.getInetAddress().getHostAddress(), clientSocket);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Acept thread has exited");
		}
	}
*/
	
	
	/**
	 * Returns the audio format the client is using to stream audio data.
	 * @return Returns the audio format as the object-type AudioFormat
	 */
	
	public AudioFormat getClientAudioFormat() {
		
		return clientAudioFormat;
	
	}

	
	/**
	 * Sets the client audio format using specific values (can be used by the server object when deciphering client commands).
	 * @param encoding The type of encoding
	 * @param sampleRate The sample rate of the audio
	 * @param sampleSizeInBits The sampleSize of the audio in bits
	 * @param channels The number of channels the audio uses
	 * @param frameSize The frame size of the audio
	 * @param frameRate The frame rate of the audio
	 */
	
	public void setClientAudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits, int channels, int frameSize, float frameRate)
	{
		
		clientAudioFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, false);
		audioPlayThread.setAudioFormat(clientAudioFormat);
	
	}
	
	
	/**
	 * Sets the audio line that the computer plays too write audio suing the specified audio format
	 * @param clientAudioFormat
	 * @throws LineUnavailableException 
	 */
	
	public void setMainCableInputLine(AudioFormat format) throws LineUnavailableException
	{
		
		if (Main.cableInputLine.isOpen())
			Main.cableInputLine.close();
		Main.cableInputLine = AudioSystem.getSourceDataLine(format);
		
	}
	
	
	/**
	 * Sets the client's audio streaming format
	 * @param clientAudioFormat The AudioFormat used to decipher the client's audio byte streaming.
	 */
	
	public void setClientAudioFormat(AudioFormat clientAudioFormat) {
		
		this.clientAudioFormat = clientAudioFormat;
		audioPlayThread.setAudioFormat(clientAudioFormat);

	}

	
	/**
	 * A class for starting the main server loop on a separate thread.
	 * @author nickj
	 *
	 */
	
	class StartServerThread extends Thread
	{
		private Server server;
		StartServerThread(Server server)
		{
			this.server = server;
		}
		public void run()
		{
			this.server.runServer();
		
		}
	}
	
	class AudioPlayThread extends Thread
	{
		private SourceDataLine dataLine;
		private Server server;
		private AudioFormat audioFormat;
		private SynchronousQueue<byte[]> byteBuffers;
		private LinkedList<byte[]> audioBuffer;
		private ConcurrentLinkedQueue<byte[]> pipeLineBuffer;
		private boolean startedPlaying = false;
		AudioPlayThread(SourceDataLine dataLine, Server server, AudioFormat format)
		{
			audioBuffer = new LinkedList<byte[]>();
			byteBuffers = new SynchronousQueue<byte[]>();
			pipeLineBuffer = new ConcurrentLinkedQueue<byte[]>();
			this.dataLine = dataLine;
			this.setServer(server);
			this.setAudioFormat(format);
		}
		
		public void run()
		{
			while(server.isRunning())
			{
			//	System.out.println("audio play thread running");
				this.playAudioBytes();
			}
		}
		
		/**
		 * The data line
		 * @return
		 */
		
		public SourceDataLine getDataLine() {
			return dataLine;
		}

		
		public void setDataLine(SourceDataLine dataLine) {
			this.dataLine = dataLine;
		}
		
		
		public Server getServer() {
			return server;
		}
		
		
		public void setServer(Server server) {
			this.server = server;
		}
		
		
		public AudioFormat setAudioFormat() {
			return audioFormat;
		}
		
		
		public void setAudioFormat(AudioFormat audioFormat) {
			this.audioFormat = audioFormat;
		}
		
		
		public SynchronousQueue<byte[]> getByteBuffers() {
			return byteBuffers;
		}
		
		
		public void setByteBuffers(SynchronousQueue<byte[]> byteBuffers) {
			this.byteBuffers = byteBuffers;
		}
		
		
		public boolean addAudioToQueue(byte[] data)
		{
			this.byteBuffers.offer(data);
			return true;
		}
		
		
		//server feeds audio 
		public boolean playAudioBytes()
		{
			
			long timePlayedFor = 0;

			if (Main.cableInputLine != null)
			{

				int placeHolderDelta = 20;
				
			//	int size = pipeLineBuffer.size();
				//there is a
				//System.out.println("buffer size: " + otherByteBuffer.size());
				if (!pipeLineBuffer.isEmpty() && pipeLineBuffer.size() > placeHolderDelta)
				{
					timePlayedFor = System.currentTimeMillis();
					//unload audio from pipeLineBuffer to aduioBuffer
					
					System.out.println("Unloading audio from pipeline buffer to audio buffer.");
					while(!pipeLineBuffer.isEmpty())
					{
						audioBuffer.add(pipeLineBuffer.remove());
					}
					
					//play audio
					System.out.println("Playing audio from audio buffer");
					while(!audioBuffer.isEmpty())
					{
						byte[] dataFromBuffer;
						dataFromBuffer = audioBuffer.remove();
						long curTime = System.currentTimeMillis();
						AudioFunctions.writeDataToLine(dataFromBuffer, Main.cableInputLine);
						System.out.println("Play time: " + (System.currentTimeMillis() - curTime));
						while (!pipeLineBuffer.isEmpty())
						{
							
							audioBuffer.add(pipeLineBuffer.remove());
							System.out.println("Adding bytes from pipeLine Buffer to audioBuffer while playing the audio\n The size of the audio buffer is now " + audioBuffer.size());
						}
					}
					System.out.println("Total time played for: " + (System.currentTimeMillis() - timePlayedFor));
				}
			} else
			{
					//System.out.println("ping ratio: " + server.pingRatio);
			}
			 

			//Print out the data.
			return true;
		}
		
	}
}




