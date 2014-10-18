package make.boiler.pebblefighter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import make.boiler.pebblefighter.Game.HostSearch;
import make.boiler.pebblefighter.Game.PlayerListener;

/**
 * Created by Matthew on 10/18/2014.
 */
public class SetupActivity extends Activity {

    private RadioButton singlePlayer;
    private RadioButton multiPlayerHost;
    private RadioButton multiPlayerClient;
    private Button start;

    private static int REQUEST_ENABLE_BT = 1;
    private static int REQUEST_ENABLE_BT_DISCOVERABLE = 2;

    public static BluetoothSocket otherPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        singlePlayer = (RadioButton) findViewById(R.id.single_player);
        multiPlayerClient = (RadioButton) findViewById(R.id.multi_player_client);
        multiPlayerHost = (RadioButton) findViewById(R.id.multi_player_host);
        start = (Button) findViewById(R.id.start_play);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestGameStart();
            }
        });
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    private  boolean isBluetoothAvailable() {
        return getBluetoothAdapter() != null;
    }

    private void requestBluetoothEnabled() {
        if(!getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            onBluetoothEnabled();
        }
    }

    private void requestBluetoothDiscoverable() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 20);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT_DISCOVERABLE);
    }

    private void onBluetoothEnabled() {
        if(multiPlayerClient.isChecked()) {
            final HostSearch hostSearch = new HostSearch(this, getBluetoothAdapter());
            final ArrayAdapter<BluetoothDevice>  hostAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setText(getItem(position).getName() + ": " + getItem(position).getAddress());
                    return tv;
                }
            };
            ListView listView = new ListView(this);
            listView.setAdapter(hostAdapter);

            final AlertDialog hostDialog = new AlertDialog.Builder(this)
                    .setTitle("Select Host")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            hostSearch.stopSearch();
                            Toast.makeText(SetupActivity.this, "Aborting host search!", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            hostSearch.stopSearch();
                            otherPlayer = (BluetoothSocket) parent.getAdapter().getItem(position);
                            Toast.makeText(SetupActivity.this, "Selected host " + otherPlayer.getRemoteDevice().getName(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    })
                    .setView(listView)
                    .create();
            hostSearch.setListener(new HostSearch.Listener() {

                @Override
                public void onDiscoverHost(BluetoothDevice device) {
                    hostAdapter.add(device);
                }

                @Override
                public void onConnectToHost(BluetoothSocket socket) {
                }

                @Override
                public void onFailToConnectToHost(BluetoothDevice device) {
                    Log.w("SetupActivity", "failed to connect to host " + device.getName() + " " + device.getAddress());
                }
            });

            hostDialog.show();
            hostSearch.startSearch();
        }
        else if(multiPlayerHost.isChecked()) {
            requestBluetoothDiscoverable();
        }
    }

    private void onBluetoothDiscoverable() {
        final PlayerListener playerListener = new PlayerListener(this, getBluetoothAdapter());
        final ProgressDialog searchingProgress = new ProgressDialog(this);
        searchingProgress.setIndeterminate(true);
        searchingProgress.setTitle("Waiting for players");
        searchingProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                playerListener.stopListening();
                Toast.makeText(SetupActivity.this, "Aborting search for clients!", Toast.LENGTH_LONG).show();
            }
        });
        playerListener.setListener(new PlayerListener.Listener() {

            @Override
            public void onPlayerConnected(final BluetoothSocket player) {
                playerListener.stopListening();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchingProgress.dismiss();
                        final AlertDialog acceptClient = new AlertDialog.Builder(SetupActivity.this)
                                .setTitle("Found client")
                                .setMessage(player.getRemoteDevice().getName() + "\n" + player.getRemoteDevice().getAddress())
                                .setNegativeButton("No", new AlertDialog.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        playerListener.beginListening();
                                        searchingProgress.show();
                                    }
                                })
                                .setPositiveButton("Yes", new AlertDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(SetupActivity.this, "accepted player " + player.getRemoteDevice().getName(), Toast.LENGTH_LONG).show();
                                    }
                                })
                                .create();
                        acceptClient.show();

                    }
                });
            }
        });
        playerListener.beginListening();
        searchingProgress.show();

    }

    public void onRequestGameStart() {
        if(singlePlayer.isChecked()) {

        }
        else if(multiPlayerClient.isChecked() || multiPlayerHost.isChecked()) {
            requestBluetoothEnabled();
        }
        else {
            Toast.makeText(this, "You must select an option before starting!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                onBluetoothEnabled();
            }
            else {
                Toast.makeText(this, "You didn't enable bluetooth! Cannot do multiplayer!", Toast.LENGTH_LONG).show();
            }
        }
        else if(requestCode == REQUEST_ENABLE_BT_DISCOVERABLE) {
            if(resultCode != RESULT_CANCELED) {
                onBluetoothDiscoverable();
            }
            else {
                Toast.makeText(this, "You didn't enable bluetooth discoverable mode! Cannot do multiplayer hosting!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
