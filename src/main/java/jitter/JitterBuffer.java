package jitter;

import rtp.Helper;
import rtp.RtpPacket;

import java.net.DatagramPacket;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;


/**
 *
 * The read fgrom should return a specciifed millisecond amount of packets or array
 * And then it can call a write to overflow buffer after it finishes execution blocking when nesccesary!!!
 *
 *
 * OverflowBuffer could be multiple arrays too, issues is with allocating space for these arrays is main concern.
 */
public class JitterBuffer {
    private RtpPacket[] tempBuffer;
    private LinkedBlockingQueue<RtpPacket> outputBuffer;
    private LinkedList<RtpPacket> overflowBuffer;
    private boolean isReading = false;
    private int size;

    public JitterBuffer(int size) {
        this.size = size;
        tempBuffer = new RtpPacket[size];
        outputBuffer = new LinkedBlockingQueue<>();
        overflowBuffer = new LinkedList<>();
    }


    public void addToTempBuffer(DatagramPacket packet, int hashedIndex) {

        //Check to see if the tempBuffer already has a packet at this index.
        if (tempBuffer[hashedIndex] != null) {
            //Add to overflow linekdlist
        }
    }



    /**
     * Adds a datagram packet to the jitter tempBuffer.
     *
     * @param packet
     * @return Returns the index that the packet was placed on. If it is full, -1 is returned.
     */
    public synchronized int addToBuffer(DatagramPacket packet) throws InterruptedException {

        RtpPacket rtpPacket = Helper.datagramToRtpPacket(packet);
        int hashedIndex = (int) (rtpPacket.getSequenceNumber() % this.size);
        if (tempBuffer[hashedIndex] == null)
            tempBuffer[hashedIndex] = rtpPacket;
        //Add to overflow if there is a collision.
        else
            addToOverFlow(packet);

            return hashedIndex;
    }


    /**
     * Reads from the tempBuffer
     * @param size
     * @return
     */

    public RtpPacket[] readPackets(int size) throws InterruptedException {
        int i = 0;
        RtpPacket[] retPackets = new RtpPacket[size];

        if (isReading)
            this.wait();

        isReading = true;

        while (!outputBuffer.isEmpty() && i < size)
            retPackets[i++] = outputBuffer.remove();

        isReading = false;

        return retPackets;
    }

    /**
     * Writes datagram packets to buffer.
     *
     * @param packet
     * @throws InterruptedException
     */

    public void writeToOutputBuffer(DatagramPacket packet) throws InterruptedException {

        RtpPacket rtpPacket = Helper.datagramToRtpPacket(packet);

        if (!isReading)
            isReading = true;

        transferOverflow();

        this.outputBuffer.put(rtpPacket);

        this.notify();


    }

    public RtpPacket[] readBuffer(int numPackets) throws InterruptedException {

        //TODO O(n), might be better to declare an object array and it is cleared/changed instead of at each read.
        RtpPacket[] retPackets = new RtpPacket[numPackets];

        //If the outputbuffer is empty, transfer buffer data.
        if (outputBuffer.isEmpty())
            transferBufferData();
        else
        {
            for (int i = 0; !outputBuffer.isEmpty() && i < numPackets; ++i)
            {
                retPackets[i] = outputBuffer.take();
            }
        }
        return retPackets;

    }

    private void addToOverFlow(DatagramPacket packet)
    {
        RtpPacket rtpPacket = Helper.datagramToRtpPacket(packet);
        overflowBuffer.add(rtpPacket);
    }

    public void transferBufferData() throws InterruptedException
    {
        transferTempBuffer();
        transferOverflow();
    }

    public void transferTempBuffer() throws InterruptedException
    {
        int arrLen = tempBuffer.length;

        for (int i = 0; i < arrLen; ++i)
        {
            if (tempBuffer[i] != null) {

                //place onto output buffer.


                outputBuffer.put(tempBuffer[i]);
                //clear this array slot
                tempBuffer[i] = null;
            } else
            {
                //If it is null, find next avaiable i and interpolate
                int nextNonNull = this.findNonNull(tempBuffer, i);
                if (nextNonNull != -1) {

                    //TODO: INTERPOLATION.
                } else
                {
                    //Done iterating through the tempBuffer.
                    break;
                }
            }
        }
    }

    /**
     * Finds the next non-null element past the specified offset.
     * @param arr The array to search
     * @param offset The offset to begin searching for the next non-null element.
     * @param <T> Generic type for the array.
     * @return
     */
    private<T> int findNonNull(T[] arr, int offset)
    {
        int len = arr.length;
        for (int i = offset; i < len; ++i)
            if (arr[i] != null)
                return i;

        return -1;
    }

    public void transferOverflow() throws InterruptedException{
        while (!this.overflowBuffer.isEmpty())
        {
            //clear data from overflow
            RtpPacket curPacket = overflowBuffer.remove();
            //place packet on queue
            outputBuffer.put(curPacket);
        }
    }

    public RtpPacket readFromBuffer(int index)
    {
        if (index < 0 || index > this.size)
            return null;
        else
            return tempBuffer[index];
    }

    public synchronized RtpPacket getFromBuffer(int index)
    {
        if (index < 0 || index > this.size)
            return null;

        RtpPacket retPacket = tempBuffer[index];
        //If the packet is null, search for the next avaiable packet.
        if (retPacket == null)
        {
            for (int i = index; i < this.size; ++i) {
                if (tempBuffer[i] != null)
                {
                    retPacket = tempBuffer[i];
                    tempBuffer[i] = null;
                    return retPacket;
                }

            }
        }

        return retPacket;
    }




}
