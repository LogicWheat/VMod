package net.spaceeye.vmod.vsStuff

import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.ChunkPos
import net.spaceeye.vmod.VMConfig
import net.spaceeye.vmod.events.AVSEvents
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.mixin.ChunkStorageAccessor
import net.spaceeye.vmod.mixin.IOWorkerAccessor
import net.spaceeye.vmod.mixin.RegionFileStorageAccessor
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.game.ships.ShipData
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.mod.common.getLevelFromDimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.isChunkInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists

object VSShipyardPruner {
    val toClear = ConcurrentLinkedQueue<ServerShip>()

    init {
        AVSEvents.serverShipRemoveEvent.on { (ship), _ ->
            if (!VMConfig.SERVER.SHIPYARD_PRUNER.CLEAR_SHIP_PLOT_ON_DELETION) {return@on}
            toClear.add(ship)
        }
        PersistentEvents.serverAfterTick.on { (server), _ ->
            if (!VMConfig.SERVER.SHIPYARD_PRUNER.CLEAR_SHIP_PLOT_ON_DELETION) {return@on}
            if (toClear.isEmpty()) {return@on}
            val temp = mutableListOf<ServerShip>()
            val shipWorld = server.shipObjectWorld
            toClear.pollUntilEmpty { ship ->
                if (shipWorld.allShips.contains(ship.id)) {
                    temp.add(ship)
                    return@pollUntilEmpty
                }
                clearId(server, ship)
            }
            toClear.addAll(temp)
        }
    }

    private fun clearId(server: MinecraftServer, ship: ServerShip) {
        val level = server?.getLevelFromDimensionId(ship.chunkClaimDimension ?: return) ?: return
        val path = (((level.chunkSource.chunkMap as? ChunkStorageAccessor)?.`vmod$getWorker`() as? IOWorkerAccessor)?.`vmod$getStorage`() as? RegionFileStorageAccessor)?.`vmod$getPath`() ?: return

        if (path.notExists()) return

        val claim = ship.chunkClaim

        val start = ChunkPos(claim.xStart, claim.zStart)
        val end = ChunkPos(claim.xEnd, claim.zEnd)

        val regions =   (start.regionX .. end.regionX)
            .map { x -> (start.regionZ .. end.regionZ)
                .map { z -> Pair(x, z) } }
                .flatten()
                .filter { // just in case
                (   ChunkPos.minFromRegion(it.first, it.second).let { level.isChunkInShipyard(it.x, it.z) }
                 || ChunkPos.maxFromRegion(it.first, it.second).let { level.isChunkInShipyard(it.x, it.z) }
                ) && ChunkPos.minFromRegion(it.first, it.second).let { level.getShipManagingPos(it) == null }
                  && ChunkPos.maxFromRegion(it.first, it.second).let { level.getShipManagingPos(it) == null }
                }

        for ((x, z) in regions) {
            path.resolve("r.${x}.${z}.mca").deleteIfExists()
        }
    }
}