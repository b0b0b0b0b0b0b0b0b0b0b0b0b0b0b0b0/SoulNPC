package bm.b0b0b0.SoulNPC.config.settings;

import bm.b0b0b0.SoulNPC.config.serializer.MaterialYamlSerializer;
import net.elytrium.serializer.SerializerConfig;

public final class SoulNpcSerializerConfig {

    public static final SerializerConfig INSTANCE = new SerializerConfig.Builder()
            .registerSerializer(new MaterialYamlSerializer())
            .build();

    private SoulNpcSerializerConfig() {
    }
}
