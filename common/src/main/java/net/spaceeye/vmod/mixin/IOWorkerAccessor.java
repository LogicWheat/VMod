package net.spaceeye.vmod.mixin;

import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IOWorker.class)
public interface IOWorkerAccessor {
    @Accessor("storage") @NotNull RegionFileStorage vmod$getStorage();
}
