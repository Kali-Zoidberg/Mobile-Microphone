

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.*;
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
	
	public static void main (String args[])
	{
			//testByteStringConversion();
		testClientServer();
	}
	
	public static void testClientServer()
	{
		int port = 400;
		
		Server server = new Server(port);
		String filename = "darude.wav";
		try {
			System.out.println(server.getHostName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AudioInputStream testStream = null;
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);

			cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
			cableInputLine.open();
			cableInputLine.start();
			//AudioFunctions.writeFromStreamToLine(testStream, cableInputLine, 10, 1024, 1024);

			server.startServer();
			/*
			Client client = new Client("0.0.0.0", port);
			try {

				client.connectToServer();
				
				client.sendDataToServer("UID.asdf");
				Thread.sleep(2000);
				System.out.println(client.readMessageFromSerer());
				int bytesPerRead = 1024;
				int maxReadSize = 1024;
				int bytesRead = 0;
				int numBytesRead = 0;
				byte data[] = new byte[bytesPerRead];

			
				while (bytesRead < maxReadSize)
				{
					try {
						
						numBytesRead = testStream.read(data, 0, bytesPerRead);
						if (numBytesRead == -1) break;
						bytesRead += bytesRead;

						String strData = "";
						for (int i = 0; i < data.length - 1; ++i)
						{
							strData += Byte.toString(data[i]) + " ";
						
						}
						strData += Byte.toString(data[data.length - 1]);
						
						client.sendDataToServer("A." + strData);
						
					} catch (IOException e) {
						// TODO Cleanup if needed.
						e.printStackTrace();
						
					}
				}
				
				System.out.println(client.readMessageFromSerer());
				client.sendDataToServer("S.close");
				System.out.println(client.readMessageFromSerer());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e)
			{
				server.closeServer();
			}	*/
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
		String filename = "missionimpossible.wav";
		int bufferSize = 1024;
		int port = 4000;
		AudioInputStream testStream;
		
		
	
		try {
			testStream = AudioFunctions.createAudioInputStream(filename);
			try {
				
				Hashtable<String, Mixer> audioMixerTable = AudioFunctions.createHashTableOfMixers();
				Mixer cableinput = audioMixerTable.get("CABLE Input (VB-Audio Virtual Cable)");
 				 cableInputLine = AudioFunctions.getLineFromDevice(testStream.getFormat(), cableinput.getMixerInfo());
 				cableInputLine = AudioSystem.getSourceDataLine(testStream.getFormat());
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
