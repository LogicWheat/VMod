package net.spaceeye.vmod.vsStuff

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import net.spaceeye.vmod.utils.PosMap
import net.spaceeye.vmod.utils.ServerClosable
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.core.apigame.world.properties.DimensionId
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

object CustomBlockMassManager: ServerClosable() {
    val dimToPosToMass = mutableMapOf<DimensionId, PosMap<Double>>()
    val shipToPosToMass = mutableMapOf<ShipId, PosMap<Double>>()

    override fun close() {
        dimToPosToMass.clear()
        shipToPosToMass.clear()
    }

    fun removeCustomMass(dimensionId: DimensionId, x: Int, y: Int, z: Int) {
        dimToPosToMass.getOrPut(dimensionId) { PosMap() }.removeItemFromPos(x, y, z)
    }

    fun getCustomMass(dimension: DimensionId, x: Int, y: Int, z: Int): Double? {
        return dimToPosToMass.getOrPut(dimension) { PosMap() }.getItemAt(x, y, z)
    }

    fun zeroMass(level: ServerLevel, ship: ServerShip): Boolean {
        val (_, air) = BlockStateInfo.get(Blocks.AIR.defaultBlockState())!!
        val (_, dummy) = BlockStateInfo.get(Blocks.GOLD_BLOCK.defaultBlockState())!!

        val aabb = ship.shipAABB ?: return false

        //VS will zero mass if ship has no colliders and you do onSetBlock like that, actual block types don't seem to matter
        for (x in aabb.minX() until aabb.maxX()) {
        for (y in aabb.minY() until aabb.maxY()) {
        for (z in aabb.minZ() until aabb.maxZ()) {
            level.shipObjectWorld.onSetBlock(x, y, z, ship.chunkClaimDimension, dummy, air, 0.0, 0.0)
        } } }

        level.shipObjectWorld.onSetBlock(aabb.minX(), aabb.minY(), aabb.minZ(), ship.chunkClaimDimension, air, air, ship.inertiaData.mass, 0.0)

        shipToPosToMass[ship.id]?.also {
            val dimMap = dimToPosToMass[level.dimensionId]

            it.asList().forEach { (pos, _) ->
                dimMap?.removeItemFromPos(pos.x, pos.y, pos.z)
            }
            it.clear()
        }

        return true
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, mass: Double): Boolean {
        val ship = level.getShipManagingPos(x shr 4, z shr 4) ?: return false
        val block = level.getBlockState(BlockPos(x, y, z))
        val (defaultMass, type) = BlockStateInfo.get(block) ?: return false

        return setCustomMass(level, x, y, z, defaultMass, mass, type, type, ship)
    }

    fun setCustomMass(level: ServerLevel, x: Int, y: Int, z: Int, oldMass: Double, mass: Double, oldType: BlockType, type: BlockType, ship: ServerShip): Boolean {
        val oldMass = getCustomMass(level.dimensionId, x, y, z) ?: oldMass

        val dimensionId = level.dimensionId
        val shipObjectWorld = level.shipObjectWorld

        shipObjectWorld.onSetBlock(x, y, z, dimensionId, oldType, type, oldMass, mass)

        dimToPosToMass.getOrPut(level.dimensionId) { PosMap() }.setItemTo(mass, x, y, z)
        shipToPosToMass.getOrPut(ship.id) { PosMap() }.setItemTo(mass, x, y, z)
        return true
    }

    fun loadCustomMass(dimensionId: DimensionId, shipId: ShipId, x: Int, y: Int, z: Int, mass: Double) {
        dimToPosToMass.getOrPut(dimensionId) { PosMap() }.setItemTo(mass, x, y, z)
        shipToPosToMass.getOrPut(shipId) { PosMap() }.setItemTo(mass, x, y, z)
    }
}