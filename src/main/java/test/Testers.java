package test;
import java.util.AbstractList;
import java.util.LinkedList;
public class Testers {

        public void main()
        {
            byte[][] testArray = {{0,1,2,3}, {4,5,6,7},{8,9,10,11},{12,13,14,15}};
            //print2DArray(rowsToCols(genArray(8,8)));
            byte[][] reorderedArray = reorder(genArray(8,3),4);
            byte[][] array = new byte[reorderedArray.length][];
            int u = 0;
            for (int i = 0; i < reorderedArray.length; ++i)
            {
                if (i != 1)
                    array[i] = reorderedArray[i];
            }
            array[1] = interpolate(reorderedArray[0], reorderedArray[2], 1)[0];
            //array[2] = interpolate(reorderedArray[0], reorderedArray[3], 2)[1];

            print2DArray(genArray(4,3));
            System.out.println("**************Reordered*******************");
            print2DArray(reorderedArray);
            System.out.println("****************Without*****************");
            print2DArray(array);
            System.out.println("********************Unshuffled*************");
            byte[][] firstHalf = new byte[array.length / 2][];
            for (int i = 0; i < firstHalf.length; ++i)
                firstHalf[i] = array[i];

            print2DArray(unshuffle(firstHalf));
            for (int i = 0; i < firstHalf.length; ++i)
                firstHalf[i] = array[i + firstHalf.length];
            print2DArray(unshuffle(firstHalf));
        }

        public byte[][] interpolate(byte[] a, byte[] b, int delta) {
            //Determine the max size of the columns.
            int cols = a.length > b.length ? a.length : b.length;
            float ratio = 1;
            double aAverage = calcAverage(a);
            double bAverage = calcAverage(b);

            boolean isASmaller = aAverage < bAverage ? true: false;

            byte[][] interpolShorts = new byte[delta][cols];

            for (int i = 0; i < delta; ++i) {
                ratio = (i + 1) / (float) (delta + 1);
                System.out.println("ratio : " + ratio);

                //This equation calculates the average amount we need to add to each short in the array.
                double interpolAmount = ((bAverage - aAverage) * ratio);
                //double interpolAmount = cosineInterpolate(aAverage, bAverage, ratio);
                for (int j = 0; j < a.length; ++j) {

                    //add a[j] to the interpolated amount to reach the new average.
                    //short offset = isASmaller ? a[j] : b[j];
                    int offset = b[j] - a[j];
                    interpolAmount = ((int)a[j] + (int) b[j]) * ratio + 0.5;
                    interpolShorts[i][j] = (byte) (a[j] + (offset * ratio) + 0.5);
                    //interpolShorts[i][j] = (short) interpolAmount;

                }
                //interpolShorts[i][j] = (short) (a[j] + ((b[j] - a[j])/2));
                //test interpolation equation

            }



            return interpolShorts;
        }

        private double calcAverage(byte[] a)
        {
            int sum = 0;
            int len = a.length;
            for (int i = 0; i < len; ++i)
                sum += (int) a[i];

            return (sum / (double) len);

        }
        /**
         * Reorders an array of MxN in clumpSizes s.t. the array becomes NxM e.g.
         * [0 1 2]	  [0 3 6]
         * [3 4 5] -> [1 4 7]
         * [6 7 8]	  [2 5 8]
         * @param a The array to shuffle
         * @param clumpSize The clump size to shuffle by.
         * @return Returns a reorder array (useful for delivering UDP packets and utilizing interpolation).
         */

        public byte[][] reorder(byte[][] a, int clumpSize)
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
         * 'Unshuffles' bytes whose rows are cols and whose cols are rows.
         * @param a The array to 'unshuffle'
         * @return Returns a new array that is 'unshuffled'.
         */

        private  byte[][] unshuffle(byte[][] a)
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

                for (int j = 0; j < cols; ++j)
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

        private  byte[][] listToArray(LinkedList<byte[]> lBytes)
        {
            int rows = lBytes.size();
            byte[][] retArray = new byte[rows][];

            //remove first byte[] from the list
            for (int i = 0; !lBytes.isEmpty() && i < rows; ++i)
                retArray[i] = lBytes.remove();

            return retArray;
        }

        private  byte[][] genArray(int rows, int cols)
        {
            int count = 0;
            byte[][] retArray = new byte[rows][cols];
            for (int i = 0; i < rows; ++i)
            {
                for (int j = 0; j < cols; ++j)
                {
                    retArray[i][j] = (byte) (count * 2);
                    ++count;
                }
            }
            return retArray;
        }

        public  void print2DArray(byte[][] array)
        {
            for (int i = 0; i < array.length; ++i)
            {
                for (int j = 0; j < array[i].length; ++j)
                    System.out.print(array[i][j] + " ");
                System.out.println();

            }
        }
    }


