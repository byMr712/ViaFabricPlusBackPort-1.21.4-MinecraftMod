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

package com.viaversion.viafabricplus.protocoltranslator.protocol;

import com.viaversion.viafabricplus.features.entity.metadata_handling.WolfHealthTracker1_14_4;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.protocol.storage.BedrockJoinGameTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.util.Key;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.custom.DebugGameTestAddMarkerCustomPayload;
import net.minecraft.network.packet.s2c.custom.DebugGameTestClearCustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.util.HashMap;
import java.util.Map;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class ViaFabricPlusProtocol extends AbstractProtocol<ClientboundPacket26_1, ClientboundPacket26_1, ServerboundPacket26_1, ServerboundPacket26_1> {

    public static final ViaFabricPlusProtocol INSTANCE = new ViaFabricPlusProtocol();

    private final Map<String, Pair<ProtocolVersion, PacketReader>> payloadDiff = new HashMap<>();

    public ViaFabricPlusProtocol() {
        super(ClientboundPacket26_1.class, ClientboundPacket26_1.class, ServerboundPacket26_1.class, ServerboundPacket26_1.class);
        registerMapping(BrandCustomPayload.ID, LegacyProtocolVersion.c0_0_15a_1, wrapper -> wrapper.passthrough(Types.STRING));
        registerMapping(DebugGameTestAddMarkerCustomPayload.ID, ProtocolVersion.v1_14, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14);
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.STRING);
            wrapper.passthrough(Types.INT);
        });
        registerMapping(DebugGameTestClearCustomPayload.ID, ProtocolVersion.v1_14, wrapper -> {
        });
    }

    @Override
    protected void registerPackets() {
        registerClientbound(State.PLAY, getCustomPayload().getId(), getCustomPayload().getId(), wrapper -> {
            final String channel = Key.namespaced(wrapper.passthrough(Types.STRING));
            if (!channel.startsWith(Identifier.DEFAULT_NAMESPACE)) {
                return;
            }

            final ProtocolVersion version = wrapper.user().getProtocolInfo().serverProtocolVersion();
            if (!payloadDiff.containsKey(channel) || version.olderThan(payloadDiff.get(channel).getLeft())) {
                wrapper.cancel();
                return;
            }

            if (version.olderThanOrEqualTo(ProtocolVersion.v1_20)) {
                final PacketReader reader = payloadDiff.get(channel).getRight();
                try {
                    reader.read(wrapper);
                    wrapper.read(Types.REMAINING_BYTES);
                } catch (Exception ignored) {
                    wrapper.cancel();
                }
            }
        });
    }

    @Override
    public void init(UserConnection connection) {
        super.init(connection);

        final ProtocolVersion serverVersion = ProtocolTranslator.getTargetVersion(connection.getChannel());

        if (serverVersion.equals(BedrockProtocolVersion.bedrockLatest)) {
            connection.put(new BedrockJoinGameTracker());
        } else if (serverVersion.olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            connection.put(new WolfHealthTracker1_14_4());
        }
    }

    private void registerMapping(final CustomPayload.Id<?> id, final ProtocolVersion version, final PacketReader reader) {
        payloadDiff.put(id.id().toString(), new Pair<>(version, reader));
    }

    public static ServerboundPacketType getSetCreativeModeSlot() {
        return ServerboundPackets26_1.SET_CREATIVE_MODE_SLOT;
    }

    public static ClientboundPacketType getCustomPayload() {
        return ClientboundPackets26_1.CUSTOM_PAYLOAD;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket26_1, ClientboundPacket26_1, ServerboundPacket26_1, ServerboundPacket26_1> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }

    @FunctionalInterface
    interface PacketReader {

        void read(PacketWrapper wrapper);

    }

}
