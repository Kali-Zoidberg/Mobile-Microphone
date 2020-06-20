package jitter;

import Network.PacketOrganizer;
import audio.AudioFunctions;
import resources.PacketDealer;
import rtp.RtpPacket;
import threads.AudioPlayThread;

import javax.sound.sampled.AudioFormat;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleJitterBuffer {

    //Max amount of wiggle room before discard a packet.
    private int discardTime = 400;

    private int bufSize = 100;
    private int payloadSize;
    private int clumpSize;
    private int currentFillCount = 0;
    private long numBytesRead = 0;
    private long timesReturnedNull = 0;
    private long timesReturedNonNull = 0;
    private boolean startCounting = false;
    private String position = "";
    private String remaining = "";
    private String capacity = "";
    private String actions = "";
    private int lastSequenceNumber = -1;
    public int counter = 0;
    public ArrayList<Byte> allAudioBytes = new ArrayList<>();
    private long timeBetweenWrites = 0;

    ReentrantLock lock = new ReentrantLock();

    PacketOrganizer packetOrganizer = new PacketOrganizer();
    FileWriter jitterFile;
    private LinkedList<RtpPacket> overflow;
    private ArrayList<RtpPacket> queue;
    private ByteBuffer playableAudioBuffer;
    private ByteBuffer readOnly;
    private int currentIter = 0;
    private boolean isReading;
    private boolean isWriting;
    private boolean isFull;
    private long numReads = 0;
    private long numWrites = 0;

    FileWriter numBytesFile;

    public SimpleJitterBuffer(int discardTime, int bufSize, int clumpSize, int payloadSize) {
        packetOrganizer.createFile("PacketOrganizer.txt");
        this.currentFillCount = -1;
        this.discardTime = discardTime;
        this.bufSize = bufSize;
        this.overflow = new LinkedList<>();
        this.queue = new ArrayList<>();

        this.playableAudioBuffer = ByteBuffer.allocate(bufSize * (payloadSize * this.clumpSize));
        this.clumpSize = clumpSize;
        this.payloadSize = payloadSize;
        try {
            this.jitterFile = new FileWriter("Jitterbuffer.txt");
            this.numBytesFile = new FileWriter("Jitter Buffer Number of Audio Bytes.txt");
            this.numBytesFile.write("Start ---");
            this.jitterFile.write("Start");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public byte[] read() throws InterruptedException {
        //pass in the thread, make it wait.
        synchronized (this.playableAudioBuffer) {
            int bufferSize = this.playableAudioBuffer.position();
            this.numReads++;

            //not enough packets, return null
            if (bufferSize < this.payloadSize * this.clumpSize) {
                if (startCounting) {
                    ++timesReturnedNull;
                    if (this.playableAudioBuffer.position() > 0)
                        System.out.println("position: " + this.playableAudioBuffer.position());
                }

                return null;
            }

//            System.out.println("Not null: "  +System.currentTimeMillis());
            byte[] playableAudioBytes = new byte[bufferSize];
            try {
//                startCounting = true;
//                ++timesReturedNonNull;
//                    System.out.println("Reader attempting to aquire lock: " + this.playableAudioBuffer.position());
                this.lock.lock();

                long currentTime = System.nanoTime();
//                    ByteBuffer copyBuffer = playableAudioBuffer.duplicate();
//                    System.out.println("duplicate time: " + (System.nanoTime() - currentTime));
//                    currentTime = System.nanoTime();

                this.playableAudioBuffer.flip();

                this.playableAudioBuffer.get(playableAudioBytes, 0, playableAudioBytes.length);

                this.playableAudioBuffer.clear();
//                    System.out.println("bulk read time: " + (System.nanoTime() - currentTime));

                //                    this.playableAudioBuffer.position(this.playableAudioBuffer.position() + playableAudioBytes.length);
//                    byte[][] app = {playableAudioBytes};
//                    packetOrganizer.print2DArray(app);
            } finally {
                this.lock.unlock();
//                    System.out.println("Successfully unlocked the lock!");
//                    System.out.println("new position: " + this.playableAudioBuffer.position());
//            System.out.println("NonNull: " + timesReturedNonNull);
//            System.out.println("Null: " + timesReturnedNull);
                return playableAudioBytes;
            }

        }
    }


    public void write(RtpPacket rtpPacket) throws InterruptedException {
        //For simple, if it's out of order, discard.
        //block reader from reading
        long startTime = System.currentTimeMillis();

        synchronized (this.playableAudioBuffer) {

            lastSequenceNumber = rtpPacket.getSequenceNumber();
            queue.add(rtpPacket);

            if (queue.size() >= this.clumpSize) {



                    //block
                    this.lock.lock();

                byte[][] reorderedBytePackets = null;
                try {
                    //remove packets from queue
                    RtpPacket[] unorderedPackets = new RtpPacket[this.clumpSize];


                    for (int i = 0; i < this.clumpSize; ++i) {

                        unorderedPackets[i] = queue.remove(0);
                        //Add num bytes
                        this.numBytesRead += unorderedPackets[i].getPayloadLength();
                    }


//                    try {
//                        System.out.println("i: " + (numWrites++) + " -- " + this.numBytesRead);

//                        this.numBytesFile.write("i: " + (numWrites++) + " -- " + this.numBytesRead);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    //reorder packets
                    reorderedBytePackets = this.packetOrganizer.reorder(unorderedPackets, this.clumpSize);
                    //if full, discard or allocate new buffer
                    if (this.playableAudioBuffer.position() != 0 && this.playableAudioBuffer.remaining() < (this.payloadSize * this.clumpSize)) {
                        System.out.println("Aduio buffer full. releasing lock and returning" + this.playableAudioBuffer.position());

                        System.out.println("numReads: " + numReads);
                        this.playableAudioBuffer.clear();
                        System.out.println("Aduio buffer full. releasing lock and returning" + this.playableAudioBuffer.position());

                        this.currentFillCount += 1;
//                    this.lock.unlock();
                        return;
                    }

                    timeBetweenWrites = startTime;

                    //not full, place into byte buffer
                    for (int i = 0; i < reorderedBytePackets.length; ++i) {
                        if (this.playableAudioBuffer.capacity() == 0){
                            System.out.print(" i: " + this.playableAudioBuffer.capacity());

                            this.playableAudioBuffer = ByteBuffer.allocate(bufSize * (payloadSize * this.clumpSize));
                        }
                        this.playableAudioBuffer.put(reorderedBytePackets[i]);
//                    for(Byte b : reorderedBytePackets[i])
//                        allAudioBytes.add(b);
                    }
                    counter++;

                    //Streamline 20 clumps to determine if all audio is going from client->server
//                if (counter == 20)
//                {
//                    AudioFormat clientAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);
//                    AudioPlayThread playThread = new AudioPlayThread(null,null, clientAudioFormat);
//                    byte[] primBytes = new byte[allAudioBytes.size()];
//                    for (int i = 0; i < primBytes.length; ++i)
//                        primBytes[i] = allAudioBytes.get(i);
//
//                    playThread.playAudioBytes(primBytes, 0);
//                }


                } catch (BufferOverflowException e) {
                    e.printStackTrace();
                    //Clear buffer if overflow
                    this.playableAudioBuffer.clear();

                } finally {
                    //unblock
                    this.lock.unlock();
                }

                long endTime = System.currentTimeMillis();

//                System.out.println("Time difference: " + (endTime - startTime));
            }

            ++currentIter;

        }
        
    }


    private synchronized  void setFull(boolean isFull) { this.isFull = isFull; }
    private synchronized  void setWriting(boolean isWriting) { this.isWriting = isWriting; }
    private synchronized void setReading(boolean reading)
    {
        this.isReading = reading;
    }

    private synchronized boolean getReading()
    {
        return isReading;
    }

    public int getCurrentFillCount() {
        return currentFillCount;
    }

    private synchronized boolean getWriting()
    {
        return isWriting;
    }
    private synchronized  boolean getFull()
    {
        return isFull;
    }
    public int getDiscardTime() {
        return discardTime;
    }

    public boolean isReading() {
        return isReading;
    }

    public int getBufSize() {
        return bufSize;
    }

    public FileWriter getJitterFile() {
        return jitterFile;
    }

    public ByteBuffer getReadOnlyDuplicate() {
        synchronized (this.playableAudioBuffer) {
            return this.playableAudioBuffer.asReadOnlyBuffer();
        }
    }

    public ByteBuffer getDuplicate() {
        synchronized (this.playableAudioBuffer) {
            return this.playableAudioBuffer.duplicate();
        }
    }

    public String getPosition() {
        return position;
    }

    public String getRemaining() {
        return remaining;
    }

    public String getCapacity() {
        return capacity;
    }

    public ByteBuffer getByteBuffer() {
        return playableAudioBuffer;
    }

    public String getActions() {
        return actions;
    }
}
