package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

import java.util.ArrayList;
import java.util.List;

public final class NpcAnimationData {

    @Comment(value = {
            @CommentValue(" Включить анимацию")
    })
    public boolean enabled = true;

    @Comment(value = {
            @CommentValue(" Тип: NONE, SWING_ARM, SWING_OFF_HAND, WAVE, GREET, BOW, IDLE_SWAY, CUSTOM, MOB_POSE"),
            @CommentValue(" Пакеты: HURT, CRITICAL_HIT, MAGIC_CRITICAL_HIT, WAKE_UP"),
            @CommentValue(" Позы: SPIN_ATTACK (riptide/трезубец), FALL_FLYING, CROUCH, SLEEP, SWIM"),
            @CommentValue(" Руки: USE_MAIN_HAND (зарядка), USE_OFF_HAND; алиасы: TRIDENT, RIPTIDE, TRIDENT_CHARGE")
    })
    public NpcAnimationType type = NpcAnimationType.GREET;

    @Comment(value = {
            @CommentValue(" Интервал между кадрами анимации (тики)")
    })
    public int intervalTicks = 40;

    @Comment(value = {
            @CommentValue(" MOB_POSE: SEQUENTIAL (по порядку) или RANDOM"),
            @CommentValue(" MOB_POSE: только мобы с профилем (fox, wolf); иначе NONE")
    })
    public NpcMobPoseMode mobPoseMode = NpcMobPoseMode.SEQUENTIAL;

    public List<String> mobPoses = defaultMobPoses();

    @Comment(value = {
            @CommentValue(" GREET: дистанция приветствия (блоки)")
    })
    public int greetRange = 8;

    @Comment(value = {
            @CommentValue(" GREET: пауза перед повторным приветствием того же игрока (сек)")
    })
    public int greetCooldownSeconds = 25;

    @Comment(value = {
            @CommentValue(" GREET: плавный поворот к игроку (тики)")
    })
    public int greetTurnTicks = 16;

    @Comment(value = {
            @CommentValue(" GREET: плавный кивок (тики)")
    })
    public int greetNodTicks = 22;

    @Comment(value = {
            @CommentValue(" GREET: амплитуда кивка (градусы); при greet-style: NOD")
    })
    public float greetNodDegrees = 9.0F;

    @Comment(value = {
            @CommentValue(" GREET: NOD — кивок; CROUCH — присесть (player NPC)")
    })
    public NpcGreetStyle greetStyle = NpcGreetStyle.CROUCH;

    @Comment(value = {
            @CommentValue(" GREET: длительность приседания (тики); greet-style: CROUCH")
    })
    public int greetCrouchTicks = 18;

    @Deprecated
    public int greetSwings = 0;

    @Deprecated
    public int greetSwingIntervalTicks = 0;

    @Comment(value = {
            @CommentValue(" Кадры для CUSTOM (пусто = pose из конфига)")
    })
    public List<NpcPoseData> frames = new ArrayList<>();

    private static List<String> defaultMobPoses() {
        List<String> poses = new ArrayList<>(1);
        poses.add("STANDING");
        return poses;
    }
}
