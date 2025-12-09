package net.spaceeye.vmod.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkStorage.class)
public interface ChunkStorageAccessor {
    @Invoker("write") void vmod$write(ChunkPos arg, CompoundTag arg2);
    @Accessor("worker") @NotNull IOWorker vmod$getWorker();
}
