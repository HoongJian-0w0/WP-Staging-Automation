package com.catosolutions.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class WordPressBackupFinder {

    /**
     * Finds the latest .wpress backup file for the given domain in the specified folder.
     *
     * @param backupFolderPath Full path to the folder containing .wpress files
     * @param domainName       Domain string like "www.xxx.sg"
     * @return Full path to the latest matched .wpress file, or null if not found
     */
    public static String findLatestBackupForDomain(String backupFolderPath, String domainName) {
        if (backupFolderPath == null || domainName == null) return null;

        File folder = new File(backupFolderPath);
        if (!folder.exists() || !folder.isDirectory()) return null;

        String domainKey = extractDomainKey(domainName);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".wpress") && name.contains(domainKey));

        if (files == null || files.length == 0) return null;

        File latest = null;
        Date latestDate = null;

        for (File file : files) {
            String fileName = file.getName();
            String[] parts = fileName.split("-");

            // Look for date part (8-digit number like 20250325)
            for (String part : parts) {
                if (part.matches("\\d{8}")) {
                    try {
                        Date date = new SimpleDateFormat("yyyyMMdd").parse(part);
                        if (latestDate == null || date.after(latestDate)) {
                            latestDate = date;
                            latest = file;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return (latest != null) ? latest.getAbsolutePath() : null;
    }

    /**
     * Extracts the domain key (e.g. "extremefitness-sg" from www.extremefitness.sg)
     */
    private static String extractDomainKey(String domainName) {
        String clean = domainName.toLowerCase().replace("www.", "").split("\\.")[0];
        return clean.replace("_", "-"); // just in case
    }
}
