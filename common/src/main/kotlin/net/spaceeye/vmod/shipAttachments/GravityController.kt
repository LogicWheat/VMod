package net.spaceeye.vmod.shipAttachments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.spaceeye.vmod.compat.vsBackwardsCompat.mass
import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.Vector3d
import net.spaceeye.vmod.vsStuff.VSGravityManager
import net.spaceeye.vmod.compat.vsBackwardsCompat.getAttachment
import net.spaceeye.vmod.utils.MyVectorDeserializer
import net.spaceeye.vmod.utils.MyVectorSerializer
import org.valkyrienskies.core.api.ships.*
import kotlin.reflect.KClass

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
class GravityController(
    var dimensionId: String,
): ShipForcesInducer {
    var useDimensionGravity = true

    @JsonIgnore
    var dimensionGravity = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
    @JsonSerialize(using = MyVectorSerializer::class)
    @JsonDeserialize(using = MyVectorDeserializer::class)
    @JsonProperty(required = false)
    var gravityVector = dimensionGravity

    override fun applyForces(physShip: PhysShip) {
        val gravityVector = if (useDimensionGravity) dimensionGravity else gravityVector
        val forceDiff = (gravityVector - VS_DEFAULT_GRAVITY) * physShip.mass
        if (forceDiff.sqrDist() < Float.MIN_VALUE) return

        physShip.applyInvariantForce(forceDiff.toJomlVector3d())
    }

    fun reset() {
        gravityVector = VSGravityManager.getDimensionGravityMutableReference(dimensionId)
        useDimensionGravity = true
    }

    @JsonIgnore
    fun effectiveGravity() = if (useDimensionGravity) dimensionGravity else gravityVector

    companion object {
        val VS_DEFAULT_GRAVITY = Vector3d(0, -10, 0)

        fun getOrCreate(ship: LoadedServerShip) =
            ship.getAttachment<GravityController>()
                ?: GravityController(ship.chunkClaimDimension).also {
                    ship.saveAttachment(it)
                }
    }
}