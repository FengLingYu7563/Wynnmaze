package com.wynnmaze.mixin;

import com.wynnmaze.WynnMazeClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 攔截伺服器發來的粒子封包（ParticleS2CPacket）
 * 這比攔截 ParticleManager 更早：封包一到客戶端就攔截，
 * 不管玩家距離出口多遠，只要伺服器有傳就能捕捉到。
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ParticleManagerMixin {

    @Inject(
        method = "onParticle(Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;)V",
        at = @At("HEAD")
    )
    private void onParticlePacket(ParticleS2CPacket packet, CallbackInfo ci) {
        WynnMazeClient.EXIT_TRACKER.onParticle(
            packet.getParameters(),
            packet.getX(),
            packet.getY(),
            packet.getZ()
        );
    }
}
