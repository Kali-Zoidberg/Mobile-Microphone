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

    public SimpleJitterBuffer(int discardTime, int bufSize) {
        this.discardTime = discardTime;
        this.bufSize = bufSize;
        this.overflow = new LinkedList<RtpPacket>();
        this.queue = new LinkedList<RtpPacket>();
    }

    public synchronized RtpPacket[] read() throws InterruptedException {

        //if queue is empty, return null.
        //or we could wait
        if (queue.size() < bufSize ) {
            if (queue.size() == 0)
            {
                this.setReading(false);
                return null;
            }
            if ((queue.size() != 0 && queue.peekLast().getSequenceNumber() < queue.peekFirst().getSequenceNumber() + bufSize)) {

                this.setReading(false);
                return null;
            }
        }

        this.setReading(true);
        int len = queue.size();
        RtpPacket[] rtpPacketArray = new RtpPacket[len];
        //Unload from  overflow

        //Read from current queue
        for ( int i = 0; !queue.isEmpty() && i < len; ++i)
        {
            rtpPacketArray[i] = queue.remove();
        }

        this.setReading(false);

        return rtpPacketArray;
    }

    public synchronized void write(RtpPacket rtpPacket)
    {
        //For simple, if it's out of order, discard.

        if (isReading)
        {
            System.out.println("Adding to voerflow");

            overflow.add(rtpPacket);
        } else
        {
            RtpPacket curPacket;

            if (!queue.isEmpty() && queue.size() < bufSize)
            {
                 curPacket = queue.peekLast();


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
                queue.add(rtpPacket);

            } else if (queue.isEmpty())
            {
                //If queue is empty then just add the packet to the queue regardless
                queue.add(rtpPacket);
            }
        }
    }

    private synchronized void setReading(boolean reading)
    {
        this.isReading = reading;
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
