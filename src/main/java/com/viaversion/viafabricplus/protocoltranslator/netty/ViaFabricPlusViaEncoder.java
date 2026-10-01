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

package com.viaversion.viafabricplus.protocoltranslator.netty;

import com.viaversion.vialoader.netty.ViaEncoder;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.handler.HandlerNames;
import net.minecraft.network.handler.NetworkStateTransitions;

public final class ViaFabricPlusViaEncoder extends ViaEncoder {

    public ViaFabricPlusViaEncoder(UserConnection connection) {
        super(connection);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof NetworkStateTransitions.EncoderTransitioner transitioner) {
            try {
                ChannelHandlerContext targetCtx = ctx.pipeline().context("outbound_config");
                if (targetCtx == null) targetCtx = ctx.pipeline().context("encoder");
                if (targetCtx == null) targetCtx = ctx.pipeline().context(HandlerNames.OUTBOUND_CONFIG);
                if (targetCtx == null) targetCtx = ctx.pipeline().context(HandlerNames.ENCODER);
                if (targetCtx != null) {
                    transitioner.run(targetCtx);
                }
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            } catch (final Throwable t) {
                promise.setFailure(t);
            }
            return;
        }

        if (msg instanceof NetworkStateTransitions.DecoderTransitioner transitioner) {
            try {
                ChannelHandlerContext targetCtx = ctx.pipeline().context("inbound_config");
                if (targetCtx == null) targetCtx = ctx.pipeline().context("decoder");
                if (targetCtx == null) targetCtx = ctx.pipeline().context(HandlerNames.INBOUND_CONFIG);
                if (targetCtx == null) targetCtx = ctx.pipeline().context(HandlerNames.DECODER);
                if (targetCtx != null) {
                    transitioner.run(targetCtx);
                }
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            } catch (final Throwable t) {
                promise.setFailure(t);
            }
            return;
        }

        if (!(msg instanceof ByteBuf)) {
            try {
                if (msg instanceof Runnable runnable) {
                    runnable.run();
                }
                ReferenceCountUtil.release(msg);
                promise.setSuccess();
            } catch (final Throwable t) {
                promise.setFailure(t);
            }
            return;
        }

        super.write(ctx, msg, promise);
    }

}
