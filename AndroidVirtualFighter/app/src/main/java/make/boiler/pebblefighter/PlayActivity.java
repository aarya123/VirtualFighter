package make.boiler.pebblefighter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

import make.boiler.pebblefighter.Game.Game;
import make.boiler.pebblefighter.Game.Move;
import make.boiler.pebblefighter.R;

public class PlayActivity extends Activity {

    RadioButton hostButton, clientButton;
    Game game;
    PebbleKit.PebbleDataReceiver pebbleDataReceiver;
    public final static UUID pebbleApp = UUID.fromString("1b4eb327-bb18-4b30-957f-8ab246f3e561");

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        hostButton = (RadioButton) findViewById(R.id.hostButton);
        clientButton = (RadioButton) findViewById(R.id.clientButton);
        hostButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.d("Test", "new game");
                    game = new Game();
                } else {
                    Log.d("Test", "delete game");
                    game = null;
                }
            }
        });
    }

    protected void onResume() {
        super.onResume();
        pebbleDataReceiver = new PebbleKit.PebbleDataReceiver(pebbleApp) {
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                if (data.getUnsignedInteger(0) != null) {
                    Move move = Move.values()[data.getUnsignedInteger(0).intValue()];
                    /*if (isHost) {
                        game.setHostCommand(move);
                    }*/
                }
            }
        };
        PebbleKit.registerReceivedDataHandler(this, pebbleDataReceiver);
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(pebbleDataReceiver);
    }
}
