/*
 * This file is part of ViaFabricPlus - https://github.com/ViaVersion/ViaFabricPlus
 * Copyright (C) 2021-2025 the original authors
 *                         - FlorianMichael/EnZaXD <florian.michael07@gmail.com>
 *                         - RK_01/RaphiMC
 * Copyright (C) 2023-2025 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viafabricplus.injection.mixin.base.integration;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.ConnectException;
import java.net.SocketException;

import net.minecraft.util.Formatting;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void printNetworkingErrors(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        if (DebugSettings.INSTANCE.printNetworkingErrorsToLogs.getValue()) {
            if (ex instanceof SocketException || ex instanceof ConnectException) {
                // Thrown when server is not reachable
                return;
            }
            ViaFabricPlusImpl.INSTANCE.logger().error("An exception occurred while handling a packet", ex);
        }
    }

    @WrapOperation(method = "exceptionCaught", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;disconnect(Lnet/minecraft/network/DisconnectionInfo;)V"))
    private void friendlyDisconnectReason(ClientConnection instance, DisconnectionInfo info, Operation<Void> original, ChannelHandlerContext context, Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = ex.getClass().getSimpleName();
        }
        final String cleanMsg = Formatting.strip(msg);
        final Text friendlyText = Text.empty()
                .append(Text.literal("Автоопределение версии не удалось или произошла сетевая ошибка\n").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal("Попробуйте указать версию вручную в названии сервера (например '26.2') или через меню ViaFabricPlus в списке серверов.\n\n").formatted(Formatting.YELLOW))
                .append(Text.literal("Детали: " + cleanMsg).formatted(Formatting.GRAY));
        original.call(instance, new DisconnectionInfo(friendlyText, info.report(), info.bugReportLink()));
    }

}
