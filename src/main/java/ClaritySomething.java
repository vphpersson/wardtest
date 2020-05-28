import com.google.protobuf.Descriptors;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.entities.OnEntityPropertyChanged;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.s2.proto.S2UserMessages;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.processor.gameevents.OnGameEvent;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ClaritySomething {


    @OnEntityPropertyChanged(classPattern = "CDOTA_NPC_Observer_Ward")
    public void onEntityPropertyChanged(Context ctx, Entity e, FieldPath fp) {
        int a = 3;
    }

    public static void main(String[] args) throws Exception {
        // 1) create an input source from the replay
        Source source = new MappedFileSource("replay.dem");
        // 2) create a simple runner that will read the replay once
        SimpleRunner runner = new SimpleRunner(source);

        runner.runWith(new ClaritySomething());
    }
}
