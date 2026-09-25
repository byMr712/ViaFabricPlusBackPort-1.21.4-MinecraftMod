package com.viaversion.viafabricplus.generator;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.vialoader.ViaLoader;
import com.viaversion.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import com.viaversion.vialoader.impl.platform.ViaAprilFoolsPlatformImpl;
import com.viaversion.vialoader.impl.platform.ViaBedrockPlatformImpl;
import com.viaversion.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaLegacyPlatformImpl;
import com.viaversion.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaVersionPlatformImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.util.ProtocolVersionDetector;
import com.viaversion.viafabricplus.protocoltranslator.impl.command.ViaFabricPlusVLCommandHandler;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLInjector;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class ProtocolPathTest {

    @Test
    public void testProtocolPath() throws Exception {
        ViaLoader.init(
                new ViaFabricPlusViaVersionPlatformImpl(new File("run")),
                new ViaFabricPlusVLLoader(),
                new ViaFabricPlusVLInjector(),
                new ViaFabricPlusVLCommandHandler(),
                ViaBackwardsPlatformImpl::new,
                ViaFabricPlusViaLegacyPlatformImpl::new,
                ViaAprilFoolsPlatformImpl::new,
                ViaBedrockPlatformImpl::new
        );

        ProtocolVersion clientVersion = ProtocolVersion.v1_21_4;
        ProtocolVersion serverVersion = ProtocolVersion.v26_3;

        List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
        System.out.println("Path for 1.21.4 -> 26.3: " + path);
        if (path != null) {
            for (ProtocolPathEntry entry : path) {
                System.out.println(" - " + entry.protocol().getClass().getName());
                if (entry.protocol().getMappingData() != null) {
                    System.out.println("    MappingData: " + entry.protocol().getMappingData().getClass().getName());
                    if (entry.protocol().getMappingData().getBlockStateMappings() != null) {
                        System.out.println("    BlockStateMappings size: " + entry.protocol().getMappingData().getBlockStateMappings().size());
                    }
                    if (entry.protocol().getMappingData().getItemMappings() != null) {
                        System.out.println("    ItemMappings size: " + entry.protocol().getMappingData().getItemMappings().size());
                    }
                }
            }

            net.minecraft.SharedConstants.createGameVersion();
            net.minecraft.Bootstrap.initialize();

            System.out.println("Block sugar_cane raw ID: " + net.minecraft.registry.Registries.BLOCK.getRawId(net.minecraft.block.Blocks.SUGAR_CANE));
            System.out.println("Block budding_amethyst raw ID: " + net.minecraft.registry.Registries.BLOCK.getRawId(net.minecraft.block.Blocks.BUDDING_AMETHYST));
            System.out.println("Block wheat raw ID: " + net.minecraft.registry.Registries.BLOCK.getRawId(net.minecraft.block.Blocks.WHEAT));
            System.out.println("Block light_weighted_pressure_plate raw ID: " + net.minecraft.registry.Registries.BLOCK.getRawId(net.minecraft.block.Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE));
            System.out.println("Block oak_log raw ID: " + net.minecraft.registry.Registries.BLOCK.getRawId(net.minecraft.block.Blocks.OAK_LOG));
            System.out.println("Item sugar_cane raw ID: " + net.minecraft.registry.Registries.ITEM.getRawId(net.minecraft.item.Items.SUGAR_CANE));
            System.out.println("Item budding_amethyst raw ID: " + net.minecraft.registry.Registries.ITEM.getRawId(net.minecraft.item.Items.BUDDING_AMETHYST));
            System.out.println("Item wheat_seeds raw ID: " + net.minecraft.registry.Registries.ITEM.getRawId(net.minecraft.item.Items.WHEAT_SEEDS));
            System.out.println("Item light_weighted_pressure_plate raw ID: " + net.minecraft.registry.Registries.ITEM.getRawId(net.minecraft.item.Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
            System.out.println("Item oak_log raw ID: " + net.minecraft.registry.Registries.ITEM.getRawId(net.minecraft.item.Items.OAK_LOG));



            // Now test actual PacketWrapper transformation!
            com.viaversion.viaversion.api.connection.UserConnection dummyConnection = new com.viaversion.viaversion.connection.UserConnectionImpl(com.viaversion.viafabricplus.protocoltranslator.util.NoPacketSendChannel.INSTANCE, true);
            com.viaversion.viaversion.api.protocol.ProtocolPipeline pipeline = new com.viaversion.viaversion.protocol.ProtocolPipelineImpl(dummyConnection);
            for (ProtocolPathEntry pair : path) {
                pipeline.add(pair.protocol());
                pair.protocol().init(dummyConnection);
            }
            dummyConnection.getProtocolInfo().setState(com.viaversion.viaversion.api.protocol.packet.State.PLAY);
            dummyConnection.getProtocolInfo().setProtocolVersion(ProtocolVersion.v1_21_4);
            dummyConnection.getProtocolInfo().setServerProtocolVersion(ProtocolVersion.v26_3);

            // In 26.3, sugar cane is item ID 327.
            // Let's create a CONTAINER_SET_SLOT packet in 26.3 and transform it CLIENTBOUND through dummyConnection pipeline:
            com.viaversion.viaversion.api.protocol.Protocol<?, ?, ?, ?> sourceProtocol = dummyConnection.getProtocolInfo().getPipeline().reversedPipes().stream().filter(p -> !p.isBaseProtocol()).findFirst().orElseThrow();
            System.out.println("Source protocol on pipeline top: " + sourceProtocol.getClass().getName());

            com.viaversion.viaversion.api.minecraft.item.Item item263 = new com.viaversion.viaversion.api.minecraft.item.StructuredItem(327, 1, new com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer());
            com.viaversion.viaversion.api.protocol.packet.PacketWrapper packet = com.viaversion.viaversion.api.protocol.packet.PacketWrapper.create(
                sourceProtocol.getPacketTypesProvider().unmappedClientboundType(com.viaversion.viaversion.api.protocol.packet.State.PLAY, com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1.CONTAINER_SET_SLOT.getName()),
                dummyConnection
            );
            packet.write(com.viaversion.viaversion.api.type.Types.VAR_INT, 0); // container id
            packet.write(com.viaversion.viaversion.api.type.Types.VAR_INT, 0); // state id
            packet.write(com.viaversion.viaversion.api.type.Types.SHORT, (short) 0); // slot
            packet.write(com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator.getClientboundItemType(ProtocolVersion.v26_3), item263);

            packet.resetReader();
            System.out.println("Transforming packet from 26.3 to 1.21.4...");
            try {
                dummyConnection.getProtocolInfo().getPipeline().transform(com.viaversion.viaversion.api.protocol.packet.Direction.CLIENTBOUND, com.viaversion.viaversion.api.protocol.packet.State.PLAY, packet);

                int windowId = packet.read(com.viaversion.viaversion.api.type.Types.VAR_INT);
                int stateId = packet.read(com.viaversion.viaversion.api.type.Types.VAR_INT);
                short slot = packet.read(com.viaversion.viaversion.api.type.Types.SHORT);
                com.viaversion.viaversion.api.minecraft.item.Item outputItem = packet.read(com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator.getClientboundItemType(ProtocolVersion.v1_21_4));

                System.out.println("Output item ID after pipeline: " + outputItem.identifier() + " (expected 252 for sugar_cane)");
                net.minecraft.item.Item finalItem = net.minecraft.registry.Registries.ITEM.get(outputItem.identifier());
                System.out.println("Final resolved Minecraft Item: " + net.minecraft.registry.Registries.ITEM.getId(finalItem));
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Test CONTAINER_SET_CONTENT (inventory update packet)
            com.viaversion.viaversion.api.protocol.packet.PacketWrapper invPacket = com.viaversion.viaversion.api.protocol.packet.PacketWrapper.create(
                sourceProtocol.getPacketTypesProvider().unmappedClientboundType(com.viaversion.viaversion.api.protocol.packet.State.PLAY, com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1.CONTAINER_SET_CONTENT.getName()),
                dummyConnection
            );
            invPacket.write(com.viaversion.viaversion.api.type.Types.VAR_INT, 0); // container id
            invPacket.write(com.viaversion.viaversion.api.type.Types.VAR_INT, 0); // state id
            // List of items in 26.3:
            com.viaversion.viaversion.api.minecraft.item.Item[] items263 = new com.viaversion.viaversion.api.minecraft.item.Item[] {
                new com.viaversion.viaversion.api.minecraft.item.StructuredItem(327, 10, new com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer()), // sugar cane
                new com.viaversion.viaversion.api.minecraft.item.StructuredItem(163, 64, new com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer())  // oak log
            };
            invPacket.write(com.viaversion.viaversion.api.type.Types.VAR_INT, items263.length);
            for (com.viaversion.viaversion.api.minecraft.item.Item it : items263) {
                invPacket.write(com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator.getClientboundItemType(ProtocolVersion.v26_3), it);
            }
            invPacket.write(com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator.getClientboundItemType(ProtocolVersion.v26_3), null); // carried item
            
            invPacket.resetReader();
            try {
                dummyConnection.getProtocolInfo().getPipeline().transform(com.viaversion.viaversion.api.protocol.packet.Direction.CLIENTBOUND, com.viaversion.viaversion.api.protocol.packet.State.PLAY, invPacket);
                
                int invContainerId = invPacket.read(com.viaversion.viaversion.api.type.Types.VAR_INT);
                int invStateId = invPacket.read(com.viaversion.viaversion.api.type.Types.VAR_INT);
                int outCount = invPacket.read(com.viaversion.viaversion.api.type.Types.VAR_INT);
                System.out.println("CONTAINER_SET_CONTENT transformed items count: " + outCount);
                for (int i = 0; i < outCount; i++) {
                    com.viaversion.viaversion.api.minecraft.item.Item it = invPacket.read(com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator.getClientboundItemType(ProtocolVersion.v1_21_4));
                    if (it != null) {
                        System.out.println(" - item id " + it.identifier() + ": " + net.minecraft.registry.Registries.ITEM.getId(net.minecraft.registry.Registries.ITEM.get(it.identifier())));
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception in invPacket transform: " + e);
                e.printStackTrace(System.out);
            }
        }
    }

    private static int readVarInt(java.io.DataInputStream in) throws java.io.IOException {
        int i = 0;
        int j = 0;
        byte b;
        do {
            b = in.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) throw new java.io.IOException("VarInt too big");
        } while ((b & 128) == 128);
        return i;
    }
}

