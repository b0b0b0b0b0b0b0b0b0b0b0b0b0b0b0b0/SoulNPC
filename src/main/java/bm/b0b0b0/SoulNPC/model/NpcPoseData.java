package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;

@Comment(value = {
        @CommentValue(" Углы частей тела player-модели (градусы)"),
        @CommentValue(" Работает при animation.type: CUSTOM; иначе см. appearance.entity-pose")
})
public final class NpcPoseData {

    @Comment(value = {
            @CommentValue(" Голова")
    })
    public EulerAngleData head = new EulerAngleData();

    @Comment(value = {
            @CommentValue(" Тело")
    })
    public EulerAngleData body = new EulerAngleData();

    @NewLine
    @Comment(value = {
            @CommentValue(" Левая рука (дефолт слегка отведена)")
    })
    public EulerAngleData leftArm = new EulerAngleData(-10F, 0F, -10F);

    @Comment(value = {
            @CommentValue(" Правая рука")
    })
    public EulerAngleData rightArm = new EulerAngleData(-15F, 0F, 10F);

    @NewLine
    @Comment(value = {
            @CommentValue(" Левая нога")
    })
    public EulerAngleData leftLeg = new EulerAngleData();

    @Comment(value = {
            @CommentValue(" Правая нога")
    })
    public EulerAngleData rightLeg = new EulerAngleData();

    public static NpcPoseData defaultPlayerPose() {
        return new NpcPoseData();
    }

    public NpcPoseData copy() {
        NpcPoseData copy = new NpcPoseData();
        copy.head = head.copy();
        copy.body = body.copy();
        copy.leftArm = leftArm.copy();
        copy.rightArm = rightArm.copy();
        copy.leftLeg = leftLeg.copy();
        copy.rightLeg = rightLeg.copy();
        return copy;
    }
}
