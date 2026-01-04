package net.spaceeye.vmod.utils.vs

import net.spaceeye.vmod.utils.JVector3d
import net.spaceeye.vmod.utils.JVector3dc
import net.spaceeye.vmod.utils.Vector3d
import org.joml.Quaterniondc
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.VSDistanceJoint
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint

fun updatePosition(old: Vector3d, oldCenter: Vector3d, newCenter: Vector3d): Vector3d = old - oldCenter + newCenter

fun tryMovePosition(pos: Vector3d, shipId: Long, mapped: Map<ShipId, Pair<Vector3d, Vector3d>>): Vector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos - oldCenter + newCenter
}

fun tryMovePosition(pos: Vector3dc, shipId: Long, mapped: Map<ShipId, Pair<Vector3dc, Vector3dc>>): JVector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos.sub(oldCenter, JVector3d()).add(newCenter)
}

fun tryMovePositionJ(pos: Vector3d, shipId: Long, mapped: Map<ShipId, Pair<JVector3d, JVector3dc>>): Vector3d? {
    val (oldCenter, newCenter) = mapped[shipId] ?: return null
    return pos.sub(oldCenter.x, oldCenter.y, oldCenter.z).add(newCenter.x(), newCenter.y(), newCenter.z())
}

fun VSJoint.copy(
    shipId0: ShipId? = this.shipId0,
    pos0: Vector3dc = this.pose0.pos,
    rot0: Quaterniondc = this.pose0.rot,
    shipId1: ShipId? = this.shipId1,
    pos1: Vector3dc = this.pose1.pos,
    rot1: Quaterniondc = this.pose1.rot): VSJoint {
    return when (this) {
        is VSDistanceJoint -> this.copy(shipId0, VSJointPose(pos0, rot0), shipId1, VSJointPose(pos1, rot1))
        is VSFixedJoint -> this.copy(shipId0, VSJointPose(pos0, rot0), shipId1, VSJointPose(pos1, rot1))
        is VSRevoluteJoint -> this.copy(shipId0, VSJointPose(pos0, rot0), shipId1, VSJointPose(pos1, rot1))
        else -> throw AssertionError()
    }
}