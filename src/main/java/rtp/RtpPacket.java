package rtp;
//class RTPpacket


import helper.BitOperation;

public class RtpPacket {

    //size of the RTP header:
    static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    private int pVersion;
    private int pPadding;
    private int pExtension;
    private int pCC;
    private int pMarker;
    private int pPayloadType;
    private int pSequenceNumber;
    private int pTimeStamp;
    private int pSsrc;
    private String id;

    //Bitstream of the RTP header
    private byte[] header;

    //size of the RTP payload
    private int payload_size;
    //Bitstream of the RTP payload
    private byte[] payload;

    private byte[] packet;

    /**
     * Construct an RTP packet. You must increment Framenb each time a new packet is constructed and sent.
     * @param payloadType The payload type see: https://en.wikipedia.org/wiki/RTP_audio_video_profile for payload types.
     * @param sequenceNumber The sequence number
     * @param timeStamp The current time.
     * @param data The data to place into the payload.
     */
    public RtpPacket(int payloadType, int sequenceNumber, int timeStamp, byte[] data){
        //fill by default header fields:
        packet = new byte[HEADER_SIZE + data.length];
        pVersion = 2;
        pPadding = 0;
        pExtension = 0;
        pCC = 0;
        pMarker = 0;
        pSsrc = 0;

        //fill changing header fields:
        pSequenceNumber = sequenceNumber;
        pTimeStamp = timeStamp;
        pPayloadType = payloadType;

        //build the header bistream:
        //--------------------------
        header = initHeader(payloadType, sequenceNumber, timeStamp);
        //fill the payload bitstream:
        //--------------------------
        payload_size = data.length;
        payload = new byte[payload_size];

        //fill payload array of byte from data (given in parameter of the constructor)
        //......
        for (int i = 0; i < payload_size; ++i)
        {
            payload[i] = data[i];
            //set the packet data.
            packet[i + HEADER_SIZE] = data[i];
        }
        // ! Do not forget to uncomment method printheader() below !

    }


    /**
     * Initializes the RTP packet header
     * @param PType
     * @param Framenb
     * @param Time
     * @return
     */

    private byte[] initHeader(int PType, int Framenb, int Time)
    {
        header = new byte[HEADER_SIZE];

        //fill the header
        //x | CC

        byte head0 = BitOperation.joinRight((byte) pCC, (byte) pExtension, (byte) 1);
        // P | x | CC
        head0 = BitOperation.joinRight((byte) head0, (byte) pPadding, (byte) 1);

        //Version | X | P | CC
        head0 = BitOperation.joinRight((byte) head0, (byte) pVersion, (byte) 2);

        header[0] = head0;

        byte head1 = BitOperation.joinRight((byte) PType, (byte) pMarker, (byte) 1);
        header[1] = head1;

        header[2] = (byte) pSequenceNumber;
        header[3] = (byte) (pSequenceNumber >>> 8);

        header = BitOperation.copyBytesIntoArray(header, 4, pTimeStamp);
        header = BitOperation.copyBytesIntoArray(header, 8, pSsrc);

        //copy header into packet array
        for (int i =0; i < HEADER_SIZE; ++i)
        {
            packet[i] = header[i];
        }

        //print header
        return header;
    }


    public RtpPacket(byte[] packet, int packet_size)
    {
        //fill default fields:
        pVersion = 2;
        pPadding = 0;
        pExtension = 0;
        pCC = 0;
        pMarker = 0;
        pSsrc = 0;
        this.packet = packet;
        //check if total packet size is lower than the header size
        if (packet_size >= HEADER_SIZE)
        {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            for (int i=0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            //get the payload bitstream:
            payload_size = packet_size - HEADER_SIZE;
            payload = new byte[payload_size];

            for (int i=HEADER_SIZE; i < packet_size; i++)
                payload[i-HEADER_SIZE] = packet[i];

            //interpret the changing fields of the header:
            pPayloadType = header[1] & 127;

            pSequenceNumber = unsigned_int(header[3]) + 256*unsigned_int(header[2]);
            pTimeStamp = unsigned_int(header[7]) + 256*unsigned_int(header[6]) + 65536*unsigned_int(header[5]) + 16777216*unsigned_int(header[4]);
        }
    }

    /**
     * Generates a unique ID for the packet using the packetSequence number and timestamp.
     */
    public void generateId()
    {
        StringBuffer strBuffer = new StringBuffer();
        //Make the timestamp into hexidecimal
        //Make the Sequence number into hexidecimal
        strBuffer.append(this.getTimeStamp());
        strBuffer.append(this.getSequenceNumber());
        this.id = strBuffer.toString();

    }

    //--------------------------
    //getpayload: return the payload bistream of the RTPpacket and its size
    //--------------------------
    public int getPayload(byte[] data) {

        for (int i=0; i < payload_size; i++)
            data[i] = payload[i];

        return(payload_size);
    }


    public byte[] getPayload()
    {
        return payload;
    }
    /**
     * Returns the length of the payload
     * @return
     */
    public int getPayloadLength() {
        return(payload_size);
    }

    /**
     * Returns the length of the whole packet.
     * @return
     */
    public int getLength() {
        return(payload_size + HEADER_SIZE);
    }


    /**
     *
     * @return Returns the timestamp at which the packet was constructed
     */
    public int getTimeStamp() {
        return(pTimeStamp);
    }

    /**
     * Returns the sequence number
     * @return Returns the Sequence number
     */
    public int getSequenceNumber() {
        return(pSequenceNumber);
    }

    /**
     * Prints the payload type.
     * @return
     */
    public int getPayloadType() {
        return(pPayloadType);
    }


    /**
     * Prints the packet's header.
     */
    public void printHeader()
    {
        //TO DO: uncomment

        for (int i=0; i < (HEADER_SIZE-4); i++)
        {
            for (int j = 7; j>=0 ; j--)
                if (((1<<j) & header[i] ) != 0)
                    System.out.print("1");
                else
                    System.out.print("0");
            System.out.print(" ");
        }

        System.out.println();
    }

    /**
     * Returns the packet as a byte array.
     * @return
     */
    public byte[] getPacket() {
        return packet;
    }

    //return the unsigned value of 8-bit integer nb
    static int unsigned_int(int nb) {
        if (nb >= 0)
            return(nb);
        else
            return(256+nb);
    }

    /**
     * Returns the unique ID based on the timestamp and sequence number of the packet.
     * @return
     */
    public String getId()
    {
        return this.id;
    }

}