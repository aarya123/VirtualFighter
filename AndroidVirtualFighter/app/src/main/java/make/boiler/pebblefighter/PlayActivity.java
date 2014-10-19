package make.boiler.pebblefighter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import make.boiler.pebblefighter.Game.Game;
import make.boiler.pebblefighter.Game.Move;

public class PlayActivity extends Activity {

    public static final String EXTRA_ROLE = "role";
    public final static UUID pebbleApp = UUID.fromString("1b4eb327-bb18-4b30-957f-8ab246f3e561");
    public BluetoothSocket otherPlayer;
    Button startButton;
    View hostHealthBar, clientHealthBar;
    TextView hostAction, clientAction;
    Game game = new Game();
    PebbleKit.PebbleDataReceiver pebbleDataReceiver;
    boolean isHost = true, mStopHandler = false;
    ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    Handler mForegroundHandler;
    int maxHeight = 0;

    //Need a spot to accept and set client command
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        isHost = getIntent().getBooleanExtra(EXTRA_ROLE, true);
        startButton = (Button) findViewById(R.id.startButton);
        hostHealthBar = findViewById(R.id.hostHealthBar);
        clientHealthBar = findViewById(R.id.clientHealthBar);
        hostAction = (TextView) findViewById(R.id.hostAction);
        clientAction = (TextView) findViewById(R.id.clientAction);
        mForegroundHandler = new Handler(Looper.getMainLooper());
        startButton.setEnabled(isHost);
        otherPlayer = SetupActivity.otherPlayer;
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startGame();
                startButton.setEnabled(false);
            }
        });
    }

    public void writeIntToOtherPlayer(final int value) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    otherPlayer.getOutputStream().write(value);
                } catch (IOException e) {
                    Log.e("PlayActivity", "Error writing move", e);
                }
            }
        });

    }

    public interface IntReader {
        public void onReadInt(int i);
    }

    public void readIntFromOtherPlayer(final IntReader intReader ) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                int readInt;
                try {
                    readInt = otherPlayer.getInputStream().read();
                } catch (IOException e) {
                    Log.e("PlayActivity", "Error reading move", e);
                    readInt = 0;
                }
                intReader.onReadInt(readInt);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        pebbleDataReceiver = new PebbleKit.PebbleDataReceiver(pebbleApp) {
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                if (data.getUnsignedInteger(0) != null) {
                    int value = data.getUnsignedInteger(0).intValue();
                    if (isHost) {
                        game.setHostCommand(Move.values()[value]);
                    } else {
                        writeIntToOtherPlayer(value);
                    }
                }
            }
        };
        PebbleKit.registerReceivedDataHandler(this, pebbleDataReceiver);
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(pebbleDataReceiver);
    }

    private void startGame() {
        game = new Game();
        Runnable runnable = new Runnable() {
            public void run() {
                hostAction.setText(game.getHostAction());
                clientAction.setText(game.getClientAction());
                game.play(PlayActivity.this);
                if (maxHeight == 0)
                    maxHeight = hostHealthBar.getHeight();
                LinearLayout.LayoutParams temp = (LinearLayout.LayoutParams) hostHealthBar.getLayoutParams();
                temp.height = (int) (game.getHostHealth() / 100.0 * maxHeight);
                hostHealthBar.setLayoutParams(temp);
                temp = (LinearLayout.LayoutParams) clientHealthBar.getLayoutParams();
                temp.height = (int) (game.getClientHealth() / 100.0 * maxHeight);
                clientHealthBar.setLayoutParams(temp);
                String result = game.isDone();
                if (result != null) {
                    mStopHandler = true;
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlayActivity.this);
                    alertDialogBuilder.setTitle("Game Over!");
                    alertDialogBuilder.setMessage(result + " wins!");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    temp = (LinearLayout.LayoutParams) hostHealthBar.getLayoutParams();
                    temp.height = maxHeight;
                    hostHealthBar.setLayoutParams(temp);
                    temp = (LinearLayout.LayoutParams) clientHealthBar.getLayoutParams();
                    temp.height = maxHeight;
                    clientHealthBar.setLayoutParams(temp);
                }
                if (!mStopHandler) {
                    mForegroundHandler.postDelayed(this, 500);
                }
            }
        };
        Runnable otherPlayerUpdater = new Runnable() {
            public void run() {
                readIntFromOtherPlayer(new IntReader() {
                    @Override
                    public void onReadInt(final int i) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Move move = Move.values()[i];
                                Log.d("MoveReceived", move.toString());
                                game.setClientCommand(move);
                                if (!mStopHandler) {
                                    mForegroundHandler.postDelayed(this, 100);
                                }
                            }
                        });
                    }
                });

            }
        };
        mForegroundHandler.post(runnable);
        mForegroundHandler.post(otherPlayerUpdater);
    }
}