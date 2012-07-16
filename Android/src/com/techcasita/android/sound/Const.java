//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

package com.techcasita.android.sound;

/**
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public interface Const {
    int SAMPLE_RATE = 44100;    // in Hz
    int SAMPLES = 512;          // number of samples
    int BITS = 9;               // a power of two such that 2^b is the number_of_samples
    int REPEAT = 3;             // repeat each signal
    int OFFSET = 60;            // number of un-usable bin on the spectrum's lower end
}
