

import javax.media.MediaLocator;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
import javax.media.rtp.RTPSocket;
import javax.media.rtp.RTPStream;
public class Main {
	
	public static SourceDataLine cableInputLine;
	private static String hostname = "192.168.56.1";
	private static String port = "6209";
	private static String audioFile = "file:/C:/users/nickj/Documents/CS122b/mobilemic/sup.wav";
	//private static String  audioFile = "javasound://0";

	public static void main (String args[])
	{
			//testByteStringConversion();
		//testClientServer();
		//testAudioFiles();

		testRTPServer();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("hiya giya hgiya");
		testRTPClient();
		RTPSocket socket = new RTPSocket();
	}
	public static void testRTPServer()
	{
		Thread run = new Thread() {
			@Override
			public void run() {
				String[] listeners = {hostname + "/" + port, hostname + "/" + "6093"};
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
		RTPClient client = new RTPClient(new MediaLocator(audioFile), "192.168.56.1", port);
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
			//AudioFunctions.writeFromStreamToLine(testStream, cableInputLine, 10, 1024, 1024);
			
			server.startServer();
			
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
		String filename = "finalcountdown.wav";
		int bufferSize = 1024;
		int port = 4000;
		AudioInputStream testStream;
	
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);
			try {
				
				Hashtable<String, Mixer> audioMixerTable = AudioFunctions.createHashTableOfMixers();
				Mixer cableinput = audioMixerTable.get("CABLE Input (VB-Audio Virtual Cable)");
 				cableInputLine = AudioFunctions.getLineFromDevice(testStream.getFormat(), cableinput.getMixerInfo());
 				//cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
 				//Audiosystem.getsource dataline allows specificaiton
 				cableInputLine.open();
				AudioFunctions.writeFromStreamToLine(testStream, cableInputLine, 10, 1024, 1024);
				
				
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
