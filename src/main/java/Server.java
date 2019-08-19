import Interpolation.Interpolation;
import helper.ByteConversion;
import jitter.SimpleJitterBuffer;
import rtp.RtpPacket;

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
	private SimpleJitterBuffer jitterBuffer;

	private Hashtable<String, Socket> clientTable = new Hashtable<String, Socket>();
	private Hashtable<String, PrintWriter> clientOutputStreams = new Hashtable<String, PrintWriter>();
	private int bufferSize = 64;
	private AudioPlayThread audioPlayThread;
	private boolean UDPRunning = false;
	private String regex = " ";
	private long timeSinceLastMessage;
	private long timeSinceLastPacket;
	private long clientPing = 0;
	
	private long pingRatio = 2;
	

	
	private AudioFormat clientAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);
	private StartServerThread serverStartThread = new StartServerThread(this);

	
	private boolean isRunning = true;
	
	public Server(int port)
	{
		portNumber = port;
		jitterBuffer = new SimpleJitterBuffer(400, 100);
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
				clientSocket = serverSocket.accept();
				clientOutputStream = new PrintWriter(clientSocket.getOutputStream(), true);
	            clientInputStream = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));

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
	
	private boolean verifyUID(String uid)
	{
		return true;
	}
	
	
	/**
	 * Analyze's a command set by a user.
	 * Valid commands are:
	 * @param line The user's input
	 * @param outputStream The output stream to write to
	 * @return Returns true if the command is valid.
	 */
	
	private boolean analyzeCommand(String line, PrintWriter outputStream)
	{
		
		
		int periodIndex =line.indexOf('.');
		this.adjustPing(System.currentTimeMillis());
		
		if (periodIndex <= 0 || periodIndex == line.length() - 1)
		{
			outputStream.println("Invalid Command: " + line);
			return false;
		}
		
		String command = line.substring(0, periodIndex);
		String subCommand = line.substring(periodIndex + 1);
		
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
			outputStream.println("Thank you for the audio bytes! (Old TCP implementation)");
			
			//if the number of buffers is > than the ping ratio, play the audio from one buffer
			
			//if the ping is greater than the audio rate, then create a buffer of twice the size and play from there.
			return true;
			 
		
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
	 * @param port The port number for the UDP socket
	 * @throws SocketException
	 */
	public void setupUDP(int port) throws SocketException
	{
		dataSocket = new DatagramSocket(port);
		UDPRunning = true;
	}
	



	public void processPacket(DatagramPacket packet)
	{

		//Construct RTP packet
		byte[] data = packet.getData();
		RtpPacket rtpPacket = new RtpPacket(data, data.length);
		//Send rtpPacket to jitter buffer
		jitterBuffer.write(rtpPacket);
		//Print out packet.

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
			processPacket(packet);
		}
	}
	
	/**
	 * Iterates the server one tick
	 */
	
	public void serverTick(Socket curSocket, PrintWriter curOutputStream, BufferedReader curInputStream) throws java.net.SocketException
	{
		//checkQueues();

		//try {
	
		String curLine = null;
			if (true)
			{
				try {
					try {
						//Anaylze UDP datagrams
						if(UDPRunning)
						{
							//TODO
							byte buf[] = new byte[824];
							DatagramPacket somePacket = new DatagramPacket(buf, buf.length);
							dataSocket.receive(somePacket);
							//System.out.println("recieved packet from ");
							analyzeUDPCommand(buf, somePacket);
						} else
							curLine = clientInputStream.readLine();
					} catch (java.net.SocketException e)
					{
						System.out.println("Client reset connection... closing client socket");

					}
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
	 * @param
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

				//Read jitterbuffer
				RtpPacket[] packets = new RtpPacket[0];
				try {
					packets = this.server.jitterBuffer.read();
					if (packets != null) {
						System.out.println("read packets from jitter buffer");
						this.playAudioBytes(packets);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}

		/**
		 * Writes rtpPackets that contain audio to the main audio line.
		 * @param packets
		 * @return
		 */
		public boolean playAudioBytes(RtpPacket[] packets)
		{

			if (Main.cableInputLine != null) {
				//interpolate
				LinkedList<byte[]> audioBytes = this.interpolatePackets(packets);

				//write audioBytes from list to data line.
				while (!audioBytes.isEmpty()) {
					byte[] data = audioBytes.remove();

					AudioFunctions.writeDataToLine(data, Main.cableInputLine);
				}
			}

			return true;
		}

		/**
		 * Takes an array of rtpPackets, converts to shorts, interpolates and then returns as bytes
		 * @param packets The packets to interpolate
		 * @return REturns an array of bytes containing the audio data.
		 */
		private LinkedList<byte[]> interpolatePackets(RtpPacket[] packets)
		{
			LinkedList<byte[]> byteList = new LinkedList<>();
			//Add bytes in pairs
			for (int i = 0; i < packets.length - 1; i += 2)
			{

				byteList.add(packets[i].getPayload());

				//Convert to short since we are currently dealing with 16bit pcm
				short[] firstShorts = ByteConversion.byteArrayToShortArray(packets[i].getPayload(), true);
				short[] secondShorts = ByteConversion.byteArrayToShortArray(packets[i + 1].getPayload(), true);

				//Interpolate between the byte pairs
				if (((packets[i+1].getSequenceNumber() - packets[i].getSequenceNumber()) - 1 ) > 0) {
					short[][] interpolatedBytes = Interpolation.interpolate(firstShorts, secondShorts, (packets[i + 1].getSequenceNumber() - packets[i].getSequenceNumber()) - 1);
					//increment size by num interpoalted bytes;
					for (int j = 0; j < interpolatedBytes.length; ++j)
						byteList.push(ByteConversion.shortArrayToByteArray(interpolatedBytes[j], true));
				}
				byteList.add(packets[i+1].getPayload());
			}


			return byteList;

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


	}
}




