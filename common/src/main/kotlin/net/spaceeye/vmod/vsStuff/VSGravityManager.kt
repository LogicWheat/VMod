package net.spaceeye.vmod.vsStuff

import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.shipAttachments.GravityController
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.ServerObjectsHolder
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.mod.common.config.DimensionParametersResolver
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore

object VSGravityManager {
    val gravities = DimensionParametersResolver.dimensionMap
    init {
        vsCore.shipLoadEvent.on { event -> var ship = event.ship
            ship = ServerObjectsHolder.server!!.shipObjectWorld.loadedShips.getById(ship.id) ?: return@on
            GravityController.getOrCreate(ship)
        }
    }

    fun setGravity(level: ServerLevel, gravity: Vector3d) {
        //TODO
//        gravities.getOrPut(level.dimensionId) {Vector3d(gravity)}.set(gravity.x, gravity.y, gravity.z)
//        saveState()
    }

    fun setGravity(id: DimensionId, gravity: Vector3d) {
        //TODO
//        gravities.getOrPut(id) {Vector3d(gravity)}.set(gravity.x, gravity.y, gravity.z)
//        saveState()
    }

    fun getDimensionGravityMutableReference(id: DimensionId): Vector3dc? {
        return gravities[id]?.gravity
    }
}