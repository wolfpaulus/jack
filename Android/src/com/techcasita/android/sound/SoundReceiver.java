//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

package com.techcasita.android.sound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class SoundReceiver {
    static final String LOG_TAG = SoundReceiver.class.getSimpleName();

    interface IOBuffer {
        void putBuffer(short[] buffer);

        short[] getBuffer();

        int getBufferSize();

        void release();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (0 < msg.what) {
                mTV.setText("");
            } else {
                if (msg.getData() != null) {
                    String s = msg.getData().getString("Text");
                    mTV.setText(s);
                    try {
                        mPB.setProgress(Integer.parseInt(s) / 10);
                    } catch (NumberFormatException e) {
                        // intentionally empty
                    }
                }
            }
        }
    };

    private final IOBuffer mBuffer = new SyncBuffer(Const.SAMPLES);
    private final Decoder mDecoder = new Decoder(mHandler);

    private AudioRecord mAudioInput;
    private AudioReaderThread mAudioReader;
    private AnalyzerThread mAnalyzer;
    private TextView mTV;
    private ProgressBar mPB;

    public SoundReceiver(TextView tv, ProgressBar pb) {
        mTV = tv;
        mPB = pb;
    }

    public void start() {
        final int recBufferSize = 4 * AudioRecord.getMinBufferSize(Const.SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        assert (recBufferSize > Const.SAMPLES);
        mTV.setText("");
        mPB.setProgress(0);

        mAudioInput = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Const.SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufferSize  // most likely 1600
        );
        mAudioReader = new AudioReaderThread(mAudioInput, mBuffer);
        mAnalyzer = new AnalyzerThread(mBuffer, Const.SAMPLE_RATE, mDecoder);

        mAnalyzer.start();
        mAudioReader.start();
        mAudioInput.startRecording();
    }

    public void stop() {
        if (mAudioReader != null && mAudioReader.isAlive()) {
            mAudioReader.interrupt();
        }
        if (mAnalyzer != null && mAnalyzer.isAlive()) {
            mAnalyzer.interrupt();

        }
        mBuffer.release();
        if (mAudioInput != null) {
            try {
                mAudioInput.stop();
            } catch (IllegalStateException e) {
                // intentionally empty
            }
        }
        mAudioReader = null;
        mAnalyzer = null;
        Log.i(LOG_TAG, "SoundReceiver was stopped.");
    }


    public void destroy() {
        stop();
        if (mAudioInput != null) {
            try {
                mAudioInput.stop();
            } catch (IllegalStateException e) {
                // intentionally empty
            }
            mAudioInput.release();
            Log.i(LOG_TAG, "AudioRecord was release.");
        }
    }
}


/**
 * The <code>AudioReaderThread</code> works in concert with the <code>AnalyzerThread</code>. AudioReaderThread acts as
 * the producer, while AnalyzerThread is the consumer.
 * AudioReaderThread reads from a pre-configured <code>AudioRecord</code> and puts the read values into a
 * synchronized buffer.
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
class AudioReaderThread extends Thread {
    final int mBufferSize;
    final AudioRecord mAudioInput;
    final SoundReceiver.IOBuffer mBuffer;

    /**
     * @param audioInput <code>AudioRecord</code> needs to be configured and ready to go.
     * @param buffer     <code>SoundReceiver.IOBuffer</code> synchronized buffer.
     */
    AudioReaderThread(final AudioRecord audioInput, final SoundReceiver.IOBuffer buffer) {
        super(AudioReaderThread.class.getSimpleName());
        this.setDaemon(true);

        this.mAudioInput = audioInput;
        this.mBuffer = buffer;
        this.mBufferSize = buffer.getBufferSize();
    }

    @Override
    public void run() {
        short[] buffer = new short[mBufferSize];
        while (!Thread.interrupted()) {
            int offset = 0;
            while (offset < mBufferSize && !Thread.interrupted()) {
                offset += mAudioInput.read(buffer, offset, mBufferSize - offset);
            }
            mBuffer.putBuffer(buffer); // blocking
        }
    }
}

/**
 * The <code>AnalyzerThread</code> works in concert with the <code>AudioReaderThread</code>. AudioReaderThread acts as
 * the producer, while AnalyzerThread is the consumer.
 * AnalyzerThread reads values from a synchronized buffer into its own data array. It subsequently performs an FFT and
 * looks for the dominant / strongest frequency in the FFT result array.
 * short[]
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
class AnalyzerThread extends Thread {
    final int mSampleRate;
    final int mBufferSize;
    final SoundReceiver.IOBuffer mBuffer;
    final Decoder mDecoder;

    final double[] xr;
    final double[] xi;
    final double[] magSQR;
    //final double[] frq;

    final double amplification = 100.0 / 32768.0; // choose a number as you like
    FFT fft = new FFT(Const.BITS);


    AnalyzerThread(final SoundReceiver.IOBuffer buffer, int sampleRate, Decoder decoder) {

        super(AnalyzerThread.class.getSimpleName());
        this.setDaemon(true);

        this.mBuffer = buffer;
        this.mBufferSize = buffer.getBufferSize();
        this.mSampleRate = sampleRate;
        this.mDecoder = decoder;

        xr = new double[mBufferSize];
        xi = new double[mBufferSize];
        magSQR = new double[mBufferSize];
    }

    @Override
    public void run() {

        while (!Thread.interrupted()) {

            short[] buffer = mBuffer.getBuffer();
            for (int i = 0; i < buffer.length; i++) {
                xr[i] = amplification * buffer[i];
                xi[i] = 0;
            }

            fft.doFFT(xr, xi, true);
            // compute the magnitudes, freq, and max:
            int mi = 0;
            for (int i = 0; i < mBufferSize / 2; i++) {
                double re = xr[i];
                double im = xi[i];
                magSQR[i] = re * re + im * im;
                if (magSQR[mi] < magSQR[i]) {
                    mi = i;
                }
            }
            double d0 = 0 < mi ? Math.abs(magSQR[mi] - magSQR[mi - 1]) : 0;
            double d1 = Math.abs(magSQR[mi] - magSQR[mi + 1]);
            final int confidence = 0;
            if (confidence < (d0 + d1)) {
                byte b = mDecoder.put(mi);
                if (b == Decoder.STX) {
                    mDecoder.getHandler().sendEmptyMessage(0);
                }
                final double Freq_Res = (double) Const.SAMPLE_RATE / Const.SAMPLES;
                int frq = (int) (Freq_Res * mi + Freq_Res / 2);
                // Log.v(SoundReceiver.LOG_TAG, "Bin, Symbol_ID, Freq, Confidence : " + mi + " ,  " + b + " ,  " + frq  + " ,  " + d0+d1 );

                Log.v(SoundReceiver.LOG_TAG, "Bin, Symbol_ID, Freq : " + mi + " ,  " + b + " ,  " + frq);
            }

        }
    }
}


/**
 * short[] backed implementation of a Producer/Consumer Queue
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
class SyncBuffer implements SoundReceiver.IOBuffer {
    private final int mBufferSize;
    private final short[] mBuffer;
    private boolean mAvailable;

    SyncBuffer(final int bufferSize) {
        this.mBufferSize = bufferSize;
        mBuffer = new short[mBufferSize];    // 16-bit buffer : – 32,768 to 32,767
    }

    /**
     * @return <code>int</code> size of the buffer, which is a constant ver the lifetime of the SyncBuffer.
     */
    public int getBufferSize() {
        return mBufferSize;
    }

    /**
     * Fills the buffer with <b>copies /b>the provided values.
     * This methods <b>blocks</b> is until buffer space becomes available.
     *
     * @param buffer <code>short</code>[]
     */
    public synchronized void putBuffer(short[] buffer) {
        while (mAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                // intentionally empty
            }
        }
        System.arraycopy(buffer, 0, mBuffer, 0, mBuffer.length);
        mAvailable = true;
        notifyAll();
    }

    /**
     * Returns the buffer.
     * This methods <b>blocks</b> is until the buffer is filled.
     *
     * @return <code>short</code>[] a copy of the current buffer.
     */
    public synchronized short[] getBuffer() {
        while (!mAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                // intentionally empty
            }
        }
        short[] buffer = new short[mBufferSize];
        System.arraycopy(mBuffer, 0, buffer, 0, mBuffer.length);
        mAvailable = false;
        notifyAll();
        return buffer;
    }

    public synchronized void release() {
        notifyAll();
    }
}


