package make.boiler.pebblefighter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import make.boiler.pebblefighter.Game.Game;
import make.boiler.pebblefighter.Game.Move;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class PlayActivity extends Activity {

    public static final String EXTRA_ROLE = "role";
    public final static UUID pebbleApp = UUID.fromString("1b4eb327-bb18-4b30-957f-8ab246f3e561");
    Button startButton;
    View hostHealthBar, clientHealthBar;
    TextView hostAction, clientAction;
    Game game = new Game();
    PebbleKit.PebbleDataReceiver pebbleDataReceiver;
    boolean isHost = true;
    int maxHeight = 0;
    Scheduler.Worker writeCommandWorker = null;
    Scheduler.Worker gameLoop = null;
    Subscription gameLoopSubscription = null;
    InputStream otherPlayerIn = null;
    OutputStream otherPlayerOs = null;


    private Subscription otherPlayerCommandStreamSubscription;

    public Observable<Integer> getOtherPlayerCommandStream() {
        return Observable.create((Observable.OnSubscribe<Integer>) subscriber -> {
            try {
                int nextCommand;
                Log.v("OtherPlayerCommandStream", "going to read");
                while((nextCommand = otherPlayerIn.read()) != -1) {
                    Log.v("OtherPlayerCommandStream", Integer.toString(nextCommand));
                    subscriber.onNext(nextCommand);
                }
                Log.v("OtherPlayerCommandStream", Integer.toString(nextCommand));
            }
            catch(IOException ioe) {
                subscriber.onError(ioe);
            }
            subscriber.onCompleted();
        })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

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
        startButton.setEnabled(isHost);
        game = new Game();
        otherPlayerOs = SetupActivity.otherPlayerOut;
        otherPlayerIn = SetupActivity.otherPlayerIn;
        otherPlayerCommandStreamSubscription = getOtherPlayerCommandStream().subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                Log.v("onCompleted", "command stream finished");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.v("onError", "problem reading from command stream!");
            }

            boolean clientStartedGame = false;
            int clientPlaceInSequence = 1;
            final int hostHealth = 0;
            final int clientHealth = 1;
            final int hostCommand = 2;
            final int clientCommand = 3;
            final int sequenceSize = 4;
            @Override
            public void onNext(Integer i) {



                if(isHost) {
                    Move move = Move.values()[i];
                    Log.d("MoveReceived", move.toString());
                    game.setClientCommand(move);
                }
                else {
                    if(!clientStartedGame) {
                        clientStartedGame = true;
                        startGame();
                        return;
                    }
                    if(i == 120) {
                        PebbleDictionary dict = new PebbleDictionary();
                        dict.addInt32(0, -1);
                        PebbleKit.sendDataToPebble(PlayActivity.this, PlayActivity.pebbleApp, dict);
                        closeOtherPlayerConnections();
                        finish();
                    }
                    else {
                        switch(clientPlaceInSequence) {
                            case hostHealth:
                                Log.v("onNext", "hostHealth = " + i);
                                game.setHostHealth(i);
                                break;
                            case clientHealth:
                                Log.v("onNext", "clientHealth = " + i);
                                PebbleDictionary dict = new PebbleDictionary();
                                dict.addInt32(0, i);
                                PebbleKit.sendDataToPebble(PlayActivity.this, PlayActivity.pebbleApp, dict);
                                game.setClientHealth(i);
                                break;
                            case hostCommand:
                                Log.v("onNext", "hostCommand = " + i);
                                game.setHostCommandInt(i);
                                break;
                            case clientCommand:
                                Log.v("onNext", "clientCommand = " + i);
                                game.setClientCommandInt(i);
                                break;
                            default:
                                throw new RuntimeException("unknown place in client sequence!");
                            }
                        }
                    clientPlaceInSequence++;
                    clientPlaceInSequence %= sequenceSize;
                }
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startGame();
                startButton.setEnabled(false);
            }
        });
    }


    public void writeIntToOtherPlayer(int value) {
        if(writeCommandWorker == null) {
            writeCommandWorker = Schedulers.io().createWorker();
        }
        writeCommandWorker.schedule(() -> {
            try {
                Log.v("WriteCommandWorker", "schedule write " + value);
                otherPlayerOs.write(value);
                otherPlayerOs.flush();
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
                Log.e("PlayActivity", "error writing command");
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
        Log.v("startGame", "called");
        if(gameLoop == null) {
            gameLoop = AndroidSchedulers.mainThread().createWorker();
        }
        gameLoopSubscription = gameLoop.schedulePeriodically(() -> {
            hostAction.setText(game.getHostAction());
            clientAction.setText(game.getClientAction());
            if(isHost) {
                game.play(PlayActivity.this);
                PebbleDictionary dict = new PebbleDictionary();
                dict.addInt32(0, game.getHostHealth());
                PebbleKit.sendDataToPebble(PlayActivity.this, PlayActivity.pebbleApp, dict);
            }
            if (maxHeight == 0)
                maxHeight = hostHealthBar.getHeight();
            LinearLayout.LayoutParams temp = (LinearLayout.LayoutParams) hostHealthBar.getLayoutParams();
            temp.height = (int) (game.getHostHealth() / 100.0 * maxHeight);
            hostHealthBar.setLayoutParams(temp);
            temp = (LinearLayout.LayoutParams) clientHealthBar.getLayoutParams();
            temp.height = (int) (game.getClientHealth() / 100.0 * maxHeight);
            clientHealthBar.setLayoutParams(temp);
            if(isHost) {
                String result = game.isDone();
                if (result != null) {
                    gameLoopSubscription.unsubscribe();
                    gameLoop.unsubscribe();
                    otherPlayerCommandStreamSubscription.unsubscribe();
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PlayActivity.this);
                    alertDialogBuilder.setTitle("Game Over!");
                    alertDialogBuilder.setMessage(result + " wins!");
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });
                    temp = (LinearLayout.LayoutParams) hostHealthBar.getLayoutParams();
                    temp.height = maxHeight;
                    hostHealthBar.setLayoutParams(temp);
                    temp = (LinearLayout.LayoutParams) clientHealthBar.getLayoutParams();
                    temp.height = maxHeight;
                    clientHealthBar.setLayoutParams(temp);
                    try {
                        otherPlayerOs.write(120);
                        otherPlayerOs.flush();
                    }
                    catch(IOException ioe) {
                        Log.e("PlayActivity", "couldn't finish match for client!");
                        ioe.printStackTrace();
                    }
                    finally {
                        closeOtherPlayerConnections();
                    }
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

    }

    private void closeOtherPlayerConnections() {
        try {
            otherPlayerOs.close();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            otherPlayerIn.close();
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
