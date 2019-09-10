

import Network.Client;
import Network.Server;
import audio.AudioFunctions;
import resources.PacketDealer;
import rtp.RtpPacket;

import javax.media.MediaLocator;
import javax.sound.sampled.*;

import java.io.*;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;

/**
 * Can use mixer info to get a clip to stream audio data too. clip extends dataline!
 * Okay next order of buisness,
 * either do client server or connect using a phone.
 * @author nickj
 *
 */

public class Main {
	
	public static SourceDataLine cableInputLine;
	private static String hostname = "0.0.0.0";
	private static String port = "9000";
	private static String audioFile = "rtp://0.0.0.0:" + port + "/audio";
	//private static String  audioFile = "javasound://0";

	public static void main (String args[])
	{
		startServer(Integer.parseInt(port));
		//Testers tester = new Testers();
		//tester.main();

		//testAudioFiles();
		/*
		try {
			CaptureDeviceManager.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Vector devices = CaptureDeviceManager.getDeviceList(null);

		System.out.println("Printing names of devices of :"  + devices.size() + " devices");
		for (Object device: devices)
		{
			System.out.println("Hiya");
			CaptureDevice convDevice = (CaptureDevice) device;
			System.out.println(convDevice.getCaptureDeviceInfo().getName());
		}
*/
		//testPacketHeader();
			//testByteStringConversion();
		//testClientServer();
		//testAudioFiles();

		/*
		testRTPServer();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("hiya giya hgiya");
		testRTPClient();
		RTPSocket socket = new RTPSocket();
*/
		/*
		try {
			testUDPServer();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		*/
	}

	public static void startServer(int port) {

		Server server = new Server(port);
		server.start();
		//PacketDealer.initializer(port);
	}


	public static void testUDPServer() throws LineUnavailableException {
		int udpPort = 6914;

		System.out.println("Starting udp server...");
		Server server = new Server(6915);
		try {
			server.setupUDP(udpPort);

			System.out.println("UDP open on port: " + udpPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		server.start();
	}

	@SuppressWarnings("Duplicates")
	public static void testPacketHeader()
	{
		String filename = "sup.wav";
		int payloadType = 17; // dvi4
		int seqNum = 0;
		AudioInputStream testStream = null;
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);
			System.out.println(testStream.getFormat().toString());
			System.out.println(AudioFunctions.getAudioPlayTime(testStream.getFormat(), 32768));
			System.out.println("Audio data rate" + AudioFunctions.getDataRate(testStream.getFormat()));
			//print frameRAte
			cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
			cableInputLine.open();
			cableInputLine.start();

			try {
				//noinspection Duplicates
				int bytesPerRead = 8012;
				int maxReadSize = 8012;
				int bytesRead = 0;
				int numBytesRead = 0;
				byte data[] = new byte[bytesPerRead];


				while (bytesRead < maxReadSize) {
					try {
						numBytesRead = testStream.read(data, 0, bytesPerRead);

						if (numBytesRead == -1) break;
						bytesRead += bytesRead;
						RtpPacket packet = new RtpPacket(payloadType, seqNum, (int) System.currentTimeMillis(), data);
						packet.printHeader();
						long curTime = System.currentTimeMillis();
						Thread.sleep(100);

					} catch (IOException e) {
						// TODO Cleanup if needed.
						e.printStackTrace();

					}
					seqNum++;
				}

			} catch (Exception e)
			{
				e.printStackTrace();
			}

			} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	public static void testRTPServer()
	{
		Thread run = new Thread() {
			@Override
			public void run() {
				String[] listeners = {hostname + "/" + port, hostname +"/" + (Integer.parseInt(port) + 10)};

				RTPServer rtpServer = new RTPServer(listeners);
				if (!rtpServer.initialize())
				{
					System.err.println("Error intializing session.");
					System.exit(-1);
				}

				try {
					while (!rtpServer.isDone())
						Thread.sleep(1000);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		run.start();

	}

	public static void testRTPClient()
	{
		RTPClient client = new RTPClient(new MediaLocator(audioFile), "0.0.0.0", port);
		System.out.println("Strating client....");
		String result = client.start();
		System.out.println("client result: " + result);

		try {
			Thread.currentThread().sleep(60000);
		} catch (InterruptedException ie) {
		}

		// Stop the transmission
		client.stop();
		System.out.println("transmission ended...");
	}



	public static void testClientServer()
	{
		int port = 7000;
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		Server server = new Server(port);
		String filename = "sup.wav";
		try {
			System.out.println(server.getHostName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AudioInputStream testStream = null;
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);
			System.out.println(testStream.getFormat().toString());
			System.out.println(AudioFunctions.getAudioPlayTime(testStream.getFormat(), 32768));
			System.out.println("Audio data rate" + AudioFunctions.getDataRate(testStream.getFormat()));
			//print frameRAte
			cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
			cableInputLine.open();
			cableInputLine.start();
			//audio.AudioFunctions.writeFromStreamToLine(testStream, cableInputLine, 10, 1024, 1024);
			
			server.start();

			Client client = new Client("0.0.0.0", port);
			try {

				client.connectToServer();
				client.sendDataToServer("UID.asdf");

				Thread.sleep(2000);
				System.out.println(client.readMessageFromSerer());
				int bytesPerRead = 8012;
				int maxReadSize = 8012;
				int bytesRead = 0;
				int numBytesRead = 0;
				byte data[] = new byte[bytesPerRead];
				

				client.connectToUDPServer();
				while (bytesRead < maxReadSize)
				{
					try {
						numBytesRead = testStream.read(data, 0, bytesPerRead);
						
						if (numBytesRead == -1) break;
						bytesRead += bytesRead;
						
						long curTime = System.currentTimeMillis();

						client.sendBytesToUDP(data);
						Thread.sleep(100);

					} catch (IOException e) {
						// TODO Cleanup if needed.
						e.printStackTrace();
						
					}
				}
				data = "end".getBytes();
				client.sendBytesToUDP(data);
				System.out.println(client.readMessageFromSerer());

				client.sendDataToServer("S.close");
				System.out.println(client.readMessageFromSerer());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e)
			{
				server.closeServer();
				e.printStackTrace();
				
			}	
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
		
		
	}
	
	
	public static void testByteStringConversion()
	{
		String str = "-9";
		System.out.println("Testing string conversion");
		String[] strTests = {"-127 -10 -11 3", "3 1 127"};
		byte[][] correctStringBytes = {{-127,-10,-11,3}, {3,1,127}};
		int tests = 0;
		int errors = 0;
		for(int i = 0; i < strTests.length; ++i)
		{
			byte[] data = AudioFunctions.getBytesFromString(strTests[i], " ");
			for (int j = 0; j < correctStringBytes[i].length; ++j)
			{
				++tests;

				if (data[j] != correctStringBytes[i][j])
					System.out.println("Invalid byte conversion. \n Invalid byte: " + data[j] + " Correct Byte" + correctStringBytes[i][j] + "");
			}
		}
		System.out.println("# of Tests: " + tests + "# of Errors: " + errors + "\nSuccess rate: " + ((tests - errors) / tests) * 100 + "%");
	}
	
	public static void testAudioFiles()
	{
		String filename = "darude.wav";
		int bufferSize = 1024;
		int port = 4000;
		AudioInputStream testStream;
	
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);
			try {
				
				Hashtable<String, Mixer> audioMixerTable = AudioFunctions.createHashTableOfMixers();
				Mixer cableinput = audioMixerTable.get("Speakers (Realtek High Definition Audio)");
 				cableInputLine = AudioFunctions.getLineFromDevice(testStream.getFormat(), cableinput.getMixerInfo());
 				System.out.println(testStream.getFormat().toString());
 				//cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
 				//Audiosystem.getsource dataline allows specificaiton
 				cableInputLine.open();
				AudioFunctions.writeFromStreamToLine(testStream, cableInputLine, 1, 4, 4);
				
				
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void testPrintAudioDevice()
	{
		String[] audioDeviceNames = AudioFunctions.getAudioDeviceNames();
		
		for (String str : audioDeviceNames)
			System.out.println(str);
	}
}
