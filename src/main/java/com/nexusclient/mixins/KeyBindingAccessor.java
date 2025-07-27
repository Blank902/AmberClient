package com.nexusclient.mixins;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Accessor("pressed")
    boolean isPressed();

    @Accessor("pressed")
    void setPressed(boolean pressed);

    @Accessor("timesPressed")
    int getTimesPressed();

    @Accessor("timesPressed")
    void setTimesPressed(int timesPressed);
}