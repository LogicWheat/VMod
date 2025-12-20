package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.extensions.SnapModeExtension
import net.spaceeye.vmod.toolgun.modes.state.HydraulicsMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.toolgun.modes.extensions.ThreeClicksActivationSteps.*
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.vEntityManaging.types.constraints.HydraulicsConstraint

interface HydraulicsHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as HydraulicsMode
        val paStage = getExtensionOfType<SnapModeExtension>().paStage
        when {
            paStage == FIRST_RAYCAST && !primaryFirstRaycast -> makeText(
                if (connectionMode == HydraulicsConstraint.ConnectionMode.HINGE_ORIENTATION) COMMON_HUD_1.get() else COMMON_HUD_6.get()
            )
            primaryFirstRaycast -> makeText(COMMON_HUD_2.get())
            paStage == SECOND_RAYCAST -> makeText(COMMON_HUD_3.get())
            paStage == FINALIZATION -> makeText(COMMON_HUD_4.get())
        }
    }
}