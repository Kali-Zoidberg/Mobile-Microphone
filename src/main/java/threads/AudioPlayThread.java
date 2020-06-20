package threads;

import Interpolation.Interpolation;
import Network.PacketOrganizer;
import Network.Server;
import audio.AudioFunctions;
import helper.ByteConversion;
import jitter.SimpleJitterBuffer;
import resources.PacketDealer;
import rtp.RtpPacket;

import javax.sound.sampled.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

public class AudioPlayThread extends Thread {

    private SourceDataLine dataLine;
    private Server server;
    private AudioFormat audioFormat;
    private SynchronousQueue<byte[]> byteBuffers;
    private LinkedList<byte[]> audioBuffer;
    private ConcurrentLinkedQueue<byte[]> pipeLineBuffer;
    private boolean startedPlaying = false;
    private PacketOrganizer packetOrganizer = new PacketOrganizer();
    private FileWriter audioPlayThread;
    public SourceDataLine cableInputLine;

    public AudioPlayThread(SourceDataLine dataLine, Server server, AudioFormat format)
    {
        audioBuffer = new LinkedList<byte[]>();
        byteBuffers = new SynchronousQueue<byte[]>();
        pipeLineBuffer = new ConcurrentLinkedQueue<byte[]>();
        this.dataLine = dataLine;
        this.setServer(server);
        this.setAudioFormat(format);


        try {
            audioPlayThread = new FileWriter("AudioPlayThread.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Attempt to open
        try {
            this.openCableLine();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /*****
     *
     * README
     *  What I think is wrong -- I believe that the reordering is taking too long and thus the delay between writes is noticable
     *  If reorganization happens in the jitter buffer before releasing any resources or writing a new packet, then the audio line can write as soon as possible.
     *
     */
    public void run()
    {
        //audioBytes needs to be the same size as the jitter buffer
        //Some data is still lost due to the audio bytes size.
        byte[] audioBytes = new byte[this.server.getClumpSize() * this.server.getPayloadSize() * 12];//new byte[2097152];
        ArrayList<Byte> allAudioBytes = new ArrayList<>();
        int pid = 0;
        SimpleJitterBuffer serverJitterBuffer = null;
        int previousDiscardTime = -1;
        int currentDiscardTime = 0;
        int currentFillCount = -1;
        int posBeforeFlip = 0;
        int counter = 0;
        int totalBytesPlayed = 0;
        while(server.isRunning())
        {
            serverJitterBuffer = this.server.getJitterBuffer();

            currentDiscardTime = serverJitterBuffer.getDiscardTime();
            previousDiscardTime = currentDiscardTime;

            //Read jitterbuffer

            try {
                audioBytes = serverJitterBuffer.read();


                if (audioBytes != null && audioBytes.length > 0) {
//                    for (byte b : audioBytes)
//                        allAudioBytes.add(b);

                    this.playAudioBytes(audioBytes, pid++);

                    counter++;
//                    if (counter % 30 == 0) {
//                        byte[] primBytes = new byte[allAudioBytes.size()];
//                        for (int i = 0; i < primBytes.length; ++i)
//                            primBytes[i] = allAudioBytes.get(i);
//
//                        this.playAudioBytes(primBytes, 0);
//                    }
                    totalBytesPlayed += audioBytes.length;
//                    System.out.println("total audio bytes played: " + totalBytesPlayed);
//                    try {
//                        audioPlayThread.write((new PacketOrganizer()).byteArrToString(audioBytes));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    //Let's write the bytes to a file to compare
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
    public boolean playAudioBytes(byte[] packets, int pid)
    {
        int len = packets.length;
        byte[][] organizedPackets;
        if (this.cableInputLine != null) {


            //write audio bytes from organizedPackets Array to data line.

            //This could lead to out of order depending on queue for threads (blocking needs to be FIFO).
            //Create thread to write packets.
            long currentTime = System.nanoTime();

            AudioWriter writer = new AudioWriter(packets, this.cableInputLine);
            //probably need to synchronize play order.
//            System.out.println("Time to make new thread: " + (System.nanoTime() - currentTime) + "\n: " + pid);
            writer.run();
            //Start thread.
           // writer.start();



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
     * Opens the cable input line (Will be expanded to take audio format as parameters).
     * @throws LineUnavailableException
     */
    public void openCableLine() throws LineUnavailableException {
        String filename = "sup.wav";

        AudioInputStream testStream = null;
        try {
            testStream = AudioFunctions.createAudioInputStream(filename);
            Hashtable<String, Mixer> audioMixerTable = AudioFunctions.createHashTableOfMixers();
            Mixer cableinput = audioMixerTable.get("Headset Earphone (Corsair VOID PRO USB Gaming Headset )");
            this.cableInputLine = AudioFunctions.getLineFromDevice(testStream.getFormat(), cableinput.getMixerInfo());
            System.out.print(testStream.getFormat().toString());
            this.cableInputLine.open();
            this.cableInputLine.start();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

}
