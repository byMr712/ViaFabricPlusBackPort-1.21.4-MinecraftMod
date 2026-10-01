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

package com.viaversion.viafabricplus.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Optional;

public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static Text makeHighContrast(final Text text) {
        if (text == null) {
            return Text.empty();
        }

        final MutableText result = Text.empty();
        text.visit((style, asString) -> {
            if (asString.isEmpty()) {
                return Optional.empty();
            }

            // Strip embedded legacy formatting codes that might force black font (§0, §8, etc.)
            String cleanText = asString;
            if (cleanText.contains("§")) {
                cleanText = Formatting.strip(cleanText);
            }

            Style newStyle = style;
            final TextColor color = style.getColor();
            if (color != null) {
                final int rgb = color.getRgb();
                final int r = (rgb >> 16) & 0xFF;
                final int g = (rgb >> 8) & 0xFF;
                final int b = rgb & 0xFF;
                final double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                if (luminance < 70) {
                    newStyle = style.withColor(Formatting.WHITE);
                }
            } else {
                newStyle = style.withColor(Formatting.WHITE);
            }

            result.append(Text.literal(cleanText).setStyle(newStyle));
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

}
