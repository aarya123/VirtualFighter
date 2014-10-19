package make.boiler.pebblefighter.Game;

import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import make.boiler.pebblefighter.PlayActivity;

/**
 * Created by AnubhawArya on 10/18/14.
 */
public class Game {
    Player host, client;

    public Game() {
        host = new Player(true);
        client = new Player(false);
    }

    public void setHostCommand(Move move) {
        host.setCommand(move);
    }

    public void setClientCommand(Move move) {
        client.setCommand(move);
    }

    public void play(PlayActivity context) {
        host.doDamage(client.getCommand(), context);
        client.doDamage(host.getCommand(), context);
        Log.v("Game", "writing " + host.getHealth() + " " + client.getHealth() + " " + host.getCommandInt() + " " + client.getCommandInt());
        context.writeIntToOtherPlayer(host.getHealth());
        context.writeIntToOtherPlayer(client.getHealth());
        context.writeIntToOtherPlayer(host.getCommandInt());
        context.writeIntToOtherPlayer(client.getCommandInt());
    }

    public int getHostHealth() {
        return host.getHealth();
    }

    public int getClientHealth() {
        return client.getHealth();
    }

    public void setHostHealth(int i) {
        host.setHealth(i);
    }

    public void setClientHealth(int i) {
        client.setHealth(i);
    }

    public void setHostCommandInt(int i) {
        host.setCommandInt(i);
    }

    public void setClientCommandInt(int i) {
        client.setCommandInt(i);
    }

    public String getHostAction() {
        return host.getMove().toString() + "!!!";
    }

    public String getClientAction() {
        return client.getMove().toString() + "!!!";
    }

    public String isDone() {
        if (host.getHealth() > 0 && client.getHealth() > 0)
            return null;
        else if (host.getHealth() == 0 && client.getHealth() == 0)
            return "No one";
        else if (host.getHealth() == 0)
            return "Client";
        else if (client.getHealth() == 0)
            return "Host";
        else
            return null;
    }
}
