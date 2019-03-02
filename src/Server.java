import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * To implement multiple, clientSocket = accept should be on a separate thread which willb e blocked until someone conents.
 * Once someone connects, they are added the the synchronous queue.
 * @author nickj
 *
 */

public class Server {
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private int portNumber;
	private PrintWriter clientOutputStream;
	private String directFile = "Direct.txt";
	private FileWriter stringFile = null;
	private BufferedReader clientInputStream;
	private Hashtable<String, Socket> clientTable = new Hashtable<String, Socket>();
	private Hashtable<String, PrintWriter> clientOutputStreams = new Hashtable<String, PrintWriter>();
	private Hashtable<String, BufferedReader> clientInputStreams =  new Hashtable<String, BufferedReader>();
	private LinkedList<Tuple<String, Socket>> clientSocketQueue = new LinkedList<Tuple<String, Socket>>();
	private LinkedList<Tuple<String, BufferedReader>> clientInputQueue =  new LinkedList<Tuple<String, BufferedReader>>();
	private LinkedList<Tuple<String, PrintWriter>> clientOutputQueue =  new LinkedList<Tuple<String, PrintWriter>>();
	private String regex = " ";
	private StartServerThread serverStartThread = new StartServerThread(this);

	
	private boolean isRunning = true;
	Server(int port)
	{
		portNumber = port;
		try {
			serverSocket = new ServerSocket(portNumber);
			
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
	 * Strats the server on a spearate thread.
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
	            System.out.println("accepted client");
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
			} catch (IOException e1) {
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
		String commands[] = {"A", "UID", "S"};
		
		Hashtable<String, String> commandDescription = new Hashtable<String, String>();
		Hashtable<String, String> subStringCommands = new Hashtable<String, String>();
		commandDescription.put("A", "Sends audio data using _ as a regex");
		commandDescription.put("UID", "Specifies the user's id so that the Server may verify it");
		commandDescription.put("S", "Specifies server commands.");
		subStringCommands.put("A", "No commands");
		subStringCommands.put("UID", "No sub commands");
		subStringCommands.put("S", "Sub commands are: \nclose - Closes the server.\ndisconnect - Disconnects the client from the server.");
		
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
	 * @param socket
	 * @param outputStream
	 * @return
	 */
	
	public boolean analyzeCommand(String line, PrintWriter outputStream)
	{
		System.out.println("analyzing.");
		int periodIndex =line.indexOf('.');
		
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
				System.out.println("VAlid uid");
				outputStream.println("Valid UID. Accepting client connection.");
				return true;
			} else
				return false;
				
		}
		if(command.equals("A"))
		{
			outputStream.println("Thank you for the audio bytes!");
			if(playAudioBytes(subCommand))
			{
				outputStream.println("Those were valid audio bytes!");
				return true;
			}
			else
			{
				outputStream.println("Those were invalid audio bytes.");
				return false;
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
		}
		return true;
		
	}
	
	/**
	 * Processes audio bytes and plays them to Main's cableinput line. This method will most likely be changed as the GUI evolves
	 * @param strBytes
	 * @return
	 */
	
	public boolean playAudioBytes(String strBytes)
	{
		try {
			stringFile.write(strBytes);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[] data = AudioFunctions.getBytesFromString(strBytes, regex);
		if (Main.cableInputLine != null)
		{
			
			AudioFunctions.writeDataToLine(data, Main.cableInputLine);
		}
		
	
		//Print out the data.
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
	
	/**
	 * Iterates the server one tick
	 */
	
	public void serverTick(Socket curSocket, PrintWriter curOutputStream, BufferedReader curInputStream) throws java.net.SocketException
	{
		//checkQueues();

		//try {
	
	//	Set<String> names = clientTable.keySet();
		String curLine = "";
	//		for (String name : names)
	//		{
				//System.out.println("Checking clients");
					
			//	Socket curSocket = clientTable.get(name);
			//	PrintWriter curOutputStream = clientOutputStreams.get(name);
			//	BufferedReader curInputStream = clientInputStreams.get(name);
				if (clientInputStream != null && clientOutputStream != null && !clientSocket.isClosed())
				{
					try {
						System.out.println("*****Waiting to Read line from User*******");
						try {
						curLine = clientInputStream.readLine();
						} catch (java.net.SocketException e)
						{
							System.out.println("Client reset connection... closing client socket");
							
						}
						System.out.println("*******Message from user: " + curLine + " ***************");
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

	
	public ServerSocket getServerSocket()
	{
		return serverSocket;
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
}




