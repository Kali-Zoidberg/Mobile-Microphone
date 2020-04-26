package threads;

import javax.sound.sampled.SourceDataLine;
import audio.AudioFunctions;
public class AudioWriter extends Thread{

    private byte[] data;
    private SourceDataLine dataLine;

    /**
     * Constructs a new Audio writer with the specified bytes to write.
     * @param data A 2D array of bytes containing audio data to write to the data line.
     * @param dataLine The data line to write to
     */

    public AudioWriter(byte[] data, SourceDataLine dataLine)
    {
        this.data = data;
        this.dataLine = dataLine;
    }

    /**
     * Writes to the Data line.
     */
    public void run()
    {
            AudioFunctions.writeDataToLine(data, dataLine);

    }

    public byte[] getData() {
        return data;
    }

    public SourceDataLine getDataLine() {
        return dataLine;
    }
}
