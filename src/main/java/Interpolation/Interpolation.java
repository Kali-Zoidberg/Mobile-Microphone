package Interpolation;

public class Interpolation {




    /**
     * Source: http://paulbourke.net/miscellaneous/interpolation/
     * Code by
     * @param a
     * @param b
     * @param mu
     * @return
     */
    private static double cosineInterpolate(double a, double b, double mu)
    {
        double mu2 = (1 - Math.cos(mu * Math.PI))/2;
        return (a * (1- mu2) + b *mu2);
    }


    @SuppressWarnings("Duplicates")
    public static short[][] interpolate(short[] a, short[] b, int delta) {
        //Determine the max size of the columns.
        int cols = a.length > b.length ? a.length : b.length;
        float ratio = 1;
        double aAverage = calcAverage(a);
        double bAverage = calcAverage(b);

        boolean isASmaller = aAverage < bAverage ? true: false;

        short[][] interpolShorts = new short[delta][cols];

        for (int i = 0; i < delta; ++i) {
            ratio = (i + 1) / (float) (delta + 1);

            //This equation calculates the average amount we need to add to each short in the array.
            double interpolAmount = ((bAverage - aAverage) * ratio);
            //double interpolAmount = cosineInterpolate(aAverage, bAverage, ratio);
            for (int j = 0; j < a.length; ++j) {

                //add a[j] to the interpolated amount to reach the new average.
                //short offset = isASmaller ? a[j] : b[j];
                short offset = a[j];
                interpolShorts[i][j] = (short) ((offset + interpolAmount));
                //interpolShorts[i][j] = (short) interpolAmount;

            }
                //interpolShorts[i][j] = (short) (a[j] + ((b[j] - a[j])/2));
            //test interpolation equation

        }



        return interpolShorts;
    }
    /**
     * Linear interpolates an array of shorts
     * @param a     The starting array
     * @param b     The Ending array
     * @param delta The number of short arrays to interpolate
     * @return Returns a 2D array of shorts with the delta interpolated short arrays.
     */
    public static short[][] linearInterpolation(short[] a, short[] b, int delta) {
        //Determine the max size of the columns.
        int cols = a.length > b.length ? a.length : b.length;
        float ratio = 1;
        double aAverage = calcAverage(a);
        double bAverage = calcAverage(b);

        boolean isASmaller = aAverage < bAverage ? true: false;
        short[][] interpolShorts = new short[delta][cols];

        for (int i = 0; i < delta; ++i) {
            ratio = (i + 1) / (float) (delta + 1);

            //This equation calculates the average amount we need to add to each short in the array.
            double interpolAmount = ((bAverage - aAverage) * ratio);

            for (int j = 0; j < a.length; ++j)
                //add a[j] to the interpolated amount to reach the new average.
                interpolShorts[i][j] = (short) ((a[j] + interpolAmount));
                //interpolShorts[i][j] = (short) (a[j] + ((b[j] - a[j])/2));
            //test interpolation equation

        }

        if (isASmaller)
            System.out.println("aAverage: " + aAverage);
        else
            System.out.println("bAverage: " + bAverage);
        for (int i = 0; i < delta; ++i)
        {
            ratio = (i + 1) / (float) (delta + 1);

            if (isASmaller)
                System.out.printf("%d: aAvg: %f ratio: %f\n",i, (aAverage + bAverage) * ratio, ratio);
            else
                System.out.printf("%d: bAvg: %f ratio: %f\n",i, (aAverage + bAverage) * ratio, ratio);


        }

        if (isASmaller)
            System.out.println("bAverage: " + bAverage);
        else
            System.out.println("aAverage: " + aAverage);

        return interpolShorts;
    }

    private static double calcAverage(short[] a)
    {
        int sum = 0;
        int len = a.length;
        for (int i = 0; i < len; ++i)
            sum += (int) a[i];

        return (sum / (double) len);

    }

    public static void testInterpolation(short[] a, short b[], int delta)
    {
        short[][] interpolatedShorts = linearInterpolation(a,b,delta);
        int sum = 0;
        double[] averages = new double[interpolatedShorts.length];
        double aAvg = calcAverage(a);
        double bAvg = calcAverage(b);

        for (int i = 0; i < interpolatedShorts.length; ++i)
        {
         averages[i] = calcAverage(interpolatedShorts[i]);
        }
        System.out.println("A Array Avg: " + aAvg);
        for (int i  = 0; i < averages.length; ++i)
            System.out.printf("averags[%d]: %f\n", i, averages[i]);

        System.out.println("B Array Avg: " + bAvg);
    }

}
