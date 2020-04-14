package jitter;

import Network.PacketOrganizer;
import audio.AudioFunctions;
import com.sun.xml.internal.ws.util.ByteArrayBuffer;
import rtp.RtpPacket;
import threads.AudioPlayThread;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

public class SimpleJitterBuffer {

    //Max amount of wiggle room before discard a packet.
    private int discardTime = 400;
    private Boolean isReading = new Boolean(false);
    private Boolean isFull = new Boolean(false);;
    private Boolean isWriting = new Boolean(false);;
    private int bufSize = 100;
    private int payloadSize;
    private int clumpSize = 4;
    PacketOrganizer packetOrganizer = new PacketOrganizer();

    private LinkedList<RtpPacket> overflow;
    private ArrayList<RtpPacket> queue;
    private ByteBuffer playableAudioBuffer;
    public SimpleJitterBuffer(int discardTime, int bufSize, int clumpSize, int payloadSize) {
        this.discardTime = discardTime;
        this.bufSize = bufSize;
        this.overflow = new LinkedList<>();
        this.queue = new ArrayList<>();
        this.playableAudioBuffer = ByteBuffer.allocate(bufSize * payloadSize);
        this.clumpSize = clumpSize;
        this.payloadSize = payloadSize;

    }


//    public  RtpPacket[] read() throws InterruptedException {
//        //pass in the thread, make it wait.
//        System.out.println("Beginning read.");
//
//        //if queue is empty, set to non full
//
///*
//         while ( queue.isEmpty() || this.getWriting() || !this.getFull())
//         {
//
//
//                 if (!this.getFull())
//                 {
//                     System.out.println("Audio thread blocked. Jitter buffer has not been filled.");
//                 } else if (this.getWriting())
//                     System.out.println("Audio thread blocked. Server is writing.");
//                 else
//                     System.out.println("Audio thread blocked. Queue is empty.");
//                 synchronized(this) {
//                     wait();
//                 }
//
//         }
//*/
///*
//        while (queue.size() < clumpSize && !this.getFull())
//        {
//            synchronized (this)
//            {
//                System.out.println("buf size too small. Waiting");
//                wait();
//            }
//        }
//
// */
//        while (queue.isEmpty() || this.getWriting())
//        {
//            synchronized (this)
//            {
//                if (queue.isEmpty())
//                    System.out.println("Queue empty.");
//                else
//                    System.out.println("Is writing.");
//                wait();
//            }
//        }
//
//        //Never, ever read from queue unless there is a large enough number of packets.
//        while (queue.size() < clumpSize) {
//            System.out.println("Waiting for queue to get bigger.");
//            synchronized (this) {
//                wait();
//            }
//        }
//        setReading(true);
//
//        RtpPacket[] rtpPacketArray = new RtpPacket[clumpSize];
//        //Unload from  overflow
//
//        //Read from current queue
//        String sequenceStr = "";
//        int lastSequenceNum = 0;
//        for ( int i = 0; i < clumpSize; ++i)
//        {
//            rtpPacketArray[i] = queue.remove(0);
//            if (lastSequenceNum != 0 && lastSequenceNum > rtpPacketArray[i].getSequenceNumber()) {
//                System.out.println("There was a seuqence out of ordeR:");
//                System.out.println("lastSequenceNum: " + lastSequenceNum + " Current Sequence Num:"  +rtpPacketArray[i].getSequenceNumber());
//
//            }
//            lastSequenceNum = rtpPacketArray[i].getSequenceNumber();
//            sequenceStr += rtpPacketArray[i].getSequenceNumber() + ",";
//        }
//
//        System.out.println("***Sequences***");
//        System.out.println(sequenceStr);
//        //print order of rtp packet array.
//
//        setReading(false);
//        //not reading anymore, notify writer that they can write.
//
//         System.out.println("Done reading, notifying all threads.");
//          synchronized (this) {
//              notify();
//          }
//        return rtpPacketArray;
//    }

    public  byte[] read() throws InterruptedException {
        //pass in the thread, make it wait.
        System.out.println("Beginning read.");


        while (playableAudioBuffer.remaining() == 0 || this.getWriting())
        {
            synchronized (this)
            {
                if (queue.isEmpty())
                    System.out.println("Queue empty.");
                else
                    System.out.println("Is writing.");
                wait();
            }
        }

        //Never, ever read from queue unless there is a large enough number of packets.
        //For the palyable audio buffer logic, this may not be neccessary
//        while (playableAudioBuffer.position() < clumpSize) {
//            System.out.println("Waiting for queue to get bigger.");
//            synchronized (this) {
//                wait();
//            }
//        }
        setReading(true);

        byte[] bytesFromBuffer = new byte[this.payloadSize];

        this.playableAudioBuffer.get(bytesFromBuffer, 0, this.payloadSize);

        //print order of rtp packet array.

        setReading(false);
        //not reading anymore, notify writer that they can write.

         System.out.println("Done reading, notifying all threads.");
          synchronized (this) {
              notify();
          }
        return bytesFromBuffer;
    }
    public void write(RtpPacket rtpPacket) throws InterruptedException {
        //For simple, if it's out of order, discard.
        //block reader from reading
        if (this.getReading()) {
        }
        while (this.getReading()) {


            System.out.println("Writer is waiting...");
            synchronized (this) {
                wait();
            }
            System.out.println("Writer is done waiting..");

            //overflow.add(rtpPacket);
        }
        setWriting(true);

        RtpPacket curPacket;

        if (!queue.isEmpty() && queue.size() < bufSize)
        {
             curPacket = queue.get(queue.size() - 1);


            //Packet exceeded time delay so discard it
            /*
             if ((int) (System.currentTimeMillis()  - rtpPacket.getTimeStamp()) > curPacket.getTimeStamp() + discardTime) {

                    System.out.println("Packet was discard by queue: " + rtpPacket.getSequenceNumber());
                    return;
             }
            */
            if (rtpPacket.getSequenceNumber() < curPacket.getSequenceNumber())
            {
                System.out.println("Packet is out of order, discarding the packet: " + rtpPacket.getSequenceNumber() + " < " + curPacket.getSequenceNumber());
                return ;
            }
             //add packet to queue

        }
        queue.add(rtpPacket);

        //whenever the quee reaches a clump size, reorder the packets and empty them into the orderedpackets queue.
        if (rtpPacket.getSequenceNumber() % clumpSize == 0 && queue.size() >= clumpSize) //reorder packets in queue
        {
            while(queue.size() >= clumpSize) {
                RtpPacket[] tempPackets = (RtpPacket[]) queue.subList(0, clumpSize).toArray();
                byte[][] playableAudioBytes = this.packetOrganizer.reorder(tempPackets, clumpSize);

                //place the audio bytes into the playable audio buffer.
                for (int i = 0; i < playableAudioBytes.length; ++i) {
                    queue.remove(i); //remove bytes from queue
                    this.playableAudioBuffer.put(playableAudioBytes[i]);
                }
            }
        }

    this.setWriting(false);

//    //wait till at least half full before releases
        //reamining can be used to figure how much is remaining
//    if (playableAudioBuffer.position() > playableAudioBuffer.capacity() / 2)) {
//        setFull(true);
//        System.out.println("Queue is full. Notifying all readers.");
        synchronized (this) {
            notify();
        }
//    }
    //Done writing, allow reader to read

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
