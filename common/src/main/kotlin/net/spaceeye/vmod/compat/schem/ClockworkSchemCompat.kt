package net.spaceeye.vmod.compat.schem

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.vmod.utils.JVector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.util.putVector3d

class ClockworkSchemCompat(): SchemCompatItem {
    override fun onCopy(level: ServerLevel, pos: BlockPos, state: BlockState, ships: List<ServerShip>, centerPositions: Map<Long, JVector3d>, be: BlockEntity?, tag: CompoundTag?, cancelBlockCopying: () -> Unit) {}
    override fun onPaste(level: ServerLevel, oldToNewId: Map<Long, Long>, centerPositions: Map<Long, Pair<JVector3d, JVector3d>>, tag: CompoundTag, pos: BlockPos, state: BlockState, tagTransformer: (((CompoundTag?) -> CompoundTag?)?) -> Unit, afterPasteCallbackSetter: ((be: BlockEntity?) -> Unit) -> Unit) {
        if (state.block != ClockworkBlocks.PHYS_BEARING.get()) {return}
        tagTransformer {
            val oldId = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, oldToNewId[oldId] ?: -1)
            val (oldCenter, newCenter) = centerPositions[oldId]!!
            val oldShiptraptionCenter = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER)!!
            tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, oldShiptraptionCenter.sub(oldCenter).add(newCenter))
            tag
        }
    }
}