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

package com.viaversion.viafabricplus.protocoltranslator.util;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages asynchronous pre-warming of ViaVersion protocol translation pipelines and mapping data.
 */
public final class ProtocolWarmupManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "ViaFabricPlus-Warmup");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private static final Set<ProtocolVersion> WARMED_VERSIONS = ConcurrentHashMap.newKeySet();
    private static volatile boolean startupPreloadDone = false;

    /**
     * Asynchronously pre-warms the protocol path for the given target version.
     */
    public static void warmupAsync(final ProtocolVersion targetVersion) {
        if (targetVersion == null || targetVersion == ProtocolTranslator.NATIVE_VERSION || targetVersion == ProtocolTranslator.AUTO_DETECT_PROTOCOL) {
            return;
        }

        final int mode = GeneralSettings.INSTANCE.serverWarmupMode.getIndex();
        if (mode == 3) { // Disabled
            return;
        }

        if (!WARMED_VERSIONS.add(targetVersion)) {
            return; // Already warmed or currently warming
        }

        EXECUTOR.execute(() -> warmupDirect(targetVersion));
    }

    /**
     * Directly executes protocol mapping pre-warming on the caller thread (or executor).
     */
    public static void warmupDirect(final ProtocolVersion targetVersion) {
        try {
            if (Via.getManager() == null || Via.getManager().getProtocolManager() == null) {
                return;
            }

            final ProtocolVersion clientVersion = ProtocolTranslator.NATIVE_VERSION;
            final List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(clientVersion, targetVersion);
            if (path != null && !path.isEmpty()) {
                for (final ProtocolPathEntry entry : path) {
                    final Protocol<?, ?, ?, ?> protocol = entry.protocol();
                    if (protocol.getMappingData() != null) {
                        try {
                            protocol.getMappingData().load();
                        } catch (final Throwable ignored) {
                        }
                    }
                }
                ViaFabricPlusImpl.INSTANCE.logger().info("Pre-warmed protocol path: {} -> {} ({} intermediate protocols)", clientVersion.getName(), targetVersion.getName(), path.size());
            }
        } catch (final Throwable t) {
            ViaFabricPlusImpl.INSTANCE.logger().warn("Failed to pre-warm protocol " + targetVersion.getName() + ": " + t.getMessage());
        }
    }

    /**
     * Triggered during game initialization or opening Multiplayer screen when Startup Preload (Mode 1) is active.
     */
    public static void onClientOrMultiplayerInit() {
        final int mode = GeneralSettings.INSTANCE.serverWarmupMode.getIndex();
        if (mode == 1 && !startupPreloadDone) { // Startup Preload
            startupPreloadDone = true;
            EXECUTOR.execute(() -> {
                try {
                    // Preload modern subversion chain up to 1.21.11 / 26.3
                    warmupDirect(ProtocolVersion.v1_21_11);
                    // Preload popular major legacy versions
                    warmupDirect(ProtocolVersion.v1_20);
                    warmupDirect(ProtocolVersion.v1_16_4);
                    warmupDirect(ProtocolVersion.v1_12_2);
                    warmupDirect(ProtocolVersion.v1_8);
                } catch (final Throwable t) {
                    ViaFabricPlusImpl.INSTANCE.logger().warn("Error during startup protocol preloading: " + t.getMessage());
                }
            });
        }
    }

    private ProtocolWarmupManager() {
    }
}
