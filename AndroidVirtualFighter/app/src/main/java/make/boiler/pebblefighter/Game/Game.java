package make.boiler.pebblefighter.Game;

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
        context.writeIntToOtherPlayer(host.getHealth());
        context.writeIntToOtherPlayer(client.getHealth());
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

    public String getHostAction() {
        return host.getMove().toString() + "!!!";
    }

    public String getClientAction() {
        return client.getMove().toString() + "!!!";
    }

    public String isDone() {
        if (host.getHealth() > 0 && client.getHealth() > 0)
            return null;
        else if (host.getHealth() < 0 && client.getHealth() < 0)
            return "No one";
        else if (host.getHealth() < 0)
            return "Client";
        else if (client.getHealth() < 0)
            return "Host";
        else
            return null;
    }
}
