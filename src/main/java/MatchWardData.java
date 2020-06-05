import java.util.ArrayList;

public class MatchWardData {

    public long matchId;
    public int timestamp;
    public float numElapsedSeconds;
    public ArrayList<Player> players = new ArrayList<>();

    public MatchWardData(long matchId, int timestamp, float numElapsedSeconds) {
        this.matchId = matchId;
        this.timestamp = timestamp;
        this.numElapsedSeconds = numElapsedSeconds;
    }
}
