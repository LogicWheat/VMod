package net.spaceeye.vmod.rendering

import dev.architectury.networking.NetworkManager
import net.minecraft.world.level.ChunkPos
import net.spaceeye.vmod.events.PersistentEvents
import net.spaceeye.vmod.networking.SynchronisedDataTransmitter
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.PositionDependentRenderer
import net.spaceeye.vmod.utils.addCustomServerClosable
import net.spaceeye.vmod.utils.toChunkPos
import org.valkyrienskies.mod.common.dimensionId
import java.util.UUID

class ServerWorldSynchronisedRenderingData: SynchronisedDataTransmitter<BaseRenderer>(
    "world_rendering_data",
    NetworkManager.Side.C2S,
    NetworkManager.Side.S2C,
    1000000,
    ::serializeItem,
    ::deserializeItem
) {
    private var nextPage: Long = 0
    private var idToPage = mutableMapOf<Int, Long>()
    private val dimToCPosToPage = mutableMapOf<String, MutableMap<ChunkPos, Long>>()

    fun <T> addRenderer(dimensionId: String, render: T): Int where T: BaseRenderer, T: PositionDependentRenderer = lock {
        val cpos = render.renderingPosition.toChunkPos()
        val page = dimToCPosToPage.getOrPut(dimensionId) { mutableMapOf() }.getOrPut(cpos) { nextPage++ }
        val id = add(page, render)
        idToPage[id] = page
        return id
    }

    fun <T> setRenderer(id: Int, renderer: T): Long? where T: BaseRenderer, T: PositionDependentRenderer = lock {
        val page = idToPage[id] ?: return null
        set(page, id, renderer)
        return page
    }

    fun removeRenderer(id: Int): Long? = lock {
        val page = idToPage[id] ?: return null
        idToPage.remove(id)
        return if(remove(page, id)) page else null
    }

    fun getRenderer(id: Int): BaseRenderer? = lock {
        val page = idToPage[id] ?: return null
        return get(page)?.get(id)
    }

    private data class PlayerState(var dimension: String, var lastCPos: ChunkPos)
    private var players = mutableMapOf<UUID, PlayerState>()

    init {
        addCustomServerClosable { close(); players.clear() }

        PersistentEvents.serverOnTick.on { (server), _ ->
            val viewDistance = server.playerList.viewDistance
            for (player in server.playerList.players) {
                val level = player.serverLevel()
                val playerDimension = level.dimensionId
                val curCPos = player.blockPosition().toChunkPos()

                val state = players.getOrPut(player.uuid) { PlayerState("", ChunkPos(-999999999, -999999999)) }
                if (state.dimension != playerDimension) {
                    state.dimension = playerDimension
                    state.lastCPos = ChunkPos(-999999999, -999999999)
                }
                if (state.lastCPos == curCPos) continue
                val cPosToPage = dimToCPosToPage[playerDimension] ?: continue

                lock {
                    uuidToPlayer[player.uuid] = player
                    val playerCached = subscribersSavedChecksums.getOrPut(player.uuid) { mutableMapOf() }
                    val oldCached = playerCached.keys
                    val renderPages = mutableSetOf<Long>()

                    for (cPos in ChunkPos.rangeClosed(curCPos, viewDistance)) {
                        val page = cPosToPage[cPos] ?: continue
                        renderPages.add(page)
                    }

                    val toRemove = oldCached.subtract(renderPages)
                    val toAdd = renderPages.subtract(oldCached)

                    toRemove.forEach { playerCached.remove(it) }
                    toAdd.forEach { playerCached.getOrPut(it) { mutableMapOf() } }
                    if (toRemove.isNotEmpty() || toAdd.isNotEmpty()) {
                        forceUpdate = true
                    }
                }
            }
        }
    }
}