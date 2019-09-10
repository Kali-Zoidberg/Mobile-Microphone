package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	private Socket serverSocket = null;
	private DatagramSocket udpSocket = null;
	private int portNumber;
	private String hostName;
	private PrintWriter outputStream;
	private BufferedReader inputStream;
	
	public Client(String hostName, int port)
	{
		this.setPortNumber(port);
		this.setHostName(hostName);
	}
	
	public Socket getSocket()
	{
		return serverSocket;
	}
	
	/**
	 * 
	 * @return
	 */
	
	public int getPortNumber()
	{
		return portNumber;
	}
	
	/**
	 * Sets the port number to the specified value
	 * @param port The port number to connect to.
	 * @return Returns false if the port number is invalid (less than 0).
	 */
	
	public boolean setPortNumber(int port)
	{
		if (port < 0)
		{
			System.out.println("Error, port is an invalid value: " + port);
			return false;
		} else
		{
			portNumber = port;
			return true;
		}
	}

	
	/**
	 * Returns the host name the client is trying to connect to.
	 * @return
	 */
	
	public String getHostName() {
		return hostName;
	}

	
	/**
	 * Sets the host name of the server that the client is to connect to.
	 * @param hostName
	 */
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	/**
	 * Receives incoming bytes from a udp server.
	 * @param buf The buffer to store the bytes in
	 * @return Returns the packet's data
	 * @throws IOException 
	 */
	
	public byte[] recieveBytesFromUDP(byte[] buf) throws IOException
	{
		if (udpSocket != null)
		{
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			udpSocket.receive(packet);
			return packet.getData();
		} else
		{
			return null;
		}
	}
	
	/**
	 * Sends specified bytes to the udp server the client is connected to.
	 * @param buffer The byte array to send to the server.
	 * @throws IOException You should handle this exception yourself in case the bytes cannot be accepted.
	 */
	
	public void sendBytesToUDP(byte[] buffer) throws IOException
	{
		if(udpSocket != null)
		{
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.serverSocket.getLocalAddress(), this.portNumber);
			udpSocket.send(packet);
		}
	}
	
	
	/**
	 * Starts the udp socket on the server.
	 * @throws SocketException 
	 */
	public void connectToUDPServer() throws SocketException
	{
		this.sendDataToServer("S.udp");
		udpSocket = new DatagramSocket();
	}
	

	
	/**
	 * Connects the client to the specified server
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	
	public void connectToServer() throws UnknownHostException, IOException
	{
		System.out.println("creating server socket");
		serverSocket = new Socket(hostName, portNumber);
		this.setupInputStream();
		this.setupOutputStream();
		System.out.println("connected to server");
	}
	
	
	/**
	 * Connects the client to the specified server. 
	 * @param hostName The host name to connect to.
	 * @param portNumber The port number to connect to.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	
	public void connectToServer(String hostName, int portNumber) throws UnknownHostException, IOException
	{
		serverSocket = new Socket(hostName, portNumber);
	}
	
	
	/**
	 * Sets up the output stream so long as the client has already been connected to the server.
	 * @return
	 */
	
	public PrintWriter setupOutputStream()
	{
		if (serverSocket == null)
			return null;
		else
		{
			try {
				outputStream = new PrintWriter(serverSocket.getOutputStream(), true);
			} catch (IOException e) {
				System.out.println("Error creating output stream.");
				e.printStackTrace();

				return null;
			}
			return outputStream;
		}
	}
	
	
	/**
	 * Sets up the input stream for the client.
	 * @return Returns null if the client is not connected or there was an error creating the input stream.
	 */
	
	public BufferedReader setupInputStream()
	{
		if (serverSocket == null)
			return null;
		else
		{
			
			try {
				inputStream = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
				return inputStream;
			} catch (IOException e) {
				System.out.println("Error creating input stream");
				e.printStackTrace();
				return null;
			}
		
				
		}
	}
	
	
	/**
	 * Reads a message form the server. 
	 * @return if the server closes unexplectedly, the method will return null. Otherwise, it returns a mesage from the server.
	 */
	
	public String readMessageFromSerer()
	{
		try {
			return inputStream.readLine();
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Sends data to a server.
	 * @param data
	 */
	
	public void sendDataToServer(char[] data)
	{
		System.out.println("sending: " + data.toString());
		outputStream.print(data);
		
	}
	
	
	/**
	 * Sends data to a server
	 * @param data
	 */
	public void sendDataToServer(String data)
	{
		outputStream.println(data);
	}
	
}
