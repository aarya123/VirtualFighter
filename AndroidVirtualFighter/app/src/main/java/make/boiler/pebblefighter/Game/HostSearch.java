package make.boiler.pebblefighter.Game;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Matthew on 10/18/2014.
 */
public class HostSearch {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isSearching;
    private Listener listener;
    private static final UUID myUUID = UUID.fromString("da031144-78d1-4c27-8d70-902daab81b1f");

    public interface Listener {
        public void onDiscoverHost(BluetoothDevice device);
        public void onConnectToHost(BluetoothSocket socket);
        public void onFailToConnectToHost(BluetoothDevice device);
    }

    public HostSearch(Context context, BluetoothAdapter adapter) {
        this.context = context;
        bluetoothAdapter = adapter;
        if(!bluetoothAdapter.isEnabled()) {
            throw new IllegalArgumentException("adapter must be enabled!");
        }
        isSearching = false;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void startSearch() {
        if(!isSearching) {
            if (listener != null) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    listener.onDiscoverHost(device);
                }
            }
            boolean state = bluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            context.registerReceiver(mReceiver, filter);
            Log.v("Start Search", "register receiver + start discovery " + state);
        }
        isSearching = true;
    }

    private class ConnectThread extends Thread {
        BluetoothSocket socket;
        BluetoothDevice device;
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
                socket = device.createRfcommSocketToServiceRecord(myUUID);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                socket.connect();
            }
            catch(IOException ioe) {
                try {
                    socket.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                    Log.e("HostSearch", "Couldn't connect to player!");
                }
                if(listener != null) {
                    listener.onFailToConnectToHost(device);
                }
                else {
                    Log.e("HostSearch", "failed to connect to host but no listener!");
                }
                return;
            }
            if(listener != null) {
                listener.onConnectToHost(socket);
            }
            else {
                Log.e("HostSearch", "connected to host but no listener!");
                try {
                    socket.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                    Log.e("HostSearch", "couldn't close socket properly");
                }
            }
        }
    }

    public void connectToHost(BluetoothDevice device) {
        stopSearch();
        new ConnectThread(device).start();
    }

    public void stopSearch() {
        if(isSearching) {
            bluetoothAdapter.cancelDiscovery();
            context.unregisterReceiver(mReceiver);
        }
        isSearching = false;
    }

    private void onDiscoverDevice(BluetoothDevice device) {
        if(listener != null) {
            listener.onDiscoverHost(device);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.v("DISCOVER", device.getName() + ": " + device.getAddress());

                Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if(uuids != null) {
                    for (Parcelable uuid : uuids) {
                        Log.v("UUID", uuid.toString());
                    }
                }
                else {
                    Log.v("UUID", "NO UUIDS for device " + device.getName() + " " + device.getAddress());
                }
                onDiscoverDevice(device);
            }
            else {
                Log.v("ACTION", action);
            }
        }
    };
}
