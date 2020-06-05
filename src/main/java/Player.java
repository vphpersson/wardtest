import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;

public class Player {
    private String playerName;
    private String heroIdentifier;
    private long steamId;
    private Team team;
    public ArrayList<WardPlacement> wardPlacements = new ArrayList<>();
    public HashMap<Integer, Point2D.Float> gameTimeToPosition = new HashMap<>();

    public Player(String playerName, String heroIdentifier, long steamId, Team team) {
        this.playerName = playerName;
        this.heroIdentifier = heroIdentifier;
        this.steamId = steamId;
        this.team = team;
    }
}
