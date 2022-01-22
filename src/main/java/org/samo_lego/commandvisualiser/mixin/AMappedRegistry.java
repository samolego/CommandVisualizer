package org.samo_lego.commandvisualiser.mixin;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MappedRegistry.class)
public interface AMappedRegistry {
    @Accessor("byId")
    ObjectList<?> getById();
}
