package make.boiler.pebblefighter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Set;
import java.util.UUID;

import make.boiler.pebblefighter.Game.Game;
import make.boiler.pebblefighter.Game.Move;


public class TestActivity extends Activity {

    TextView textView;
    ListView listView;
    ArrayAdapter<String> adapter;
    BluetoothAdapter bluetoothAdapter;
    PebbleKit.PebbleDataReceiver pebbleDataReceiver;
    public final static UUID pebbleApp = UUID.fromString("1b4eb327-bb18-4b30-957f-8ab246f3e561");
    static final int REQUEST_ENABLE_BT = 1;
    boolean isHost = true;
    Game game;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        textView = (TextView) findViewById(R.id.textView);
        /*listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support bluetooth!", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                showBondedList();
                if (bluetoothAdapter.startDiscovery()) {
                    // Register the BroadcastReceiver
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(pebbleDataReceiver, filter); // Don't forget to unregister during onDestroy
                } else {
                    Toast.makeText(this, "Can't start discovery!", Toast.LENGTH_LONG).show();
                }
            }
        }*/
        if (isHost) {
            game = new Game();
        } else {
            //Send commands and wait for health
        }
    }

    protected void onResume() {
        super.onResume();
        pebbleDataReceiver = new PebbleKit.PebbleDataReceiver(pebbleApp) {
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);
                if (data.getUnsignedInteger(0) != null) {
                    Move move = Move.values()[data.getUnsignedInteger(0).intValue()];
                    if(isHost) {
                        game.setHostCommand(move);
                    }
                }
            }
        };
        PebbleKit.registerReceivedDataHandler(this, pebbleDataReceiver);
    }

    /*private void showBondedList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }   // Create a BroadcastReceiver for ACTION_FOUND

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
            bluetoothAdapter.cancelDiscovery();
        }
    };

    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        } catch (RuntimeException e) {
            //Already unregistered
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showBondedList();
            } else {
                Toast.makeText(this, "you should allow bluetooth!", Toast.LENGTH_LONG).show();
            }
        }
    }*/

    protected void onPause() {
        super.onPause();
        unregisterReceiver(pebbleDataReceiver);
    }
}
