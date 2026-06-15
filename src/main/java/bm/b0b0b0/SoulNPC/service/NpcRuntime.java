package bm.b0b0b0.SoulNPC.service;

import bm.b0b0b0.SoulNPC.model.NpcEntityPose;
import bm.b0b0b0.SoulNPC.model.NpcFileData;
import bm.b0b0b0.SoulNPC.packet.PacketPlayerAppearance;
import com.github.retrooper.packetevents.protocol.npc.NPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NpcRuntime {

    private final NpcFileData data;
    private NPC packetNpc;
    private bm.b0b0b0.SoulNPC.packet.PacketMobNpc packetMob;
    private bm.b0b0b0.SoulNPC.packet.PacketPlayerSeat playerSeat;
    private final List<UUID> hologramDisplayIds = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();
    private boolean profileReady;
    private int animationTicks;
    private int customFrameIndex;
    private boolean waveMainHand = true;
    private boolean bowLowered;
    private boolean animationPhase;
    private final Map<UUID, Long> greetCooldowns = new HashMap<>();
    private float lookYaw;
    private float lookPitch;
    private boolean lookInitialized;
    private GreetPhase greetPhase = GreetPhase.IDLE;
    private int greetPhaseTick;
    private UUID greetTargetId;
    private int mobPoseIndex;
    private boolean greetCrouchActive;
    private int groundItemTicks;
    private int groundItemIndex;
    private boolean groundItemSpawnNow = true;

    public enum GreetPhase {
        IDLE,
        TURN,
        NOD,
        CROUCH
    }

    public NpcRuntime(NpcFileData data) {
        this.data = data;
    }

    public NpcFileData data() {
        return data;
    }

    public NPC packetNpc() {
        return packetNpc;
    }

    public void setPacketNpc(NPC packetNpc) {
        this.packetNpc = packetNpc;
        this.packetMob = null;
        this.playerSeat = packetNpc != null && PacketPlayerAppearance.needsSeat(data.appearance)
                ? new bm.b0b0b0.SoulNPC.packet.PacketPlayerSeat(data)
                : null;
        this.profileReady = packetNpc != null;
    }

    public bm.b0b0b0.SoulNPC.packet.PacketPlayerSeat playerSeat() {
        return playerSeat;
    }

    public void refreshPlayerSeat() {
        if (packetNpc == null) {
            playerSeat = null;
            return;
        }
        if (PacketPlayerAppearance.needsSeat(data.appearance)) {
            if (playerSeat == null) {
                playerSeat = new bm.b0b0b0.SoulNPC.packet.PacketPlayerSeat(data);
            }
        } else {
            if (playerSeat != null) {
                playerSeat.despawnAll();
                playerSeat = null;
            }
        }
    }

    public bm.b0b0b0.SoulNPC.packet.PacketMobNpc packetMob() {
        return packetMob;
    }

    public void setPacketMob(bm.b0b0b0.SoulNPC.packet.PacketMobNpc packetMob) {
        this.packetMob = packetMob;
        this.packetNpc = null;
        this.playerSeat = null;
        this.profileReady = packetMob != null;
    }

    public boolean isPlayerNpc() {
        return packetNpc != null;
    }

    public List<UUID> hologramDisplayIds() {
        return hologramDisplayIds;
    }

    public void addHologramDisplay(UUID displayId) {
        hologramDisplayIds.add(displayId);
    }

    public void clearHologramDisplays() {
        hologramDisplayIds.clear();
    }

    public boolean isProfileReady() {
        return profileReady;
    }

    public boolean isSpawned() {
        return profileReady;
    }

    public Set<UUID> viewers() {
        return viewers;
    }

    public boolean isVisibleTo(UUID playerId) {
        return viewers.contains(playerId);
    }

    public void addViewer(UUID playerId) {
        viewers.add(playerId);
    }

    public void removeViewer(UUID playerId) {
        viewers.remove(playerId);
    }

    public void clearViewers() {
        viewers.clear();
    }

    public int animationTicks() {
        return animationTicks;
    }

    public int incrementAnimationTicks() {
        return ++animationTicks;
    }

    public int customFrameIndex() {
        return customFrameIndex;
    }

    public void setCustomFrameIndex(int customFrameIndex) {
        this.customFrameIndex = customFrameIndex;
    }

    public boolean toggleWaveHand() {
        waveMainHand = !waveMainHand;
        return waveMainHand;
    }

    public boolean toggleBowPhase() {
        bowLowered = !bowLowered;
        return bowLowered;
    }

    public boolean toggleAnimationPhase() {
        animationPhase = !animationPhase;
        return animationPhase;
    }

    public void resetAnimationState() {
        animationTicks = 0;
        customFrameIndex = 0;
        mobPoseIndex = 0;
        waveMainHand = true;
        bowLowered = false;
        animationPhase = false;
        if (packetNpc != null && data.appearance.type.isPlayerModel()) {
            PacketPlayerAppearance.applyToViewers(packetNpc, data.appearance);
        }
        greetCrouchActive = false;
        endGreet();
        resetLookRotation();
    }

    public void resetLookRotation() {
        lookYaw = data.yaw;
        if (data.appearance.isPacketMob() && !data.lookAtPlayers) {
            lookPitch = 0.0F;
        } else {
            lookPitch = data.pitch;
        }
        lookInitialized = true;
    }

    public float[] smoothLookToward(float targetYaw, float targetPitch, float factor) {
        initLookIfNeeded();
        lookYaw = bm.b0b0b0.SoulNPC.packet.NpcLookAtUtil.lerpAngle(lookYaw, targetYaw, factor);
        lookPitch = bm.b0b0b0.SoulNPC.packet.NpcLookAtUtil.lerp(lookPitch, targetPitch, factor);
        lookPitch = Math.max(-90.0F, Math.min(90.0F, lookPitch));
        return new float[]{lookYaw, lookPitch};
    }

    private void initLookIfNeeded() {
        if (!lookInitialized) {
            lookYaw = data.yaw;
            lookPitch = data.pitch;
            lookInitialized = true;
        }
    }

    public boolean isGreeting() {
        return greetPhase != GreetPhase.IDLE;
    }

    public GreetPhase greetPhase() {
        return greetPhase;
    }

    public int greetPhaseTick() {
        return greetPhaseTick;
    }

    public UUID greetTargetId() {
        return greetTargetId;
    }

    public void beginGreet(UUID targetId) {
        greetPhase = GreetPhase.TURN;
        greetPhaseTick = 0;
        greetTargetId = targetId;
    }

    public void endGreet() {
        greetPhase = GreetPhase.IDLE;
        greetPhaseTick = 0;
        greetTargetId = null;
    }

    public boolean greetCrouchActive() {
        return greetCrouchActive;
    }

    public void setGreetCrouchActive(boolean greetCrouchActive) {
        this.greetCrouchActive = greetCrouchActive;
    }

    public boolean canGreetCrouch() {
        return isPlayerNpc()
                && data.appearance.type.isPlayerModel()
                && data.appearance.entityPose != NpcEntityPose.SITTING;
    }

    public int incrementGreetPhaseTick() {
        return ++greetPhaseTick;
    }

    public void setGreetPhase(GreetPhase phase) {
        greetPhase = phase;
        greetPhaseTick = 0;
    }

    public boolean canGreetPlayer(UUID playerId, long cooldownMillis) {
        Long lastGreet = greetCooldowns.get(playerId);
        if (lastGreet == null) {
            return true;
        }
        return System.currentTimeMillis() - lastGreet >= cooldownMillis;
    }

    public void markGreeted(UUID playerId) {
        greetCooldowns.put(playerId, System.currentTimeMillis());
    }

    public int mobPoseIndex() {
        return mobPoseIndex;
    }

    public void setMobPoseIndex(int mobPoseIndex) {
        this.mobPoseIndex = Math.max(0, mobPoseIndex);
    }

    public void resetGroundItemWaveState() {
        groundItemTicks = 0;
        groundItemSpawnNow = true;
    }

    public boolean consumeGroundItemWaveDue(int intervalTicks) {
        int interval = Math.max(1, intervalTicks);
        if (groundItemSpawnNow) {
            groundItemSpawnNow = false;
            groundItemTicks = 0;
            return true;
        }
        groundItemTicks++;
        if (groundItemTicks < interval) {
            return false;
        }
        groundItemTicks = 0;
        return true;
    }

    public int nextGroundItemIndex(int size) {
        if (size <= 0) {
            return 0;
        }
        int index = groundItemIndex % size;
        groundItemIndex++;
        return index;
    }
}
