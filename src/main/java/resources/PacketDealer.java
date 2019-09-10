package resources;

import Network.Server;
import jitter.JitterBuffer;
import jitter.SimpleJitterBuffer;
import rtp.RtpPacket;
import threads.AudioPlayThread;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.util.LinkedList;

/**
 * Packet dealer handles transactions of packets between threads by sharing resources
 */
@SuppressWarnings("Duplicates")
public class PacketDealer {

    private static int port;
    //Max amount of wiggle room before discard a packet.
    private static int discardTime;
    private static boolean isReading = false;
    private static boolean isFull = false;
    private static int bufSize;
    private static int clumpSize;
    private static Object object = new Object();
    private static AudioFormat clientAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, false);

    private static Server writer = new Server(9000);
    private static AudioPlayThread reader = new AudioPlayThread(null, writer, clientAudioFormat);
    private static LinkedList<RtpPacket> overflow = new LinkedList<RtpPacket>();
    private static LinkedList<RtpPacket> queue = new LinkedList<RtpPacket>();

    public static void initializer(int port)
    {
        System.out.println("started servber.");

        try {
            reader.openCableLine();
        } catch (LineUnavailableException e) {
            System.out.println("Cannot open line. Notify client of error.");
            e.printStackTrace();
        }

            writer.start();


        reader.start();

    }


    public static synchronized RtpPacket[] read() throws InterruptedException {

        //pass in the thread, make it wait.

        //if queue is empty, set to non full
        if (queue.isEmpty()) {
            setFull(false);
        }
        //or we could wait
        if (queue.size() >= bufSize)
            setFull(true);

        if (!isFull) {
                setReading(false);
                synchronized (reader) {
                    System.out.println("Not Full. Waiting...");
                }
        }


        //is reading so block writer
        setReading(true);

        RtpPacket[] rtpPacketArray = new RtpPacket[clumpSize];
        //Unload from  overflow

        //Read from current queue
        for ( int i = 0; i < clumpSize; ++i)
        {
            rtpPacketArray[i] = queue.remove();
        }

        setReading(false);
        //not reading anymore, notify writer that they can write.
        synchronized (writer) {
            System.out.println("Writer notified.");
            writer.notify();
        }

        return rtpPacketArray;
    }

    public static synchronized void write(RtpPacket rtpPacket) throws InterruptedException {
        //For simple, if it's out of order, discard.
        //block reader from reading
        System.out.println("hel;lol");
        if (isReading)
        {
            System.out.println("Adding to voerflow");
            synchronized (writer) {
                System.out.println("writer waiting.");
                writer.wait();
                System.out.println("hello");
            }
            //overflow.add(rtpPacket);
        } else
        {
            RtpPacket curPacket;

            if (!queue.isEmpty() && queue.size() < bufSize)
            {
                curPacket = queue.peekLast();


                //Packet exceeded time delay so discard it
                /*
                 if ((int) (System.currezntTimeMillis()  - rtpPacket.getTimeStamp()) > curPacket.getTimeStamp() + discardTime) {

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

        //Done writing, allow reader to read
        synchronized (reader) {
            System.out.println("reader notified.");
            reader.notify();
        }
    }

    private static synchronized  void setFull(boolean isFull) { isFull = isFull; }

    private static synchronized void setReading(boolean reading)
    {
       isReading = reading;
    }

    public static int getDiscardTime() {
        return discardTime;
    }

    public static int getBufSize() {
        return bufSize;
    }

    public static void setDiscardTime(int discardTime) {
        PacketDealer.discardTime = discardTime;
    }

    public static void setBufSize(int bufSize) {
        PacketDealer.bufSize = bufSize;
    }

    public static void setClumpSize(int clumpSize) {
        PacketDealer.clumpSize = clumpSize;
    }

    public static void setPort(int port) {
        PacketDealer.port = port;
    }

    public static void setIsReading(boolean isReading) {
        PacketDealer.isReading = isReading;
    }

    public static void setIsFull(boolean isFull) {
        PacketDealer.isFull = isFull;
    }

    public static void setReader(AudioPlayThread reader) {
        PacketDealer.reader = reader;
    }

    public static void setWriter(Server writer) {
        PacketDealer.writer = writer;
    }

    public static void setOverflow(LinkedList<RtpPacket> overflow) {
        PacketDealer.overflow = overflow;
    }

    public static void setQueue(LinkedList<RtpPacket> queue) {
        PacketDealer.queue = queue;
    }

    public static int getPort() {
        return port;
    }

    public static boolean isIsReading() {
        return isReading;
    }

    public static boolean isIsFull() {
        return isFull;
    }

    public static int getClumpSize() {
        return clumpSize;
    }

    public static AudioPlayThread getReader() {
        return reader;
    }

    public static Server getWriter() {
        return writer;
    }

    public static LinkedList<RtpPacket> getOverflow() {
        return overflow;
    }

    public static LinkedList<RtpPacket> getQueue() {
        return queue;
    }

    private static SimpleJitterBuffer jitterBuffer;


    public static SimpleJitterBuffer getJitterBuffer() {
        return jitterBuffer;
    }
}
