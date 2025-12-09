package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.shadow.Er;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;

import java.util.Objects;

@Mixin(Er.class)
abstract public class ShipObjectServerWorldMixin {
    //let's hope that this works
    @Inject(method = "deleteShip", at = @At("HEAD"), remap = false)
    void vmod$onDeleteShip(ServerShip ship, CallbackInfo ci) {
        AVSEvents.INSTANCE.getServerShipRemoveEvent().emit(new AVSEvents.ServerShipRemoveEvent(ship));
    }

    @ModifyArgs(
            method = "onSetBlock",
            at = @At(value = "INVOKE", target = "Lorg/valkyrienskies/core/impl/shadow/Et;onSetBlock(IIILjava/lang/String;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;Lorg/valkyrienskies/core/internal/world/chunks/VsiBlockType;DD)V"),
            remap = false
    )
    void vmod$onSetBlock(Args args) {
        var posX = (int)args.get(0);
        var posY = (int)args.get(1);
        var posZ = (int)args.get(2);
        var dimensionId = (String)args.get(3);
        var oldBT = (VsiBlockType)args.get(4);
        var newBT = (VsiBlockType)args.get(5);
        var oldMass = (double)args.get(6);

        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(dimensionId, posX, posY, posZ);
        if (oldBT != newBT) {
            CustomBlockMassManager.INSTANCE.removeCustomMass(dimensionId, posX, posY, posZ);
        }

        args.set(6, Objects.requireNonNullElse(mass, oldMass));
    }
}
