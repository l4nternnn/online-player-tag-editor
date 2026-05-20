package com.lantern.onlineplayertageditor.util;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TextUtil {

    public static Text info(String message) {
        return Text.literal(message).formatted(Formatting.GRAY);
    }

    public static Text success(String message) {
        return Text.literal(message).formatted(Formatting.GREEN);
    }

    public static Text error(String message) {
        return Text.literal(message).formatted(Formatting.RED);
    }

    public static Text warning(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    public static Text colored(String message, Formatting color) {
        return Text.literal(message).formatted(color);
    }
}
