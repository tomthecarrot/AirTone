package com.carrotcorp.airtone2;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

/**
 * AirTone light-based sound app.
 * by Thomas Suarez, Chief Engineer at CarrotCorp.
 */
public class AirTone extends Activity implements SensorEventListener {

    int splashDelaySec = 2; // time splash screen is on
    boolean modulate = false; // should sound be slightly modulated
    SensorManager sensorManager; // light sensor manager
    Thread thread; // thread for audio synthesis
    AudioTrack audioTrack; // audio generator
    boolean isRunning = false; // is the sound playing
    float lightValue = 0; // current light value from the sensor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the splash screen
        setContentView(R.layout.splash);

        // Hide the splash screen and show the game screen
        // after a delay (not implemented yet)
        //delaySplash();

        // Create the light sensor manager object
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Start audio
        changeState(true);
    }

    private void delaySplash() {
        Handler h = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.main);
            }
        };
        h.postDelayed(r, splashDelaySec * 1000);
    }

    /**
     * Synthesizes audio tones.
     * @see "http://audioprograming.wordpress.com/2012/10/18/a-simple-synth-in-android-step-by-step-guide-using-the-java-sdk/"
     */
    private void createAudio() {
        // Start a new Thread to create audio
        thread = new Thread() {
            public void run() {
                // Start audio synthesis track
                int sr = 44100;
                int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffsize, AudioTrack.MODE_STREAM);

                AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

                // Set math vars
                short samples[] = new short[buffsize];
                int amp = 10000;
                double twopi = 8.*Math.atan(1.);
                double ph = 0.0;

                // Start the synthesis track playing
                audioTrack.play();

                // Make two tones in modulation (first high, second low) if preferred
                int modCnt;
                if (modulate) {
                    modCnt = 2;
                }
                else {
                    modCnt = 1;
                }

                // Loop the synthesis
                while (isRunning) {
                    for (int j=0; j < modCnt; j++) {
                        double fr = j*200 + 5*lightValue;

                        for (int i=0; i < buffsize; i++) {
                            samples[i] = (short) (amp*Math.sin(ph));
                            ph += twopi*fr/sr;
                        }
                        audioTrack.write(samples, 0, buffsize);
                    }
                }
            }
        };
        thread.start();
    }

    /**
     * Turns off audio, change modulation state,
     * and re-open audio track after it has closed
     * @param view
     */
    public void toggleModulate(View view) {
        changeState(false);
        modulate = !modulate;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                changeState(true);
            }
        };
        new Handler().postDelayed(r, 500);
    }

    /**
     * Changes the state of this application's various tasks.
     * @param state On/True, Off/False
     */
    private void changeState(boolean state) {
        if (state && !isRunning) { // Turn on
            // Register the light sensor listener
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST);

            // Create and start audio
            isRunning = true;
            createAudio();
        }
        else if (!state && isRunning) { // Turn off
            // If the app is exited or interrupted
            sensorManager.unregisterListener(this);

            // Stop the AudioTrack
            isRunning = false;
            audioTrack.stop();
            audioTrack.release();

            // Stop the thread
            thread.interrupt();
            thread = null;
        }
    }

    public void onResume() {
        super.onResume();

        if (sensorManager != null) {
            changeState(true);
        }
    }

    public void onPause() {
        super.onPause();

        if (sensorManager != null) {
            changeState(false);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get the distance value and log it:
        lightValue = event.values[0];
        Log.d("AirTone", "value: " + lightValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // unused
    }

}
