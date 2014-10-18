package make.boiler.pebblefighter.Game;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Matthew on 10/18/2014.
 */
public class HostConnecter {

    private static final UUID myUUID = UUID.fromString("da031144-78d1-4c27-8d70-902daab81b1f");

    public interface Listener {
        public void onConnectToHost(BluetoothSocket socket);
        public void onFailConnectToHost(BluetoothDevice device);
    }

    private Listener listener;

    public void setListener(Listener l) {
        listener = l;
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
                    listener.onFailConnectToHost(device);
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
        new ConnectThread(device).start();
    }
}
