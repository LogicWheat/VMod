package net.spaceeye.vmod.toolgun.modes.hud

import net.spaceeye.vmod.toolgun.modes.extensions.PlacementAssistExtension
import net.spaceeye.vmod.toolgun.modes.state.ConnectionMode
import net.spaceeye.vmod.toolgun.modes.util.SimpleHUD
import net.spaceeye.vmod.toolgun.modes.extensions.ThreeClicksActivationSteps.*
import net.spaceeye.vmod.translate.*
import net.spaceeye.vmod.vEntityManaging.types.constraints.ConnectionConstraint

interface ConnectionHUD: SimpleHUD {
    override fun makeSubText(makeText: (String) -> Unit) {
        this as ConnectionMode
        val paStage = getExtensionOfType<PlacementAssistExtension>().paStage
        when {
            paStage == FIRST_RAYCAST && !primaryFirstRaycast && !paMiddleFirstRaycast -> makeText(
                if (connectionMode == ConnectionConstraint.ConnectionModes.HINGE_ORIENTATION) COMMON_HUD_1.get() else COMMON_HUD_6.get()
            )
            primaryFirstRaycast || paMiddleFirstRaycast -> makeText(COMMON_HUD_2.get())
            paStage == SECOND_RAYCAST -> makeText(COMMON_HUD_3.get())
            paStage == FINALIZATION -> makeText(COMMON_HUD_4.get())
        }
    }
}