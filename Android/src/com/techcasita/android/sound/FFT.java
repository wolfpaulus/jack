package com.techcasita.android.sound;
//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

/**
 * A fast Fourier Transformer
 * http://code.google.com/p/android-guitar-tuner/source/browse/trunk/src/com/example/GuitarTuner/FFT.java?r=2
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class FFT {

    private static final double TWOPI = 2.0 * Math.PI;
    private static final int LOG2_MAXFFTSIZE = 15;     // Limits on the number of mBits this algorithm can utilize
    private static final int MAXFFTSIZE = 1 << LOG2_MAXFFTSIZE;
    private final int[] mBitreverse = new int[MAXFFTSIZE];
    private int mBits;

    /**
     * FFT class constructor
     * Initializes code for doing a fast Fourier transform
     *
     * @param bits int mBits is a power of two such that 2^b is the number
     *             of samples.
     */
    public FFT(final int bits) {

        mBits = bits;

        if (bits > LOG2_MAXFFTSIZE) {
            throw new RuntimeException("" + bits + " is too big");
        }
        for (int i = (1 << bits) - 1; i >= 0; --i) {
            int k = 0;
            for (int j = 0; j < bits; ++j) {
                k *= 2;
                if ((i & (1 << j)) != 0)
                    k++;
            }
            this.mBitreverse[i] = k;
        }
    }

    /**
     * A fast Fourier transform routine
     *
     * @param xr      double [] xr is the real part of the data to be transformed
     * @param xi      double [] xi is the imaginary part of the data to be transformed
     *                (normally zero unless inverse transform is effect).
     * @param invFlag boolean invFlag which is true if inverse transform is being
     *                applied. false for a forward transform.
     */
    public void doFFT(final double[] xr, final double[] xi, final boolean invFlag) {
        // for mBits = 9
        final int n = (1 << mBits);                     // n = 512
        int n2 = n / 2;                                 // n2 = 256

        for (int l = 0; l < mBits; l++) {               // l= 0,1,2,..,7,8
            for (int k = 0; k < n; k += n2) {           // k= 0,256
                for (int i = 0; i < n2; i++, k++) {     // i= 0,1,2, .. 255, k=0..255 || 256..511
                    final int p = mBitreverse[k / n2];
                    final double ang = TWOPI * p / n;
                    final double c = Math.cos(ang);
                    double s = Math.sin(ang);
                    final int kn2 = k + n2;

                    if (invFlag) {
                        s = -s;
                    }
                    double tr = xr[kn2] * c + xi[kn2] * s;
                    double ti = xi[kn2] * c - xr[kn2] * s;

                    xr[kn2] = xr[k] - tr;
                    xi[kn2] = xi[k] - ti;
                    xr[k] += tr;
                    xi[k] += ti;
                }
            }
            n2 /= 2;
        }

        for (int k = 0; k < n; k++) {
            final int i = mBitreverse[k];
            if (i <= k) {
                continue;
            }
            final double tr = xr[k];
            final double ti = xi[k];
            xr[k] = xr[i];
            xi[k] = xi[i];
            xr[i] = tr;
            xi[i] = ti;
        }

        // Finally, multiply each value by 1/n, if this is the forward transform.
        if (!invFlag) {
            final double f = 1.0 / n;
            for (int i = 0; i < n; i++) {
                xr[i] *= f;
                xi[i] *= f;
            }
        }
    }
}