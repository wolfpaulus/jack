//Jack by Wolf Paulus is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
package com.techcasita.android;

import android.app.Activity;
import android.os.Bundle;
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
    SoundReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView textView = (TextView) findViewById(R.id.tv);
        ProgressBar pbar = (ProgressBar) findViewById(R.id.pb);

        textView.setMovementMethod(new ScrollingMovementMethod());

        mReceiver = new SoundReceiver(textView, pbar);
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
    public void onClick_Toggle(View view) {
        ToggleButton tb = (ToggleButton) view;
        if (tb.isChecked()) {
            mReceiver.start();
        } else {
            mReceiver.stop();
        }
    }
}
