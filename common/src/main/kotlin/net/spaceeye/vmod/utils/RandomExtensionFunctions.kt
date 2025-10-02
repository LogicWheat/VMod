package net.spaceeye.vmod.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf

inline fun Quaternionf.toJoml() = Quaterniond(this.x.toDouble(), this.y.toDouble(), this.z.toDouble(), this.w.toDouble())

inline fun BlockPos(x: Number, y: Number, z: Number) = BlockPos(x.toInt(), y.toInt(), z.toInt())
inline fun BlockPos(vec3: Vec3) = BlockPos(vec3.x.toInt(), vec3.y.toInt(), vec3.z.toInt())

inline fun BlockPos.toChunkPos() = ChunkPos(this)
inline fun ChunkPos.sub(by: ChunkPos) = ChunkPos(this.x - by.x, this.z - by.z)
inline fun ChunkPos.add(by: ChunkPos) = ChunkPos(this.x + by.x, this.z + by.z)