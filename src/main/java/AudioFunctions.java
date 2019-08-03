import Interpolation.Interpolation;
import helper.ByteConversion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.util.zip.Deflater;

import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFunctions {

	private static Deflater encoder = new Deflater();
	
	/**
	 * Creates a Source line with the specified audio format and opens it with a specified buffer size.
	 * @param format The specified audio format for the data line
	 * @param bufferSize The buffer size
	 * @return Returns an SourceDataLine that has been opened with the specified audioFormat and buffer size
	 * @throws LineUnavailableException
	 */
	
	public static SourceDataLine setupSourceDataLine(AudioFormat format, int bufferSize) throws LineUnavailableException
	{
		SourceDataLine audioLine = AudioSystem.getSourceDataLine(format);
		return audioLine;
	}
	
	
	/**
	 * Drains, stops, and closes the source data line
	 * @param line the data lien to close.
	 */
	
	public void closeSourceDataLine(SourceDataLine line)
	{
		line.drain();
		line.stop();
		line.close();
	}
	
	
	/**
	 * Creates an Audio Input Stream with the specified filename.
	 * @param filename
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	
	public static AudioInputStream createAudioInputStream(String filename) throws UnsupportedAudioFileException, IOException
	{
		File file = new File(filename);
		AudioInputStream stream = AudioSystem.getAudioInputStream(file);
		return stream;
		
	}
	
	
	/**
	 * Returns all Mixers that exist on the user's PC
	 * @return Returns Mixers as an array.
	 */
	
	public static Mixer[] getAudioMixers()
	{
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		Mixer[] mixers = new Mixer[mixerInfos.length];
		for (int i = 0; i < mixerInfos.length; ++i)
		{
			mixers[i] = AudioSystem.getMixer(mixerInfos[i]);
		}
		return mixers;
	}
	
	
	/**
	 * Returns the names of all the audio devices
	 * @return Returns a string listing the names of all the Audio Devices.
	 */
	
	public static String[] getAudioDeviceNames()
	{
		 Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		 String[] mixerNameStrings = new String[mixers.length];
		 int len = mixers.length;
		    for (int i = 0; i < len; ++i) 
		    {
		    	Mixer.Info info = mixers[i];
		    

		        mixerNameStrings[i] = info.getName();
		       
		    }
		   return mixerNameStrings; 
	}
	
	
	public static Line[] getLinesFromDevice(Mixer.Info info)
	{
		Mixer device = AudioSystem.getMixer(info);
		return device.getSourceLines();
	}
	
	
	/**
	 * Returns a dataline from a specified device	
	 * @param info
	 * @return
	 * @throws LineUnavailableException
	 */
	
	public static SourceDataLine getLineFromDevice(AudioFormat format, Mixer.Info info) throws LineUnavailableException
	{
		return AudioSystem.getSourceDataLine(format, info);
	}
	
	
	/*
	 * Data rate = sampleRate * frameSize (in bytes) * numChannels
	 * To get audio play time per message, numBytesInMessage/dataRate = time
	 */
	
	/**
	 * Returns the data rate of an audio format (bytes/second)
	 * @param format The audio format to extract the data rate from
	 * @return REturns the data rate of an audio format (bytes/second)
	 */
	
	public static long getDataRate(AudioFormat format)
	{
		return (long) (format.getSampleRate() * (format.getSampleSizeInBits() / 8) * format.getChannels());
	}
	
	
	/**
	 * Returns the audio playtime 
	 * @param format The audio format to extract the audio play time from
	 * @param bytesOfData The bytes of data that is given per write
	 * @return Returns a float representing the number of seconds that the an audio of specified format and bytes of data will play.
	 */
	
	public static float getAudioPlayTime(AudioFormat format, int bytesOfData)
	{
		return (float) bytesOfData / getDataRate(format);
	}
	
	/**
	 * Creates a hashtable of the mixer devices using the key as the name
	 * @return Returns a hashtable with the key as the name and the mixer as the value
	 */
	
	public static Hashtable<String, Mixer> createHashTableOfMixers()
	{
		Hashtable<String, Mixer> retHashTable = new Hashtable<String, Mixer>();
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info info : mixerInfos) {
			System.out.println(info.getName());
			retHashTable.put(info.getName(), AudioSystem.getMixer(info));
		}
		return retHashTable;
		
	}
	
	
	public static void writeDataToLine(byte[] data, SourceDataLine outputLine)
	{
		outputLine.write(data, 0, data.length);
	}
	
	/**
	 * 	 * THIS METHOD SHOULD BE STARTED ON A SEPARATE THREAD!!!

	 * @param inputStream
	 * @param outputLine
	 * @param readSize
	 * @param maxReadSize
	 * @param bytesPerRead
	 * @return
	 */
	
	public static boolean writeFromStreamToLine(InputStream inputStream, SourceDataLine outputLine, int readSize, int maxReadSize, int bytesPerRead)
	{

		int interpolationSize = 2;
		FileWriter testFile = null;
		try {
			testFile = new FileWriter("testFile.txt");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		outputLine.start();
		ArrayList<short[]> audioDataList = new ArrayList<>();
		int bytesRead = 0;
		int numBytesRead = 0;
		int testCounter = 0;


		byte data[] = new byte[bytesPerRead];
		short[] startArray = null;
		short[] endArray = null;
		while (bytesRead < maxReadSize)
		{
			try {
				++testCounter;
				numBytesRead = inputStream.read(data, 0, bytesPerRead);
				if (numBytesRead == -1) break;
				bytesRead += bytesRead;

				if (testCounter % interpolationSize == 0)
					audioDataList.add(ByteConversion.byteArrayToShortArray(data, true));
				//outputLine.write(data, 0, numBytesRead);

				//testFile.write(data[data.length - 1] + "\n");

			} catch (IOException e) {
				// TODO Cleanup if needed.
				e.printStackTrace();
				
				return false;
			}
			
		}



		ArrayList<byte[]> audioBytes = new ArrayList<>();
		//This won't play the last line of shorts.


		//Write the interpolated bytes to a temporary array.
		for (int i = 0; i < audioDataList.size() - 1; ++i)
		{
			byte[] shortToByte = ByteConversion.shortArrayToByteArray(audioDataList.get(i));
			short[][] interpolatedShorts = Interpolation.linearInterpolation(audioDataList.get(i), audioDataList.get(i + 1), interpolationSize - 1);
			audioBytes.add(shortToByte);
			//outputLine.write(ByteConversion.shortArrayToByteArray(audioDataList.get(i)), 0, shortToByte.length);
			try {
				testFile.write(shortToByte[shortToByte.length - 1] + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int j = 0; j < interpolationSize - 1; ++ j)
			{
				//convert the interpolated bytes.
				shortToByte = ByteConversion.shortArrayToByteArray(interpolatedShorts[j]);
				try {
					testFile.write(shortToByte[shortToByte.length - 1] + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				audioBytes.add(shortToByte);
			//	outputLine.write(shortToByte, 0, shortToByte.length);
			}

		}

		for (int i = 0; i < audioBytes.size(); ++i)
		{
			outputLine.write(audioBytes.get(i), 0, audioBytes.get(i).length);
		}
		try {
			testFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	/**
	 * Splits a string of data between some specified regex and converts each substring into a byte.
	 * @param str The string to split
	 * @param regex The regex to split the string by.
	 * @return Returns the bytes from the string.
	 */
	public static byte[] getBytesFromString(String str, String regex)
	{
		String[] stringBytes = str.split(regex);
		int len = stringBytes.length;
		byte[] data = new byte[len];
		for (int i = 0; i < len; ++i)
			data[i] = Byte.parseByte(stringBytes[i]);
		return data;
	}
	
	public static Tuple<byte[], Integer> encodeAudio(byte[] data)
	{
		encoder.setInput(data);
		encoder.finish();
		Integer compressedDataLen = new Integer(encoder.deflate(data));
		return new Tuple<byte[], Integer>(data, compressedDataLen);
	}

}
