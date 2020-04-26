package jitter;

import Network.PacketOrganizer;
import audio.AudioFunctions;
import rtp.RtpPacket;
import threads.AudioPlayThread;

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
    private Boolean isReading = new Boolean(false);
    private Boolean isFull = new Boolean(false);;
    private Boolean isWriting = new Boolean(false);;
    private int bufSize = 100;
    private int payloadSize;
    private int clumpSize = 4;
    ReentrantLock lock = new ReentrantLock();

    PacketOrganizer packetOrganizer = new PacketOrganizer();
    FileWriter jitterFile;
    private LinkedList<RtpPacket> overflow;
    private ArrayList<RtpPacket> queue;
    private ByteBuffer playableAudioBuffer;
    private ByteBuffer readOnly;
    public SimpleJitterBuffer(int discardTime, int bufSize, int clumpSize, int payloadSize) {
        packetOrganizer.createFile("PacketOrganizer.txt");
        this.discardTime = discardTime;
        this.bufSize = bufSize;
        this.overflow = new LinkedList<>();
        this.queue = new ArrayList<>();
        this.playableAudioBuffer = ByteBuffer.allocate(bufSize * (payloadSize * this.clumpSize));
        this.clumpSize = clumpSize;
        this.payloadSize = payloadSize;
        try {
            this.jitterFile = new FileWriter("Jitterbuffer.txt");
            this.jitterFile.write("Start");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] read() throws InterruptedException {
        //pass in the thread, make it wait.
        synchronized (this.playableAudioBuffer) {
            int bufferSize = this.playableAudioBuffer.capacity() - this.playableAudioBuffer.remaining();

            //not enough packets, return null
            if (bufferSize < (this.clumpSize * this.payloadSize)) {
                return null;
            } else {

                byte[] playableAudioBytes = new byte[this.payloadSize];
                try {
                    System.out.println("Reader attempting to aquire lock: " + this.playableAudioBuffer.position());
                    this.lock.lock();
                    readOnly = this.playableAudioBuffer.duplicate();
                    readOnly.get(playableAudioBytes, 0, playableAudioBytes.length);
//                    this.playableAudioBuffer.position(this.playableAudioBuffer.position() + playableAudioBytes.length);
                    byte[][] app = {playableAudioBytes};
                    packetOrganizer.print2DArray(app);
                } finally {
                    this.lock.unlock();
                    System.out.println("Successfully unlocked the lock!");
                    System.out.println("new position: " + this.playableAudioBuffer.position());

                    return playableAudioBytes;
                }
            }
        }
    }


    public void write(RtpPacket rtpPacket) throws InterruptedException {
        //For simple, if it's out of order, discard.
        //block reader from reading

            queue.add(rtpPacket);

        if (queue.size() >= this.clumpSize)
        {
            byte[][] reorderedBytePackets = null;
            try {
                //remove packets from queue
                RtpPacket[] unorderedPackets = new RtpPacket[this.clumpSize];
                for (int i = 0; i < this.clumpSize; ++i) {
                    unorderedPackets[i] = queue.remove(0);
                }

                //block
                this.lock.lock();
                //reorder packets

                reorderedBytePackets = this.packetOrganizer.reorder(unorderedPackets, this.clumpSize);

                //if full, discard or allocate new buffer
                if (this.playableAudioBuffer.remaining() == 0) {
                    System.out.println("Aduio buffer full. releasing lock and returning");
                    this.playableAudioBuffer.position(0);
                    this.lock.unlock();
                    return;
                }
                try {
                //not full, place into byte buffer
                for (int i = 0; i < reorderedBytePackets.length; ++i) {
                    this.jitterFile.write(packetOrganizer.byteArrToString(reorderedBytePackets[i]));

                    this.playableAudioBuffer.put(reorderedBytePackets[i]);
                    System.out.println("capacity: " + this.playableAudioBuffer.remaining());
                    System.out.println("position: " + this.playableAudioBuffer.position());
//                    this.playableAudioBuffer.flip();
//                    this.playableAudioBuffer.position(this.playableAudioBuffer.position() + reorderedBytePackets[i].length);
                }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            } catch(BufferOverflowException e) {
                e.printStackTrace();
                System.out.println("reorderPackets length: " + reorderedBytePackets.length);
                System.out.println("remaining: " + this.playableAudioBuffer.remaining());
                System.out.println("capacity: " + this.playableAudioBuffer.capacity());
                //Clear buffer if overflow
                this.playableAudioBuffer.clear();

            }
            finally {
                //unblock
                this.lock.unlock();
                System.out.println("Writer has released lock");
            }
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


}
