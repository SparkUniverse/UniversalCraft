package gg.essential.universal;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
//#if MC>10809
//$$ import net.minecraft.util.SoundEvent;
//#endif

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

//#if MC >= 26.4
//$$ import net.minecraft.core.Holder;
//#endif

class PositionedSoundRecordFactory {
    //#if MC>10809
    //$$ public static PositionedSoundRecord makeRecord(SoundEvent event, float volume, float pitch) {
        //#if MC >= 26.4
        //$$ return SimpleSoundInstance.forUI(Holder.direct(event), pitch, volume);
        //#else
        //$$ return PositionedSoundRecord.func_194007_a(event, pitch, volume);
        //#endif
    //$$ }
    //#else
    private static final MethodHandle constructor;
    static {
        MethodHandle locatedConstructor;
        try {
            Constructor<PositionedSoundRecord> ctor = PositionedSoundRecord.class.getDeclaredConstructor(ResourceLocation.class, float.class, float.class, boolean.class, int.class, ISound.AttenuationType.class, float.class, float.class, float.class);
            ctor.setAccessible(true);
            locatedConstructor = MethodHandles.lookup().unreflectConstructor(ctor);
        } catch (Throwable e) {
            e.printStackTrace();
            locatedConstructor = null;
        }
        constructor = locatedConstructor;
    }
    public static PositionedSoundRecord makeRecord(ResourceLocation event, float volume, float pitch) {
        if (constructor == null) return null;
        try {
            return (PositionedSoundRecord) constructor.invokeExact(event, volume, pitch, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }
    //#endif
}
