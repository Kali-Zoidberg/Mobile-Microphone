package Network;

import Interpolation.Interpolation;
import helper.ByteConversion;
import rtp.RtpPacket;

import java.util.*;

public class PacketOrganizer {

    
    /**
     * Reorders an array of MxN in clumpSizes s.t. the array becomes NxM e.g.
     * [0 1 2]	  [0 3 6]
     * [3 4 5] -> [1 4 7]
     * [6 7 8]	  [2 5 8]
     * @param a The array to shuffle
     * @param clumpSize The clump size to shuffle by.
     * @return Returns a reorder array (useful for delivering UDP packets and utilizing interpolation).
     */

    public byte[][] order(byte[][] a, int clumpSize)
    {

        if (clumpSize > a.length)
            return rowsToCols(a);
        int rows = a[0].length;

        byte[][] retArray = new byte[rows][];
        byte[][] subArray = new byte[clumpSize][];
        LinkedList<byte[]> lSubArrays = new LinkedList<>();
        //Reorder sub arrays
        int count = 0;
        for (int i = 0; i < a.length; ++i)
        {
            //Reorder sub 2d array.
            //flawed logic.

            subArray[count] = a[i];

            //if count is of clump size - 1, do the shuffle.
            if (count >= clumpSize - 1)
            {
                //TODO:
                //rows to cols needs to change.
                byte[][] tempArray = shuffle(subArray);

                for (int k = 0; k < tempArray.length; ++k)
                    lSubArrays.add(tempArray[k]);

                //reset count
                count = 0;
            } else
                ++count;

        }

        //Convert list to array.

        return listToArray(lSubArrays);
    }

    /**
     * Orders and array of RtpPacket's whose array is w
     * @param a
     * @param clumpSize
     * @return
     */
    public byte[][] reorder(RtpPacket[] a, int clumpSize)
    {
        //WAIT WE FORGOT ABOUT CLUMPID
        SortedMap<Integer, ArrayList<byte[]>> clumps = new TreeMap<>();
        int numByteArrays = 0;

        //Iterate over pair
        for (int i = 0; i < a.length - 1; ++i)
        {
            if (a[i +1] != null && a[i] != null) {
                int seqDelta = a[i + 1].getSequenceNumber() - a[i].getSequenceNumber();

                int firstClumpID = a[i].getSequenceNumber() / clumpSize;
                int secondClumpID = a[i + 1].getSequenceNumber() / clumpSize;
                //Check if first clumpID is null
                if (clumps.get(firstClumpID) == null)
                    clumps.put(firstClumpID, new ArrayList<>());

                //Always store first packet  in queue
                clumps.get(firstClumpID).add(a[i].getPayload());
                ++numByteArrays;

                //Interpolate
                if (seqDelta > 1 && firstClumpID == secondClumpID) {

                    //interpolate

                    short[][] interpolShorts = Interpolation.interpolate(ByteConversion.byteArrayToShortArray(a[i].getPayload(), true), ByteConversion.byteArrayToShortArray(a[i + 1].getPayload(), true), seqDelta - 1);
                    for (int j = 0; j < interpolShorts.length; ++j) {
                        System.out.println("interpolated");
                        byte[] interpolBytes = ByteConversion.shortArrayToByteArray(interpolShorts[j], true);
                        clumps.get(firstClumpID).add(interpolBytes);
                        ++numByteArrays;

                    }

                } else if ( seqDelta > 1)
                {
                    System.out.println("could not interpolate.");
                }
            }
        }
        System.out.println("a len: " + a.length);
        int lastClumpID = a[a.length - 1].getSequenceNumber() / clumpSize;
        if (clumps.get(lastClumpID) == null)
            clumps.put(lastClumpID, new ArrayList<>());

        //Add the last payload to the queue
        clumps.get(lastClumpID).add(a[a.length - 1].getPayload());
        ++numByteArrays;

        System.out.println("numByte Arrays: " + numByteArrays);

        byte[][] orderedBytes = new byte[numByteArrays][];
        int k = 0;


        //Loop over each clump
        for (Integer i : clumps.keySet())
        {
            ArrayList<byte[]> bytes = clumps.get(i);
            byte[][] subByteArray = new byte[bytes.size()][];
            if (bytes.size() != clumpSize)
            {
               // System.out.println("****************");
              //  System.out.println("ERROR CLUMP SIZE: " + bytes.size());
              //  System.out.println("****************");
            }
            //Store the clump elements in a temporarry array
            for (int j = 0; j < bytes.size(); ++j)
                subByteArray[j] = bytes.get(j);
           // System.out.println("*******Before shuffle*********");
          //  print2DArray(subByteArray);
          //  System.out.println("****************");
            //unshuffle the clumps
            subByteArray = unshuffle(subByteArray);
          //  System.out.println("********After shuffle********");
          //  print2DArray(subByteArray);
          //  System.out.println("****************");
            //store the clumps in an array
            for (int j = 0; j < subByteArray.length; ++j) {
                orderedBytes[k++] = subByteArray[j];
            }

        }


        /*
        int k = 0;


        //Reorder from queue and reorganize subArrays of clumpSize.
        for (int i = 0; !queue.isEmpty(); ++i)
        {

            if (i != 0 && i % clumpSize == 0)
            {
                //Unshuffle sub byte array
                subByteArray = unshuffle(subByteArray);

                //Copy subByteArray to ordered bytes
                for (int j = 0; j < clumpSize; ++j) {
                    System.out.println("unshuffled at: " + i);
                    orderedBytes[k++] = subByteArray[j];
                    //flush element
                    subByteArray[j] = null;
                }
            }
            //remove from queue
            subByteArray[i % clumpSize] = queue.remove();

        }
            */
        return orderedBytes;
    }

    /**
     * 'Unshuffles' bytes whose rows are cols and whose cols are rows.
     * @param a The array to 'unshuffle'
     * @return Returns a new array that is 'unshuffled'.
     */

    private byte[][] unshuffle(byte[][] a)
    {
        int u = 0;
        int v = 0;
        int rows = a.length;
        int cols;
        byte[][] retArray = new byte[rows][];

        for (int i = 0; i < rows; ++i)
        {
            cols = a[i].length;

            retArray[i] = new byte[cols];

            for (int j = 0; j < cols && v < cols; ++j)
            {
                //Copies cols to rows essentially.

                retArray[i][j] = a[u++][v];

                //Once done looping over u rows, increment v to next col and set u = 0.
                if (u > rows - 1)
                {
                    ++v;
                    u = 0;

                }
            }
        }
        return retArray;
    }

    private byte[][] shuffle(byte[][] a)
    {
        int rows = a.length;
        int cols = 0;
        int u = 0;
        int v = 0;
        byte[][] retArray = new byte[rows][a[0].length];

        //Iterates through array and shuffles using my uhh crappy algorithm. :). PLS HIRE ME.
        for (int i = 0; i < a.length; ++i)
        {

            for (int j = 0; j < a[i].length; ++j)
            {

                retArray[u++][v] = a[i][j];
                //If u is finally >= to num rows, increment v to start copying via columns
                if (u > rows - 1)
                {
                    ++v;
                    u = 0;
                }

            }

        }

        return retArray;
    }
    /**
     * Converts rows of a 2D array to the columns
     * @param a The array to convert
     * @return Returns an array where the columns and rows ahve been switched.
     */
    private byte[][] rowsToCols(byte[][] a)
    {
        //Assume num cols is == for each row in a.
        int rows = a[0].length;
        int cols = 0;
        byte[][] retArray = new byte[rows][];

        for (int i = 0; i < rows; ++i)
        {
            cols = a.length;
            //Allocate enough cols for the array
            retArray[i] = new byte[cols];
            //Flip cols and rows.
            for (int j = 0; j < cols; ++j)
                retArray[i][j] = a[j][i];

        }
        return retArray;
    }

    private byte[][] listToArray(LinkedList<byte[]> lBytes)
    {
        int rows = lBytes.size();
        byte[][] retArray = new byte[rows][];

        //remove first byte[] from the list
        for (int i = 0; !lBytes.isEmpty() && i < rows; ++i)
            retArray[i] = lBytes.remove();

        return retArray;
    }

    private byte[][] genArray(int rows, int cols)
    {
        byte[][] retArray = new byte[rows][cols];
        for (int i = 0; i < rows; ++i)
        {
            for (int j = 0; j < cols; ++j)
            {
                retArray[i][j] = (byte) ((i * cols) + j);
            }
        }
        return retArray;
    }

    public void print2DArray(byte[][] array)
    {
        for (int i = 0; i < array.length; ++i)
        {
            for (int j = 0; j < array[i].length; ++j)
                System.out.print(array[i][j] + " ");
            System.out.println();

        }
    }
}
