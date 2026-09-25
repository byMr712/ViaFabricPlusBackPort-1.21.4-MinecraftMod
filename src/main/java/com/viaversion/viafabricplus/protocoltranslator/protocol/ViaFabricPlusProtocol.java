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

import com.google.common.collect.Lists;
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
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.util.Key;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.custom.DebugGameTestAddMarkerCustomPayload;
import net.minecraft.network.packet.s2c.custom.DebugGameTestClearCustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class ViaFabricPlusProtocol extends AbstractProtocol<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_2, ServerboundPacket1_21_2> {

    public static final ViaFabricPlusProtocol INSTANCE = new ViaFabricPlusProtocol();

    private final Map<String, Pair<ProtocolVersion, PacketReader>> payloadDiff = new HashMap<>();

    public ViaFabricPlusProtocol() {
        super(ClientboundPacket1_21_2.class, ClientboundPacket1_21_2.class, ServerboundPacket1_21_2.class, ServerboundPacket1_21_2.class);
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

        // Fixes an issue where the Fabric Particle API causes disconnects when both the client and server have the mod installed and both are 1.21.5+.
        // See https://github.com/ViaVersion/ViaFabric/issues/428
        this.registerServerbound(ServerboundConfigurationPackets1_20_5.CUSTOM_PAYLOAD, wrapper -> {
            final ProtocolVersion serverVersion = wrapper.user().getProtocolInfo().serverProtocolVersion();
            if (serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_21_5) && !serverVersion.equals(wrapper.user().getProtocolInfo().protocolVersion())) {
                final String channel = Key.namespaced(wrapper.passthrough(Types.STRING));
                if (channel.equals("minecraft:register") || channel.equals("minecraft:unregister")) {
                    final List<String> channels = Lists.newArrayList(new String(wrapper.passthrough(Types.SERVERBOUND_CUSTOM_PAYLOAD_DATA), StandardCharsets.UTF_8).split("\0"));
                    if (channels.remove("fabric:extended_block_particle_option_sync")) {
                        if (!channels.isEmpty()) {
                            wrapper.set(Types.SERVERBOUND_CUSTOM_PAYLOAD_DATA, 0, String.join("\0", channels).getBytes(StandardCharsets.UTF_8));
                        } else {
                            wrapper.cancel();
                        }
                    }
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

    @Override
    protected void applySharedRegistrations() {
        // Not for us, protocols will already track states down the line
    }

    private void registerMapping(final CustomPayload.Id<?> id, final ProtocolVersion version, final PacketReader reader) {
        payloadDiff.put(id.id().toString(), new Pair<>(version, reader));
    }

    public static ServerboundPacketType getSetCreativeModeSlot() {
        return ServerboundPackets1_21_2.SET_CREATIVE_MODE_SLOT;
    }

    public static ClientboundPacketType getCustomPayload() {
        return ClientboundPackets1_21_2.CUSTOM_PAYLOAD;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_2, ClientboundPacket1_21_2, ServerboundPacket1_21_2, ServerboundPacket1_21_2> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }

    @FunctionalInterface
    interface PacketReader {

        void read(PacketWrapper wrapper);

    }

}
