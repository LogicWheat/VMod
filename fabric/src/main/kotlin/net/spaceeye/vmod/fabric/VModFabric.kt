package net.spaceeye.vmod.fabric

import com.mojang.brigadier.CommandDispatcher
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.ResourceLocation
import net.spaceeye.vmod.MOD_ID
import net.spaceeye.vmod.VM.init
import net.spaceeye.vmod.VMClientCommands
import net.spaceeye.vmod.rendering.RenderTypes
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric

class VModFabric : ModInitializer {
    override fun onInitialize() {
        ValkyrienSkiesModFabric().onInitialize()
        init()
        EnvExecutor.runInEnv(Env.CLIENT) { Runnable {
            ClientCommandRegistrationCallback.EVENT.register { it, a -> VMClientCommands.registerClientCommands(it as CommandDispatcher<CommandSourceStack>) }
            CoreShaderRegistrationCallback.EVENT.register {
                it.register(ResourceLocation(MOD_ID, "texture_array_full"), RenderTypes.VERTEX_FORMAT) {
                    RenderTypes.textureArrayShader = it
                }
            }
        } }
    }
}

class VModFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
    }
}