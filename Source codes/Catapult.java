import java.io.Serializable;

public class Catapult extends Piece implements Board, Serializable {


    public Catapult(int mainPower, int power, int move, int x, int y, int boost, String type) {
        super(mainPower, power, move, x, y, boost, type);
    }
}
