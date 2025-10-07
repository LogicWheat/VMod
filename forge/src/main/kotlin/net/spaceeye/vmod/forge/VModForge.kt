package net.spaceeye.vmod.forge

import dev.architectury.platform.forge.EventBuses
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.VM
import net.spaceeye.vmod.VM.init
import net.spaceeye.vmod.VMClientCommands
import net.spaceeye.vmod.rendering.RenderTypes

@Mod(VM.MOD_ID)
class VModForge {
    init {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(VM.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        init()
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            MinecraftForge.EVENT_BUS.addListener<RegisterClientCommandsEvent> { VMClientCommands.registerClientCommands(it.dispatcher) }
            MinecraftForge.EVENT_BUS.addListener<RegisterShadersEvent> {
                for (state in RenderTypes.states) {
                    it.registerShader(ShaderInstance(it.resourceProvider, ResourceLocation(MOD_ID, state.name), RenderTypes.VERTEX_FORMAT), state.shaderSetter)
                }
            }
        } }
    }
}