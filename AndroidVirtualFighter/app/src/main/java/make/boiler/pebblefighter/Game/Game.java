package make.boiler.pebblefighter.Game;

/**
 * Created by AnubhawArya on 10/18/14.
 */
public class Game {
    Player me, them;
    public Game(){
        me=new Player(true);
        them=new Player(false);
    }
    public void setHostCommand(Move move) {
        me.setCommand(move);
    }
    public void setTheirCommand(Move move){
        them.setCommand(move);
    }
}
