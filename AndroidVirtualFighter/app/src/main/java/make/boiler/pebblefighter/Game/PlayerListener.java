package make.boiler.pebblefighter.Game;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Matthew on 10/18/2014.
 */
public class PlayerListener {
    private static final UUID myUUID = UUID.fromString("da031144-78d1-4c27-8d70-902daab81b1f");
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private Listener listener;
    private ListenThread listenThread;
    private static final String NAME = "player_listener";

    public interface Listener {
        public void onPlayerConnected(BluetoothSocket player);
    }

    /**
     * You should be in discovery mode before creating this
     * @param context
     * @param adapter
     */
    public PlayerListener(Context context, BluetoothAdapter adapter) {
        this.context = context;
        bluetoothAdapter = adapter;
        if(!bluetoothAdapter.isEnabled()) {
            throw new IllegalArgumentException("Bluetooth adapter must be enabled!");
        }
        listenThread = null;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private class ListenThread extends Thread {

        private BluetoothServerSocket serverSocket;

        public ListenThread() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, myUUID);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }


        private void closeSocket(BluetoothSocket s) {
            try {
                s.close();
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
        @Override
        public void run() {
            BluetoothSocket socket;
            while(!isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                }
                catch(IOException e) {
                    e.printStackTrace();
                    break;
                }
                if(socket != null) {
                    if(listener != null) {
                        listener.onPlayerConnected(socket);
                        break;
                    }
                    else {
                        Log.w("PlayerListener", "someone connected but no listener to respond!");
                        closeSocket(socket);
                    }
                }
            }
            try {
                serverSocket.close();
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }

        }
    }

    public void beginListening() {
        if(listenThread != null) {
            stopListening();
        }
        listenThread = new ListenThread();
        listenThread.start();
    }

    public void stopListening() {
        if(listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }
    }
}
