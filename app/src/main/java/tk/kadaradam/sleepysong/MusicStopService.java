package tk.kadaradam.sleepysong;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.Date;


public class MusicStopService extends Service {

    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";

    private Handler handler = new Handler();
    public static Intent intent_musicService;

    public static LocalBroadcastManager broadcaster;

    private static boolean musicAction;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int
                stopTime    = intent.getIntExtra(MainActivity.SERVICE_INTERVAL_KEY, MainActivity.MIN_TIME);     // Getting the time value from the main activity
                musicAction = intent.getBooleanExtra(MainActivity.SERVICE_STATE_KEY, MainActivity.ACTION_PAUSE);// Getting the state (play or pause)

        stopTime = (stopTime * 1000) * 60;
        handler.postDelayed(StopMusic, stopTime);

        intent_musicService = intent;

        Date d = new Date();
        CharSequence s = DateFormat.format("hh:mm:ss", d.getTime() + stopTime);

        //Toast.makeText(this, "Music will " + (musicAction ? "play" : "stop") + " at " + s, Toast.LENGTH_LONG).show();
        Toast.makeText(this, String.format(getString(R.string.music_stop_time), (musicAction ? "play" : "stop"), s), Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Registering the broadcaster
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Code for stopping the timer if someone stops the service
        handler.removeCallbacks(StopMusic);
    }


    private Runnable StopMusic = new Runnable() {
        @Override
        public void run() {

            // Sending a message to the MainActivity that the UI should be reseted.
            updateMainUI();

            // Stopping / Playing the music
            if(isMusicActive() != musicAction)
                toggleMusic();

            // Stop the service
            if(intent_musicService != null)
                stopService(intent_musicService);
        }
    };

    // Sending an update user interface request to the main activity
    private void updateMainUI() {

        Intent intent = new Intent(COPA_RESULT);
        intent.putExtra(COPA_MESSAGE, "update"); // The message can be anything, since we have only one broadcaster / receiver
        broadcaster.sendBroadcast(intent);
    }

    private void toggleMusic() {

        AudioManager am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        long eventtime = SystemClock.uptimeMillis() - 1;
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
        am.dispatchMediaKeyEvent(downEvent);

        eventtime++;
        KeyEvent upEvent = new KeyEvent(eventtime,eventtime,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
        am.dispatchMediaKeyEvent(upEvent);
    }

    private boolean isMusicActive() {
        AudioManager manager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        return manager.isMusicActive();
    }
}
