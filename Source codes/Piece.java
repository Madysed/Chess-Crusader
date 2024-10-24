import java.io.Serializable;

public class Piece implements Serializable {

    private int mainPower, power, move, x, y, boost = 0;
    String type;

    public Piece(int mainPower, int power, int move, int x, int y, int boost, String type) {
        this.mainPower = mainPower;
        this.power = power;
        this.move = move;
        this.x = x;
        this.y = y;
        this.boost = boost;
        this.type = type;
    }

    public int getMainPower() {
        return mainPower;
    }

    public void setMainPower(int mainPower) {
        this.mainPower = mainPower;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getMove() {
        return move;
    }

    public void setMove(int move) {
        this.move = move;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getBoost() {
        return boost;
    }

    public void setBoost(int boost) {
        this.boost = boost;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
