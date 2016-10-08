package tk.kadaradam.sleepysong;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static boolean ACTION_PAUSE = false;
    public static boolean ACTION_PLAY = true;
    public static int MIN_TIME = 1;
    public static String SERVICE_INTERVAL_KEY = "TIME_MINUTES";
    public static String SERVICE_STATE_KEY = "STATE";

    public static Intent serviceIntent;
    public static BroadcastReceiver receiver;

    public static Button toggleButton;
    public static SeekBar timeSlider;
    public static TextView progressText;
    public static TextView statusText;
    public static MenuItem stopCheckBox;
    public static MenuItem startCheckBox;

    public static int countDownTime;
    public static boolean serviceState = false;
    public static boolean musicAction = ACTION_PAUSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent   = new Intent(getBaseContext(), MusicStopService.class);

        toggleButton    = (Button) findViewById(R.id.toggleButton);
        timeSlider      = (SeekBar) findViewById(R.id.timeSlider);
        progressText    = (TextView) findViewById(R.id.progressText);
        statusText      = (TextView) findViewById(R.id.statusText);


        progressText.setText(String.format(getString(R.string.app_count_minutes), MIN_TIME));
        timeSlider.setProgress(MIN_TIME);

        timeSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if(i <= MIN_TIME) {
                            timeSlider.setProgress(MIN_TIME);
                        }

                        countDownTime = (timeSlider.getProgress() * 60);

                        progressText.setText(String.format(getString(R.string.app_count_minutes), timeSlider.getProgress()));

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );

        toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                serviceState = !serviceState;

                if(serviceState) {
                    statusText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorGREEN));
                    statusText.setText      (R.string.app_enabled);
                    toggleButton.setText    (R.string.app_stop_button);

                    timeSlider.setEnabled(false);

                    // Start the service
                    serviceIntent.putExtra(SERVICE_INTERVAL_KEY, timeSlider.getProgress());
                    serviceIntent.putExtra(SERVICE_STATE_KEY, musicAction);
                    startService(serviceIntent);
                }else{
                    statusText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRED));
                    statusText.setText      (R.string.app_disabled);
                    toggleButton.setText    (R.string.app_start_button);

                    timeSlider.setEnabled(true);

                    // Stop the service
                    stopService(serviceIntent);
                }

            }
        });

        // Start a receiver: The MusicStopService when it's finished will notify the MainActivity that the UI should be reseted.
        startNotifyUIReciever();
    }

    @Override
    protected void onStart() {
        super.onStart();

		// Checking if our service is running, if not we'll reset the UI. We need this, because if the user is multitasking
		// while the music stops, and reopens our app the UI won't reseted.
        if(!isMusicServiceRunning()){
            resetUI();
        }

        // Starting the receiver when the user opens the app or reopens the minimized app
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(MusicStopService.COPA_RESULT)
        );
    }

    @Override
    protected void onStop() {
        // Stopping the receiver when the user minimizes the app
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow_menu_main, menu);

        stopCheckBox = menu.findItem(R.id.menu_stop);
        startCheckBox = menu.findItem(R.id.menu_start);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_stop:

                musicAction = ACTION_PAUSE;

                stopCheckBox.setChecked(true);
                startCheckBox.setChecked(false);

                return true;
            case R.id.menu_start:

                musicAction = ACTION_PLAY;

                startCheckBox.setChecked(true);
                stopCheckBox.setChecked(false);

                return true;
            case R.id.menu_about:
                showAppAboutInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showAppAboutInfo() {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog))
                .setTitle(R.string.dialog_about_title)
                .setMessage(R.string.dialog_about_message)
                .setNegativeButton(R.string.dialog_about_button, null)
                .show();
    }

    private void startNotifyUIReciever() {
            receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // We could filter the messages with intents, and execute different codes for each message, but
                // since we have only one broadcaster / receiver we don't need filtering.
                resetUI();
            }
        };
    }

    // Reseting the user interface
    void resetUI() {

        toggleButton    = (Button) findViewById(R.id.toggleButton);
        statusText      = (TextView) findViewById(R.id.statusText);
        timeSlider      = (SeekBar) findViewById(R.id.timeSlider);
        progressText    = (TextView) findViewById(R.id.progressText);

        statusText.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRED));
        statusText.setText(R.string.app_disabled);

        toggleButton.setText(R.string.app_start_button);

        progressText.setText(String.format(getString(R.string.app_count_minutes), MIN_TIME));
        timeSlider.setProgress(MIN_TIME);

        timeSlider.setEnabled(true);

        serviceState = false;
        countDownTime = 0;
    }

    // A simple check if our music service is active
    private boolean isMusicServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("tk.kadaradam.sleepysong.MusicStopService".equals(service.service.getClassName())) { // Change the app name to yours
                return true;
            }
        }
        return false;
    }
}
