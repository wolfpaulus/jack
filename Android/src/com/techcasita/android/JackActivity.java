//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
package com.techcasita.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.techcasita.android.sound.SoundReceiver;

/**
 * Launcher Activity
 *
 * @author <a href="mailto:wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class JackActivity extends Activity {
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (0 < msg.what) {
                mTextView.setText("");
            } else {
                if (msg.getData() != null) {
                    String s = msg.getData().getString("Text");
                    mTextView.setText(s);
                    try {
                        mProgressBar.setProgress(Integer.parseInt(s) / 10);
                    } catch (NumberFormatException e) {
                        // intentionally empty
                    }
                }
            }
        }
    };

    private TextView mTextView;
    private ProgressBar mProgressBar;
    private SoundReceiver mReceiver;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTextView = (TextView) findViewById(R.id.tv);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.setText("");

        mProgressBar = (ProgressBar) findViewById(R.id.pb);
        mProgressBar.setProgress(0);

        mReceiver = new SoundReceiver(mHandler);
    }

    @Override
    protected void onPause() {
        mReceiver.stop();
        super.onPause();
        System.runFinalizersOnExit(true);
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        mReceiver.destroy();
        super.onDestroy();
    }

    @SuppressWarnings("UnusedParameters")
    public void onClick_Toggle(final View view) {
        ToggleButton tb = (ToggleButton) view;
        if (tb.isChecked()) {
            mReceiver.start();
        } else {
            mReceiver.stop();
        }
    }
}
