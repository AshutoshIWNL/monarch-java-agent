package com.asm.mja.utils;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class BannerUtils {
    public static String getBanner(String title) {
        title = title.toUpperCase();
        int length = title.length();
        StringBuilder banner = new StringBuilder();
        banner.append("  ");
        for (int i = 0; i < length + 2; i++) {
            banner.append("_");
        }
        banner.append("\n");
        banner.append(" |");
        for (int i = 0; i < length + 2; i++) {
            banner.append(" ");
        }
        banner.append("|\n");
        banner.append(" | ").append(title).append(" |\n");
        banner.append(" |");
        for (int i = 0; i < length + 2; i++) {
            banner.append("_");
        }
        banner.append("|\n");

        return banner.toString();
    }
}
