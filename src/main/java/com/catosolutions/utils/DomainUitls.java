package com.catosolutions.utils;

import java.util.ArrayList;
import java.util.List;

public class DomainUitls {

    public static List<String> splitDomainName(boolean applyMmPrefix, String text) {
        List<String> extractedDomains = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return extractedDomains;

        String[] domains = text.split("[,\r?\n]+");
        for (String raw : domains) {
            raw = raw.trim();
            if (raw.isEmpty()) continue;
            String cleaned = raw.replace("https://", "").replace("http://", "").replace("www.", "");
            String domain;
            if (!cleaned.contains(".")) domain = cleaned;
            else {
                String[] parts = cleaned.split("\\.");
                String[] knownSuffixes = {"com.sg", "com.my", "co.uk", "org.sg", "net.cn"};
                String lastTwoParts = parts[parts.length - 2] + "." + parts[parts.length - 1];
                boolean isKnownSuffix = false;
                for (String suffix : knownSuffixes) {
                    if (lastTwoParts.equalsIgnoreCase(suffix)) {
                        isKnownSuffix = true;
                        break;
                    }
                }
                if (isKnownSuffix && parts.length >= 3) domain = parts[parts.length - 3];
                else domain = parts[parts.length - 2];
            }
            if (applyMmPrefix) domain = "mm-" + domain;
            extractedDomains.add(domain);
        }
        return extractedDomains;
    }

    /**
     * Ensures the given cPanel URL ends with a slash.
     * e.g. https://example.com:2083 → https://example.com:2083/
     */
    public static String normalizeCpanelUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";

        url = url.trim();

        // Ensure it starts with https://
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        // Ensure port 2083 is included
        if (!url.contains(":2083")) {
            url = url.replaceFirst("https://", "https://") + ":2083";
        }

        // Ensure trailing slash
        if (!url.endsWith("/")) {
            url += "/";
        }

        return url;
    }

}
