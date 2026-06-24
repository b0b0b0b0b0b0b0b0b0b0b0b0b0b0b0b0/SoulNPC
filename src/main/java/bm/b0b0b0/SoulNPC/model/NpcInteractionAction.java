package bm.b0b0b0.SoulNPC.model;

public final class NpcInteractionAction {

    public NpcClickBinding click = NpcClickBinding.RIGHT;
    public NpcActionType type = NpcActionType.MESSAGE;
    public String value = "";
    public int delayTicks;
    public int cooldownSeconds;

    public boolean isActionable() {
        return value != null && !value.isBlank();
    }

    public NpcInteractionAction copy() {
        NpcInteractionAction copy = new NpcInteractionAction();
        copy.click = click;
        copy.type = type;
        copy.value = value;
        copy.delayTicks = delayTicks;
        copy.cooldownSeconds = cooldownSeconds;
        return copy;
    }
}
