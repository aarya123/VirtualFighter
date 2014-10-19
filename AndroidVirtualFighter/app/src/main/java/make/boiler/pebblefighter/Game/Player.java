package make.boiler.pebblefighter.Game;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import make.boiler.pebblefighter.PlayActivity;

/**
 * Created by AnubhawArya on 10/18/14.
 */
public class Player {
    boolean host;
    int health = 100;
    Move move = Move.BLOCK;

    public Player(boolean host) {
        this.host = host;
    }

    public Move getCommand() {
        return move;
    }

    public void setCommand(Move move) {
        this.move = move;
    }

    public void doDamage(Move theirMove, PlayActivity context) {
        if (theirMove == move) {
            if (theirMove == Move.PUNCH) {
                health -= 5;
            } else if (theirMove == Move.BLOCK) {
                health -= 1;
            }
        } else if (theirMove == Move.BLOCK) {
            health -= 1;
        } else {
            health -= 2;
        }
    }

    public void setHealth(int i) {
        health = i;
    }

    public int getHealth() {
        return health;
    }

    public Move getMove() {
        return move;
    }
}
