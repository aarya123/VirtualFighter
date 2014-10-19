package make.boiler.pebblefighter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import make.boiler.pebblefighter.Game.HostConnecter;
import make.boiler.pebblefighter.Game.HostSearch;
import make.boiler.pebblefighter.Game.PlayerListener;

/**
 * Created by Matthew on 10/18/2014.
 */
public class SetupActivity extends Activity {

    public static InputStream otherPlayerIn;
    public static OutputStream otherPlayerOut;
    private static int REQUEST_ENABLE_BT = 1;
    private static int REQUEST_ENABLE_BT_DISCOVERABLE = 2;
    boolean isHost;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Button singlePlayer = (Button) findViewById(R.id.singlePlayer);
        Button joinMatch = (Button) findViewById(R.id.joinMatch);
        Button hostMatch = (Button) findViewById(R.id.hostMatch);
        singlePlayer.setOnClickListener(view -> {
            isHost = true;
            startGameSinglePlayer();
        });
        joinMatch.setOnClickListener(view -> {
            isHost = false;
            requestBluetoothEnabled();
        });
        hostMatch.setOnClickListener(view -> {
            isHost = true;
            requestBluetoothEnabled();
        });
    }

    private BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    private boolean isBluetoothAvailable() {
        return getBluetoothAdapter() != null;
    }

    private void requestBluetoothEnabled() {
        if (!getBluetoothAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
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
        if (!isHost) {
            final HostSearch hostSearch = new HostSearch(this, getBluetoothAdapter());
            final ArrayAdapter<BluetoothDevice> hostAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setText(getItem(position).getName() + ": " + getItem(position).getAddress());
                    return tv;
                }
            };
            final AlertDialog hostDialog;
            final ListView listView = new ListView(this);
            hostDialog = new AlertDialog.Builder(this)
                    .setTitle("Select Host")
                    .setOnCancelListener(dialog -> {
                        hostSearch.stopSearch();
                        Toast.makeText(SetupActivity.this, "Aborting host search!", Toast.LENGTH_LONG).show();
                    })
                    .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            hostSearch.stopSearch();
                            Toast.makeText(SetupActivity.this, "Selected host " + hostAdapter.getItem(position).getName(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    })
                    .setView(listView)
                    .create();
            listView.setAdapter(hostAdapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                final ProgressDialog hostConnectProgress = new ProgressDialog(SetupActivity.this);
                hostConnectProgress.setTitle("Connecting to host");
                hostConnectProgress.setIndeterminate(true);
                hostConnectProgress.setOnCancelListener(dialog -> Toast.makeText(SetupActivity.this, "Canceling connecting to host!", Toast.LENGTH_LONG).show());
                HostConnecter connecter = new HostConnecter();
                connecter.setListener(new HostConnecter.Listener() {
                    public void onConnectToHost(final BluetoothSocket socket) {
                        runOnUiThread(() -> {
                            if (!hostConnectProgress.isShowing()) {
                                Log.v("HostConnect", "stopping due to dismiss");
                                try {
                                    socket.close();
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                                return;
                            }
                            hostConnectProgress.dismiss();
                            try {
                                otherPlayerIn = socket.getInputStream();
                                otherPlayerOut = socket.getOutputStream();
                                int response = otherPlayerIn.read();
                                if (response == 1) {
                                    startGameAsClient(socket);
                                } else {
                                    Toast.makeText(SetupActivity.this, "Failed due to host rejection", Toast.LENGTH_LONG).show();
                                    hostDialog.show();
                                    hostSearch.startSearch();
                                }
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                                Toast.makeText(SetupActivity.this, "Couldn't read host response", Toast.LENGTH_LONG).show();
                                hostDialog.show();
                                hostSearch.startSearch();
                            }
                        });

                    }

                    @Override
                    public void onFailConnectToHost(BluetoothDevice device) {
                        runOnUiThread(() -> {
                            if (!hostConnectProgress.isShowing()) {
                                Log.v("HostConnect", "fail to connect while canceled");
                                return;
                            }
                            hostConnectProgress.dismiss();
                            new AlertDialog.Builder(SetupActivity.this)
                                    .setTitle("Problem connecting to host")
                                    .setMessage("Try someone else")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        hostDialog.show();
                                        hostSearch.startSearch();
                                    })
                                    .setOnCancelListener(dialog -> {
                                        hostDialog.show();
                                        hostSearch.startSearch();
                                    })
                                    .create()
                                    .show();
                        });
                    }
                });
                hostDialog.dismiss();
                hostConnectProgress.show();
                hostSearch.stopSearch();
                connecter.connectToHost(hostAdapter.getItem(position));
            });
            hostSearch.setListener(hostAdapter::add);
            hostDialog.show();
            hostSearch.startSearch();
        } else if (isHost) {
            requestBluetoothDiscoverable();
        }
    }

    private void onBluetoothDiscoverable() {
        final PlayerListener playerListener = new PlayerListener(this, getBluetoothAdapter());
        final ProgressDialog searchingProgress = new ProgressDialog(this);
        searchingProgress.setIndeterminate(true);
        searchingProgress.setTitle("Waiting for players");
        searchingProgress.setOnCancelListener(dialog -> {
            playerListener.stopListening();
            Toast.makeText(SetupActivity.this, "Aborting search for clients!", Toast.LENGTH_LONG).show();
        });
        playerListener.setListener(player -> {
            playerListener.stopListening();
            runOnUiThread(() -> {
                searchingProgress.dismiss();
                final AlertDialog acceptClient = new AlertDialog.Builder(SetupActivity.this)
                        .setTitle("Found client")
                        .setMessage(player.getRemoteDevice().getName() + "\n" + player.getRemoteDevice().getAddress())
                        .setNegativeButton("No", (dialog, which) -> {
                            try {
                                player.getOutputStream().write(0);
                                player.close();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                            playerListener.beginListening();
                            searchingProgress.show();
                        })
                        .setPositiveButton("Yes", (dialog, which) -> {
                            try {
                                otherPlayerIn = player.getInputStream();
                                otherPlayerOut = player.getOutputStream();
                                otherPlayerOut.write(1);
                                startGameAsHost(player);
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                                Toast.makeText(SetupActivity.this, "Failed to accept player!", Toast.LENGTH_LONG);
                                playerListener.beginListening();
                                searchingProgress.show();
                            }
                        })
                        .create();
                acceptClient.show();

            });
        });
        playerListener.beginListening();
        searchingProgress.show();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                onBluetoothEnabled();
            } else {
                Toast.makeText(this, "You didn't enable bluetooth! Cannot do multiplayer!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BT_DISCOVERABLE) {
            if (resultCode != RESULT_CANCELED) {
                onBluetoothDiscoverable();
            } else {
                Toast.makeText(this, "You didn't enable bluetooth discoverable mode! Cannot do multiplayer hosting!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Intent getClientIntent() {
        return new Intent()
                .setClass(this, PlayActivity.class)
                .putExtra(PlayActivity.EXTRA_ROLE, false);
    }

    private Intent getHostIntent() {
        return new Intent()
                .setClass(this, PlayActivity.class)
                .putExtra(PlayActivity.EXTRA_ROLE, true);
    }

    private void startGameSinglePlayer() {
        startActivity(new Intent()
                .setClass(this, PlayActivity.class));
    }

    private void startGameAsClient(BluetoothSocket host) {
        Toast.makeText(this, "Start game as client!", Toast.LENGTH_LONG).show();
        startActivity(getClientIntent());
    }

    private void startGameAsHost(BluetoothSocket client) {
        Toast.makeText(this, "Start game as host!", Toast.LENGTH_LONG).show();
        startActivity(getHostIntent());
    }
}
