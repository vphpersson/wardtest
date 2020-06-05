import com.google.gson.Gson;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.entities.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.wire.common.proto.Demo;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.round;

public class ReplayWardAnalysisParser {
    private Float currentGameTime;
    public MatchWardData matchWardData;

    public HashMap<Integer, Integer> observerWardHandleToLifeState = new HashMap<>();
    public HashMap<Integer, HashMap<Integer, WardPlacement>> playerIndexToObserverWardHandleToWardPlacement = new HashMap<>();
    public HashMap<String, HashMap<Integer, Point2D.Float>> heroDtClassNameToRoundedGameTimeToPosition = new HashMap<>();

    private static float getPositionComponent(Entity e, String which) {
        int cell = e.getProperty("CBodyComponent.m_cell" + which);
        float vec = e.getProperty("CBodyComponent.m_vec" + which);
        return cell * 128.0f + vec;
    }

    private static Point2D.Float getPosition(Entity e) {
        return !e.hasProperty("CBodyComponent.m_cellX") ? null : new Point2D.Float(
            getPositionComponent(e, "X"),
            getPositionComponent(e, "Y")
        );
    }

    public static Float getRealGameTimeSeconds(Entity gameRulesProxyEntity) {

        Float TIME_EPS = (float) 0.01;

        Float gameTime = gameRulesProxyEntity.getProperty("m_pGameRules.m_fGameTime");
        if (gameTime == null)
            return null;

        Float preGameTime = gameRulesProxyEntity.getProperty("m_pGameRules.m_flPreGameStartTime");
        if (preGameTime <= TIME_EPS)
            return null;

        Float startTime = gameRulesProxyEntity.getProperty("m_pGameRules.m_flGameStartTime");
        if (startTime > TIME_EPS)
            return gameTime - startTime;

        return gameTime - (float) gameRulesProxyEntity.getProperty("m_pGameRules.m_flStateTransitionTime");
    }

    public static String getClockTime(Float gameTime) {
        return gameTime == null ? null : String.format(
            "%d:%02d",
            (int) Math.floor(gameTime / 60.),
            (int) round(Math.abs(gameTime % 60.))
        );
    }

    public static String getClockTimeFromEntity(Entity gameRulesProxyEntity) {
        return getClockTime(getRealGameTimeSeconds(gameRulesProxyEntity));
    }

    @OnMessage(Demo.CDemoFileInfo.class)
    public void onDemoFileInfoMessage(Context context, Demo.CDemoFileInfo demoFileInfo) {
        Demo.CGameInfo.CDotaGameInfo gameInfo = demoFileInfo.getGameInfo().getDota();
        this.matchWardData = new MatchWardData(gameInfo.getMatchId(), gameInfo.getEndTime(), this.currentGameTime);

        List<Demo.CGameInfo.CDotaGameInfo.CPlayerInfo> playerInfoList = gameInfo.getPlayerInfoList();
        for (int playerIndex = 0; playerIndex < playerInfoList.size(); playerIndex++) {
            Demo.CGameInfo.CDotaGameInfo.CPlayerInfo playerInfo = playerInfoList.get(playerIndex);

            Player player = new Player(
                playerInfo.getPlayerName(),
                playerInfo.getHeroName().replace("npc_dota_hero_", "").replaceAll("_", " ").toLowerCase(),
                playerInfo.getSteamid(),
                Team.valueOf(playerIndex < 5 ? "RADIANT" : "DIRE")
            );

            player.wardPlacements.addAll(
                this.playerIndexToObserverWardHandleToWardPlacement.getOrDefault(playerIndex, new HashMap<>()).values()
            );
            int finalPlayerIndex = playerIndex;
            player.gameTimeToPosition.putAll(this.heroDtClassNameToRoundedGameTimeToPosition
                 .entrySet()
                 .stream()
                 .filter(entry -> ((int) context.getProcessor(Entities.class).getByDtName(entry.getKey()).getProperty("m_iPlayerID")) == finalPlayerIndex)
                 .findFirst()
                 .get()
                 .getValue()
             );

            matchWardData.players.add(player);
        }
    }

    @OnEntityUpdated(classPattern = "CDOTAGamerulesProxy")
    public void onGameRulesProxyUpdate(Context context, Entity e, FieldPath[] fieldPaths, int num) {
        this.currentGameTime = getRealGameTimeSeconds(e);

        if (this.currentGameTime != null) {
            this.heroDtClassNameToRoundedGameTimeToPosition
                .forEach((key, value) -> {
                    Entity currentEntity = context.getProcessor(Entities.class).getByDtName(key);
                    value.put(
                        Math.round(this.currentGameTime),
                        (Integer) currentEntity.getProperty("m_lifeState") == 1 ? null : getPosition(currentEntity)
                    );
                })
            ;
        }
    }

    @OnEntityCreated(classPattern = "CDOTA_Unit_Hero_.*")
    public void onHeroCreated(Entity entity) {
        this.heroDtClassNameToRoundedGameTimeToPosition.putIfAbsent(
            entity.getDtClass().getDtName(),
            new HashMap<>()
        );
    }

    @OnEntityCreated(classPattern = "CDOTA_NPC_Observer_Ward")
    public void onObserverWardCreated(Entity entity) {
        Integer playerIndex = entity.getProperty("m_nPlayerOwnerID");

        HashMap<Integer, WardPlacement> observerWardHandleToWardPlacement = this.playerIndexToObserverWardHandleToWardPlacement.getOrDefault(
            playerIndex,
            new HashMap<>()
        );

        observerWardHandleToWardPlacement.put(
            entity.getHandle(),
            new WardPlacement(getPosition(entity), this.currentGameTime)
        );

        this.playerIndexToObserverWardHandleToWardPlacement.put(playerIndex, observerWardHandleToWardPlacement);
    }

    @OnEntityPropertyChanged(classPattern = "CDOTA_NPC_Observer_Ward")
    public void onEntityPropertyChanged(Context context, Entity entity, FieldPath fieldPath) {
        Integer playerIndex = entity.getProperty("m_nPlayerOwnerID");

        Integer lastLifeState = this.observerWardHandleToLifeState.getOrDefault(entity.getHandle(), 0);
        Integer newLifeState = entity.getProperty("m_lifeState");
        if (lastLifeState == 0 && newLifeState == 1)
            this.playerIndexToObserverWardHandleToWardPlacement.get(playerIndex).get(entity.getHandle()).timeRemoved = this.currentGameTime;

        this.observerWardHandleToLifeState.put(entity.getHandle(), newLifeState);
    }

    public static void main(String[] args) throws Exception {
        InputStreamSource source = new InputStreamSource(System.in);

        ReplayWardAnalysisParser parser = new ReplayWardAnalysisParser();
        new SimpleRunner(source).runWith(parser);

        System.out.println(new Gson().toJson(parser.matchWardData));
    }
}
