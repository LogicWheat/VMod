package net.spaceeye.vmod.mixin;

import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.file.Path;

@Mixin(RegionFileStorage.class)
public interface RegionFileStorageAccessor {
    @Accessor("folder") @NotNull Path vmod$getPath();
}
