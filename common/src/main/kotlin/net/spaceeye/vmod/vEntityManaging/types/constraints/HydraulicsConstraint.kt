package net.spaceeye.vmod.vEntityManaging.types.constraints

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.spaceeye.vmod.vEntityManaging.*
import net.spaceeye.vmod.vEntityManaging.util.*
import net.spaceeye.vmod.utils.*
import net.spaceeye.vmod.utils.vs.copy
import org.valkyrienskies.core.api.ships.properties.ShipId
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import net.spaceeye.vmod.utils.vs.tryMovePosition
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointMaxForceTorque
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel

class HydraulicsConstraint(): TwoShipsMConstraint(), VEAutoSerializable, Tickable {
    //TODO unify and rename values (needs backwards compat)
    enum class ConnectionMode {
        FIXED_ORIENTATION,
        HINGE_ORIENTATION,
        FREE_ORIENTATION
    }

    override var sPos1: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var sPos2: Vector3d by get(i++, Vector3d()).also { it.metadata["NoTagSerialization"] = true }
    override var shipId1: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }
    override var shipId2: Long by get(i++, -1L).also { it.metadata["NoTagSerialization"] = true }

    var minLength: Float by get(i++, -1f)
    var maxLength: Float by get(i++, -1f)

    var extensionSpeed: Float by get(i++, 1f)
    var extendedDist: Float by get(i++, 0f)

    var channel: String by get(i++, "")

    var connectionMode: ConnectionMode by get(i++, ConnectionMode.FIXED_ORIENTATION)

    var sRot1: Quaterniond by get(i++, Quaterniond())
    var sRot2: Quaterniond by get(i++, Quaterniond())

    //shipyard direction and scale information
    var sDir1: Vector3d by get(i++, Vector3d())
    var sDir2: Vector3d by get(i++, Vector3d())

    var maxForce: Float by get(i++, -1f)
    var stiffness: Float by get(i++, 0f)
    var damping: Float by get(i++, 0f)

    var d1: VSJoint? = null
    var d2: VSJoint? = null

    constructor(
        sPos1: Vector3d,
        sPos2: Vector3d,

        sDir1: Vector3d,
        sDir2: Vector3d,

        sRot1: Quaterniondc,
        sRot2: Quaterniondc,

        shipId1: ShipId,
        shipId2: ShipId,

        maxForce: Float,
        stiffness: Float,
        damping: Float,

        minLength: Float,
        maxLength: Float,
        extensionSpeed: Float,

        channel: String,

        connectionMode: ConnectionMode,
        ): this() {
        this.sPos1 = sPos1.copy()
        this.sPos2 = sPos2.copy()

        this.sDir1 = sDir1.copy()
        this.sDir2 = sDir2.copy()

        this.sRot1 = Quaterniond(sRot1)
        this.sRot2 = Quaterniond(sRot2)

        this.shipId1 = shipId1
        this.shipId2 = shipId2

        this.minLength = minLength
        this.maxLength = maxLength
        this.extensionSpeed = extensionSpeed

        this.channel = channel
        this.connectionMode = connectionMode

        this.maxForce = maxForce
        this.stiffness = stiffness
        this.damping = damping
    }

    override fun iCopyVEntity(level: ServerLevel, mapped: Map<ShipId, ShipId>, centerPositions: Map<ShipId, Pair<Vector3d, Vector3d>>): VEntity? {
        return HydraulicsConstraint(
            tryMovePosition(sPos1, shipId1, centerPositions) ?: return null,
            tryMovePosition(sPos2, shipId2, centerPositions) ?: return null,
            sDir1, sDir2, sRot1, sRot2,
            mapped[shipId1] ?: return null,
            mapped[shipId2] ?: return null,
            maxForce, stiffness, damping,
            minLength, maxLength, extensionSpeed, channel, connectionMode
        )
    }

    override fun iOnScaleBy(level: ServerLevel, scaleBy: Double, scalingCenter: Vector3d) {
        val scaleBy = scaleBy.toFloat()
        minLength *= scaleBy
        maxLength *= scaleBy
        extendedDist *= scaleBy
        extensionSpeed *= scaleBy

        sDir1.divAssign(scaleBy)
        sDir2.divAssign(scaleBy)

        lastExtended = -1.0f
    }

    var wasDeleted = false
    var lastExtended: Float = 0f
    var targetPercentage = 0f

    private fun tryExtendDist(): Boolean {
        val length = maxLength - minLength

        val currentPercentage = extendedDist / length
        if (abs(currentPercentage - targetPercentage) < 1e-6) { return false }
        val left = targetPercentage - currentPercentage
        // 60 phys ticks in a second
        val percentageStep = extensionSpeed / 60f / length
        extendedDist += min(percentageStep, abs(left)) * length * left.sign
        return true
    }

    override fun serverTick(server: MinecraftServer, unregister: () -> Unit) {
        if (wasDeleted) {
            unregister()
            return
        }
        getExtensionsOfType<TickableVEntityExtension>().forEach { it.tick(server) }
    }

    override fun physTick(level: VsiPhysLevel, delta: Double) {
        if (cIDs.isEmpty()) {return}
        if (!tryExtendDist()) {return}

        if (lastExtended == extendedDist) {return}
        lastExtended = extendedDist

        val (sPos1, sPos2, sDir1, sDir2) = when (-1L) {
            shipId1 -> Tuple.of(sPos1 + 0.5, sPos2,  sDir1, sDir2)
            shipId2 -> Tuple.of(sPos2 + 0.5, sPos1, -sDir2, sDir1)
            else    -> Tuple.of(sPos1      , sPos2,  sDir1, sDir2)
        }
        val distance = (minLength + extendedDist)
        val p11 = sPos1.toJomlVector3d()
        val p21 = (sPos2 - sDir2 * distance).toJomlVector3d()
        val p12 = (sPos1 + sDir1 * distance).toJomlVector3d()
        val p22 = sPos2.toJomlVector3d()

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {
            d1 = (d1 as VSDistanceJoint).copy(minDistance = distance, maxDistance = distance)
            level.updateJoint(cIDs[0], d1!!)
        } else {
            d1 = d1!!.copy(null, p11, null, null, p21, null)
            d2 = d2!!.copy(null, p12, null, null, p22, null)
            level.updateJoint(cIDs[0], d1!!)
            level.updateJoint(cIDs[1], d2!!)
        }
    }

    override fun iOnMakeVEntity(level: ServerLevel) = withFutures {
        if (shipId1 == -1L && shipId2 == -1L) {throw AssertionError("Both shipId's are ground")}
        val (shipId1, shipId2, sPos1, sPos2, sDir1, sDir2, sRot1, sRot2) = when (-1L) {
            shipId1 -> Tuple.of(null   , shipId2, sPos1 + 0.5, sPos2,  sDir1, sDir2, sRot1, sRot2)
            shipId2 -> Tuple.of(null   , shipId1, sPos2 + 0.5, sPos1, -sDir2, sDir1, sRot2, sRot1)
            else    -> Tuple.of(shipId1, shipId2, sPos1      , sPos2,  sDir1, sDir2, sRot1, sRot2)
        }

        val maxForceTorque = if (maxForce < 0) {null} else {VSJointMaxForceTorque(maxForce, maxForce)}
        val stiffness = if (stiffness < 0) {null} else {stiffness}
        val damping = if (damping < 0) {null} else {damping}
        val distance = (minLength + extendedDist)

        if (connectionMode == ConnectionMode.FREE_ORIENTATION) {
            d1 = VSDistanceJoint(
                shipId1, VSJointPose(sPos1.toJomlVector3d(), Quaterniond()),
                shipId2, VSJointPose(sPos2.toJomlVector3d(), Quaterniond()),
                maxForceTorque, distance, distance, stiffness = stiffness, damping = damping
            )
            mc(d1!!, level)
            return@withFutures
        }

        val p11 = sPos1.toJomlVector3d()
        val p21 = (sPos2 - sDir2 * distance).toJomlVector3d()
        val p12 = (sPos1 + sDir1 * distance).toJomlVector3d()
        val p22 = sPos2.toJomlVector3d()

        when (connectionMode) {
            ConnectionMode.FIXED_ORIENTATION -> {
                d1 = VSFixedJoint(
                    shipId1, VSJointPose(p11, sRot1.invert(Quaterniond())),
                    shipId2, VSJointPose(p21, sRot2.invert(Quaterniond())),
                    maxForceTorque,
                )
                d2 = VSFixedJoint(
                    shipId1, VSJointPose(p12, sRot1.invert(Quaterniond())),
                    shipId2, VSJointPose(p22, sRot2.invert(Quaterniond())),
                    maxForceTorque,
                )

                mc(d1!!, level)
                mc(d2!!, level)
            }
            ConnectionMode.HINGE_ORIENTATION -> {
                d1 = VSDistanceJoint(
                    shipId1, VSJointPose(p11, Quaterniond()),
                    shipId2, VSJointPose(p21, Quaterniond()),
                    maxForceTorque, 0f, 0f, stiffness = stiffness, damping = damping
                )
                d2 = VSDistanceJoint(
                    shipId1, VSJointPose(p12, Quaterniond()),
                    shipId2, VSJointPose(p22, Quaterniond()),
                    maxForceTorque, 0f, 0f, stiffness = stiffness, damping = damping
                )

                val r1 = VSRevoluteJoint(
                    shipId1, VSJointPose(p11, getHingeRotation(sDir1)),
                    shipId2, VSJointPose(p21, getHingeRotation(sDir2)),
                    maxForceTorque, driveFreeSpin = true
                )
                val r2 = VSRevoluteJoint(
                    shipId1, VSJointPose(p12, getHingeRotation(sDir1)),
                    shipId2, VSJointPose(p22, getHingeRotation(sDir2)),
                    maxForceTorque, driveFreeSpin = true
                )

                mc(d1!!, level)
                mc(d2!!, level)
                mc(r1, level)
                mc(r2, level)
            }
            ConnectionMode.FREE_ORIENTATION -> throw AssertionError()
        }
    }

    override fun iOnDeleteVEntity(level: ServerLevel) {
        super.iOnDeleteVEntity(level)
        wasDeleted = true
    }
}