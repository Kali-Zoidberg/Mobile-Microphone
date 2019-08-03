package rtp;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;
import java.io.IOException;

public class RtpConnection implements RTPConnector {
    private PushSourceStream controlInputStream;
    private OutputDataStream controlOutputStream;
    private PushSourceStream dataInputStream;
    private OutputDataStream dataOutputStream;

    private int bufferSize;



    public int getReceiveBufferSize()
    {
        return bufferSize;
    }

    @Override
    public void setSendBufferSize(int i) throws IOException {

    }

    @Override
    public int getSendBufferSize() {
        return 0;
    }

    @Override
    public double getRTCPBandwidthFraction() {
        return 0;
    }

    @Override
    public double getRTCPSenderBandwidthFraction() {
        return 0;
    }

    @Override
    public PushSourceStream getControlInputStream() {
        return controlInputStream;
    }

    @Override
    public OutputDataStream getControlOutputStream() {
        return controlOutputStream;
    }

    @Override
    public void close() {

    }

    @Override
    public void setReceiveBufferSize(int i) throws IOException {

    }

    @Override
    public PushSourceStream getDataInputStream() {
        return dataInputStream;
    }

    @Override
    public OutputDataStream getDataOutputStream() {
        return dataOutputStream;
    }
}
