package make.boiler.pebblefighter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import java.util.UUID;


public class TestActivity extends Activity {

    TextView textView;
    private PebbleKit.PebbleDataReceiver mReceiver;
    int count = 0, health = 100;
    final static UUID pebbleApp = UUID.fromString("1b4eb327-bb18-4b30-957f-8ab246f3e561");

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        textView = (TextView) findViewById(R.id.textView);
        boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
        Log.i(getLocalClassName(), "Pebble is " + (connected ? "connected" : "not connected"));
    }

    protected void onResume() {
        super.onResume();
        mReceiver = new PebbleKit.PebbleDataReceiver(pebbleApp) {
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                if (data.getUnsignedInteger(0) != null) {
                    int move = data.getUnsignedInteger(0).intValue();
                    if (move == 0) {
                        textView.setText("Block!" + count);
                        health--;
                    } else if (move == 1) {
                        health -= 5;
                        textView.setText("Punch!" + count);
                    }
                    PebbleDictionary dict = new PebbleDictionary();
                    dict.addInt32(0, health);
                    PebbleKit.sendDataToPebble(context, pebbleApp, dict);
                    count++;
                }
            }
        };
        PebbleKit.registerReceivedDataHandler(this, mReceiver);
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}
