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

package com.viaversion.viafabricplus.injection.mixin.features.networking.packet_handling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import net.minecraft.entity.data.DataTracked;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DataTracker.class)
public abstract class MixinDataTracker {

    @Shadow
    @Final
    private DataTracked trackedEntity;

    @WrapOperation(method = "writeUpdatedEntries", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;copyToFrom(Lnet/minecraft/entity/data/DataTracker$Entry;Lnet/minecraft/entity/data/DataTracker$SerializedEntry;)V"))
    private <T> void safeCopyToFrom(DataTracker instance, DataTracker.Entry<T> to, DataTracker.SerializedEntry<?> from, Operation<Void> original) {
        try {
            original.call(instance, to, from);
        } catch (IllegalStateException e) {
            ViaFabricPlusImpl.INSTANCE.logger().warn("Ignoring invalid entity data item type for field {} on {}: {}", from.id(), this.trackedEntity, e.getMessage());
        }
    }

}
