package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Comment(value = {
        @CommentValue(" Декоративные предметы у ног NPC (лежат на земле и исчезают)"),
        @CommentValue(" Список items — по очереди; настройка в npc.yml")
})
public final class NpcGroundItemEffectData {

    @Comment(value = {
            @CommentValue(" Включить эффект")
    })
    public boolean enabled = false;

    @Comment(value = {
            @CommentValue(" Точка спавна от ног NPC вверх (блоки)")
    })
    public float spawnYOffset = 0.05F;

    @Comment(value = {
            @CommentValue(" Пауза между волнами (тики, 20 = 1 сек). Дефолт: 200 = 10 сек")
    })
    public int intervalTicks = 200;

    @Comment(value = {
            @CommentValue(" Сколько предметов вылетает за одну волну (фрррр)")
    })
    public int countPerWave = 12;

    @Comment(value = {
            @CommentValue(" Радиус разлёта от ног (масштаб):"),
            @CommentValue(" 0.1 ≈ 1–2 блока, 0.3 ≈ 4 блока, 0.5+ — широко")
    })
    public double spread = 0.1D;

    @Comment(value = {
            @CommentValue(" Подброс вверх (чем выше — тем дальше улетит)")
    })
    public double throwUp = 0.22D;

    @Comment(value = {
            @CommentValue(" Исчезновение после выстрела (тики). Дефолт: 280 = 14 сек")
    })
    public int lifetimeTicks = 280;

    @NewLine
    @Comment(value = {
            @CommentValue(" Очередь слотов (materials + amount)")
    })
    public List<NpcGroundItemEntry> items = defaultItems();

    public static List<NpcGroundItemEntry> defaultItems() {
        List<NpcGroundItemEntry> list = new ArrayList<>(1);
        list.add(defaultEntry());
        return list;
    }

    private static NpcGroundItemEntry defaultEntry() {
        NpcGroundItemEntry entry = new NpcGroundItemEntry();
        entry.materials = NpcGroundItemEntry.defaultMaterials();
        entry.amount = 1;
        return entry;
    }

    public void ensureItems() {
        if (items == null || items.isEmpty()) {
            items = defaultItems();
        }
        for (NpcGroundItemEntry entry : items) {
            entry.ensureMaterials();
        }
    }
}
