package me.evisual.arsenic.core.message;

import java.util.Map;

public final class MessageFormatter {
    private MessageFormatter() {
    }

    public static String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = template == null ? "" : template;
        if (placeholders == null || placeholders.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(key, value);
        }
        return result;
    }
}
