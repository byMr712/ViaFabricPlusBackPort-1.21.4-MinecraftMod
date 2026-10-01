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

package com.viaversion.viafabricplus.injection.mixin.base.connection;

import com.mojang.brigadier.tree.RootCommandNode;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkState;
import net.minecraft.network.handler.DecoderHandler;
import net.minecraft.network.handler.NetworkStateTransitionHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DecoderHandler.class)
public abstract class MixinDecoderHandler {

    @Shadow
    @Final
    private NetworkState<?> state;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void decodeWithFallback(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out, CallbackInfo ci) throws Exception {
        if (buf.readableBytes() == 0) {
            return;
        }

        final boolean hasViaActive = (ProtocolTranslator.getTargetVersion() != ProtocolTranslator.NATIVE_VERSION)
                || (ctx.channel() != null && ctx.channel().hasAttr(ProtocolTranslator.TARGET_VERSION_ATTRIBUTE_KEY));

        if (!hasViaActive) {
            return;
        }

        ci.cancel();

        final int startReadable = buf.readableBytes();
        final int startIndex = buf.readerIndex();

        try {
            final Packet<?> packet = (Packet<?>) this.state.codec().decode(buf);
            final PacketType<?> packetType = packet.getPacketType();
            FlightProfiler.INSTANCE.onPacketReceived(this.state.id(), packetType, ctx.channel().remoteAddress(), startReadable);

            if (buf.readableBytes() > 0) {
                ViaFabricPlusImpl.INSTANCE.logger().warn("Packet {}/{} had {} extra bytes in buffer. Skipping trailing bytes to avoid disconnect.", this.state.id().getId(), packetType, buf.readableBytes());
                buf.skipBytes(buf.readableBytes());
            }

            out.add(packet);
            NetworkStateTransitionHandler.onDecoded(ctx, packet);
        } catch (final Throwable t) {
            if (viaFabricPlus$isCommandTreeError(t)) {
                ViaFabricPlusImpl.INSTANCE.logger().warn("Intercepted CommandTree decode error: {}. Substituting empty command tree fallback to prevent disconnect.", t.getMessage());
                buf.readerIndex(startIndex + startReadable);
                final CommandTreeS2CPacket fallback = new CommandTreeS2CPacket(new RootCommandNode<>());
                out.add(fallback);
                NetworkStateTransitionHandler.onDecoded(ctx, fallback);
                return;
            }

            final int mode = GeneralSettings.INSTANCE.ignorePacketTranslationErrors.getIndex();
            if (mode != 0) {
                ViaFabricPlusImpl.INSTANCE.logger().error("Error occurred while decoding packet in DecoderHandler; dropping packet due to ignorePacketTranslationErrors setting", t);
                buf.readerIndex(startIndex + startReadable);
                if (mode == 1) {
                    ChatUtil.sendPrefixedMessage(Text.translatable("translation.viafabricplus.packet_error").formatted(Formatting.RED));
                }
                return;
            }

            if (t instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(t);
        }
    }

    @Unique
    private static boolean viaFabricPlus$isCommandTreeError(Throwable t) {
        while (t != null) {
            final String msg = t.getMessage();
            if (msg != null && (msg.contains("commands") || msg.contains("class_2641") || msg.contains("CommandTreeS2CPacket"))) {
                return true;
            }
            for (final StackTraceElement element : t.getStackTrace()) {
                final String cn = element.getClassName();
                if (cn.contains("CommandTreeS2CPacket") || cn.contains("class_2641")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

}
