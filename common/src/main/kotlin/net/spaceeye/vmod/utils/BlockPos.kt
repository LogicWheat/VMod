package net.spaceeye.vmod.utils

import net.minecraft.world.phys.Vec3

inline fun BlockPos(x: Number, y: Number, z: Number) = net.minecraft.core.BlockPos(x.toInt(), y.toInt(), z.toInt())
inline fun BlockPos(vec3: Vec3) = net.minecraft.core.BlockPos(vec3.x.toInt(), vec3.y.toInt(), vec3.z.toInt())