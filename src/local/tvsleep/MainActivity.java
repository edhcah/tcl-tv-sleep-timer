package local.tvsleep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

// Shows the same choices as the sleep timer in TCL's quick settings bar and sets it the same way:
// TvSettings only writes tcl_app_power_off_time_delay (milliseconds, -1 for off), and TCL's
// PowerOffAlarmManager in System UI watches that setting and runs the countdown and warning.
public class MainActivity extends Activity {
    private static final String DELAY = "tcl_app_power_off_time_delay";
    // Absolute time the TV turns off, on one of the two system clocks; only used to show what's left.
    private static final String EXPIRE = "tcl_app_power_off_time_expire";
    private static final int[] MINUTES = {0, 10, 20, 30, 40, 50, 60, 90, 120, 240};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ContentResolver cr = getContentResolver();
        long delay = Settings.System.getLong(cr, DELAY, -1);
        String[] labels = new String[MINUTES.length];
        int current = 0;
        for (int i = 0; i < MINUTES.length; i++) {
            labels[i] = MINUTES[i] == 0 ? "Off" : MINUTES[i] + " minutes";
            if (MINUTES[i] * 60000L == delay) current = i;
        }
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(title(cr, delay))
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    set(cr, delay, MINUTES[which]);
                    dialog.dismiss();
                })
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    private static String title(ContentResolver cr, long delay) {
        if (delay <= 0) return "Sleep timer";
        long expire = Settings.System.getLong(cr, EXPIRE, 0);
        // Use whichever clock gives a remaining time that fits inside the timer that was set.
        for (long now : new long[] {SystemClock.elapsedRealtime(), SystemClock.uptimeMillis()}) {
            long left = expire - now;
            if (left > 0 && left <= delay) return "Sleep timer: " + ((left + 59999) / 60000) + " min left";
        }
        return "Sleep timer";
    }

    private void set(ContentResolver cr, long delay, int minutes) {
        long value = minutes == 0 ? -1 : minutes * 60000L;
        try {
            // Writing the same value again isn't a change, so clear it first to restart the countdown.
            if (value > 0 && value == delay) Settings.System.putLong(cr, DELAY, -1);
            Settings.System.putLong(cr, DELAY, value);
            toast(minutes == 0 ? "Sleep timer off" : "TV will turn off in " + minutes + " minutes");
        } catch (RuntimeException e) {
            toast("Couldn't set the sleep timer: " + e.getMessage());
        }
    }

    private void toast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }
}
