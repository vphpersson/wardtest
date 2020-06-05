import java.awt.geom.Point2D;

public class HeroPositionData {
    public Point2D.Float point;
    public Float gameTime;

    public HeroPositionData(Point2D.Float point, Float gameTime) {
        this.point = point;
        this.gameTime = gameTime;
    }
}
