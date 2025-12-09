package net.spaceeye.vmod.mixin;

import net.spaceeye.vmod.events.AVSEvents;
import net.spaceeye.vmod.vsStuff.CustomBlockMassManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Unique int vmod$posX;
    @Unique int vmod$posY;
    @Unique int vmod$posZ;
    @Unique String vmod$dimensionId;
    @Unique VsiBlockType vmod$oldBlockType;
    @Unique VsiBlockType vmod$newBlockType;
    @Unique double vmod$oldBlockMass;
    @Unique double vmod$newBlockMass;

    @Inject(
            method = "onSetBlock",
            at = @At(value = "HEAD"),
            remap = false
    )
    void vmod$captureArgs(int posX, int posY, int posZ, String dimensionId, VsiBlockType oldBlockType, VsiBlockType newBlockType, double oldBlockMass, double newBlockMass, CallbackInfo ci) {
        vmod$posX = posX;
        vmod$posY = posY;
        vmod$posZ = posZ;
        vmod$dimensionId = dimensionId;
        vmod$oldBlockType = oldBlockType;
        vmod$newBlockType = newBlockType;
        vmod$oldBlockMass = oldBlockMass;
        vmod$newBlockMass = newBlockMass;
    }

    @ModifyArg(
            method = "onSetBlock",
            at = @At(value = "HEAD"),
            remap = false,
            index = 6
    )
    double vmod$onSetBlock(double oldBlockMass) {
        Double mass = CustomBlockMassManager.INSTANCE.getCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
        if (vmod$oldBlockType != vmod$newBlockType) {
            CustomBlockMassManager.INSTANCE.removeCustomMass(vmod$dimensionId, vmod$posX, vmod$posY, vmod$posZ);
        }

        return Objects.requireNonNullElse(mass, oldBlockMass);
    }
}
