import skadistats.clarity.model.Vector;

import java.awt.geom.Point2D;

public class WardPlacement {
    public Point2D.Float point;
    public Float timePlaced;
    public Float timeRemoved;

    public WardPlacement(Point2D.Float point, Float timePlaced) {
        this.point = point;
        this.timePlaced = timePlaced;
    }
}
