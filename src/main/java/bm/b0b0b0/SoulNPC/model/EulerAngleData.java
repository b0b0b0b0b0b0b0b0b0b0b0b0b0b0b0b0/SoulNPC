package bm.b0b0b0.SoulNPC.model;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class EulerAngleData {

    @Comment(value = {
            @CommentValue(" X — наклон вперёд/назад (pitch)")
    })
    public float x;

    @Comment(value = {
            @CommentValue(" Y — поворот влево/вправо (yaw)")
    })
    public float y;

    @Comment(value = {
            @CommentValue(" Z — крен (roll)")
    })
    public float z;

    public EulerAngleData() {
    }

    public EulerAngleData(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public EulerAngleData copy() {
        return new EulerAngleData(x, y, z);
    }
}
