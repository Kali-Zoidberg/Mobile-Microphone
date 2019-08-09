package jitter;

import rtp.RtpPacket;

import java.util.LinkedList;

public class SimpleJitterBuffer {

    //Max amount of wiggle room before discard a packet.
    private int discardTime = 400;
    private boolean isReading = false;
    private int bufSize = 100;

    private LinkedList<RtpPacket> overflow;
    private LinkedList<RtpPacket> queue;

    public SimpleJitterBuffer(int discardTime, boolean isReading, int bufSize) {
        this.discardTime = discardTime;
        this.isReading = isReading;
        this.bufSize = bufSize;
        this.overflow = new LinkedList<RtpPacket>();
        this.queue = new LinkedList<RtpPacket>();
    }

    public synchronized RtpPacket[] read()
    {
        isReading = true;
        int count = 0;

        //if queue is empty, return null.
        if (queue.isEmpty()) {
            isReading = false;
            return null;
        }


        RtpPacket[] rtpPacketArray = new RtpPacket[bufSize];
        //Unload from  overflow

        //Read from current queue
        for (; !queue.isEmpty() && count < bufSize; ++count)
        {
            rtpPacketArray[count] = queue.pop();
        }


        return rtpPacketArray;
    }

    public void write(RtpPacket rtpPacket)
    {
        //For simple, if it's out of order, discard.
        if (isReading)
        {
            overflow.add(rtpPacket);
        } else
        {
            RtpPacket curPacket;
            if (!queue.isEmpty())
            {
                 curPacket = queue.peek();
                 //Packet exceeded time delay so discard it
                 if ((int) (System.currentTimeMillis()  - rtpPacket.getTimeStamp()) > curPacket.getTimeStamp() + discardTime) {

                        System.out.println("Packet was discard by queue: " + rtpPacket.getSequenceNumber());
                        return;
                 }

                 //add packet to queue
                queue.add(rtpPacket);




            } else
            {
                //If queue is not empty then just add the packet to the queue
                queue.add(rtpPacket);
            }
        }
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
