package net.spaceeye.vmod.rendering.types.deprecated

import com.fasterxml.jackson.annotation.JsonIgnore
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.FriendlyByteBuf
import net.spaceeye.vmod.VMBlocks
import net.spaceeye.vmod.limits.ClientLimits
import net.spaceeye.vmod.reflectable.AutoSerializable
import net.spaceeye.vmod.reflectable.ByteSerializableItem
import net.spaceeye.vmod.reflectable.ReflectableObject
import net.spaceeye.vmod.rendering.types.BaseRenderer
import net.spaceeye.vmod.rendering.types.BlockRenderer
import net.spaceeye.vmod.rendering.types.BlockStateRenderer
import net.spaceeye.vmod.rendering.types.PositionDependentRenderer
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.utils.vs.updatePosition
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.properties.ShipId
import java.awt.Color

//TODO remove eventually?
@Deprecated("", ReplaceWith("BlockStateRenderer"), DeprecationLevel.ERROR)
class ConeBlockRenderer(): BlockRenderer(), ReflectableObject, PositionDependentRenderer {
    private class Data: AutoSerializable {
        @JsonIgnore
        private var i = 0

        var shipId: Long by ByteSerializableItem.get(i++, -1L)
        var pos: Vector3d by ByteSerializableItem.get(i++, Vector3d())
        var rot: Quaterniond by ByteSerializableItem.get(i++, Quaterniond())
        var scale: Float by ByteSerializableItem.get(i++, 1.0f, true) { ClientLimits.instance.blockRendererScale.get(it) }
        var color: Color by ByteSerializableItem.get(i++, Color(255, 255, 255))
        var fullbright: Boolean by ByteSerializableItem.get(i++, false, true) { ClientLimits.instance.lightingMode.get(it)
        }
    }
    private var data = Data()
    override val reflectObjectOverride: ReflectableObject? get() = data
    override fun serialize() = data.serialize()
    override fun deserialize(buf: FriendlyByteBuf) { data.deserialize(buf) }
    override val renderingPosition: Vector3d get() = data.pos

    constructor(
        pos: Vector3d,
        rot: Quaterniond,
        scale: Float,
        shipId: ShipId,
        color: Color = Color(255, 255, 255),
        fullbright: Boolean
    ): this() { with(data) {
        this.pos = pos
        this.rot = rot
        this.scale = scale
        this.shipId = shipId
        this.color = color
        this.fullbright = fullbright
    } }

    override fun highlightUntil(until: Long) = throw NotImplementedError("use BlockStateRenderer")
    override fun highlightUntilRenderingTicks(until: Long) = throw NotImplementedError("use BlockStateRenderer")
    override fun renderBlockData(poseStack: PoseStack, camera: Camera, buffer: MultiBufferSource, timestamp: Long) = throw NotImplementedError("use BlockStateRenderer")
    override fun scaleBy(by: Double) = throw NotImplementedError("use BlockStateRenderer")

    override fun copy(oldToNew: Map<ShipId, Ship>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): BaseRenderer? = with(data) {
        val spoint = centerPositions[shipId]!!.let { (old, new) -> updatePosition(pos, old, new) }
        return BlockStateRenderer(spoint, Quaterniond(rot), scale, oldToNew[shipId]!!.id, color, fullbright, VMBlocks.CONE_THRUSTER.get().defaultBlockState())
    }

    override fun tryUpdate(): BaseRenderer = with(data) {
        return BlockStateRenderer(pos.copy(), Quaterniond(rot), scale, shipId, color, fullbright, VMBlocks.CONE_THRUSTER.get().defaultBlockState())
    }
}