//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

package com.techcasita.android.sound;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class Decoder {


    private static final int MAX_MESSAGE_SIZE = 1024;

    static final char STX = '?';  // Start Token
    static final char ETX = '@';  // End Token

    private static final int UNDEFINED = 0;
    private static final int START_TEXT = 1;


    //
    //  Message buffer
    //
    private final byte[] bb = new byte[MAX_MESSAGE_SIZE * Const.REPEAT];

    private int mDecoderState = UNDEFINED;
    private int bbi = 0;
    private int mCounter = 0;
    private Handler mHandler;


    public Decoder(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Reverse the encoding process. An FFT id is provided.
     *
     * @param bin <code>int</code>
     * @return <code>int</code>  value
     */
    public byte decode(int bin) {
        int k = (int) ((bin - Const.OFFSET) / 5.0 + 45);
        return (byte) (0x7F & k);
    }


    /**
     * A sound sample that was received by the SoundReceiver and put through an FFT maybe encoded in either a message
     * nibble or may represent as start or stop symbol.
     *
     * @param k <code>int</code> frequency bucket that needs to be considered for decoding
     * @return <code>byte</code> uninterpreted, decoded byte value, mainly for debugging
     */
    byte put(int k) {
        byte b = decode(k);
        switch (b) {
            case STX: // unique command symbol
                mCounter++;
                if (mCounter + 1 >= Const.REPEAT) {
                    mDecoderState = START_TEXT;
                    //Log.v("Decoder", "START_TEXT");
                    mCounter = 0;
                    bbi = 0;
                }
                break;

            case ETX: // unique command symbol
                if (mDecoderState == START_TEXT) {
                    mCounter++;
                    if (mCounter + 1 >= Const.REPEAT) {
                        mDecoderState = UNDEFINED;
                        //Log.v("Decoder", "END_TRANSMISSION");
                        mCounter = 0;

                        decode();
                    }
                }
                break;

            default:
                if (mDecoderState == START_TEXT && bbi < MAX_MESSAGE_SIZE) {
                    mCounter = 0;
                    bb[bbi++] = b;
                    //Log.v("EnDeco", " payload size: " + bbi);
                }

        }
        return b;
    }

    void decode() {
        String s = "";
        //Log.v("EnDeco", Arrays.toString(bb));
        if (0 < bbi) {
            byte[] ta = new byte[bbi];
            ta[0] = bb[0];
            ta[bbi - 1] = bb[bbi - 1];
            for (int i = 1; i < bbi - 1; i++) {
                ta[i] = (byte) Decoder.median(bb[i - 1], bb[i], bb[i + 1]);
            }

            byte[] ra = new byte[bbi];
            int j = 0;
            int i = 0;
            while (i < bbi - 2) {

                if (ta[i] == ta[i + 1]) {
                    ra[j++] = ta[i++];
                    if (ta[i] == ta[i + 1]) {
                        i++;
                    }
                }
                i++;
            }
            s = new String(ra, 0, j);
        }
        bbi = 0;


        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("Text", s);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

    }

    private static int median(int fa, int fb, int fc) {
        int median;
        if (fa <= fb) {
            median = fb <= fc ? fb : Math.max(fa, fc);
        } else { // here fb<fa
            median = fc <= fb ? fb : Math.min(fa, fc);
        }
        return median;
    }
}
