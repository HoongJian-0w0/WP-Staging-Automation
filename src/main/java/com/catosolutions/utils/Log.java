package com.catosolutions.utils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {

    private static final StringBuilder sessionLog = new StringBuilder();
    private static JTextArea currentLogArea;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static Thread logThread;
    private static final PrintStream originalOut = System.out;
    private static final PrintStream originalErr = System.err;

    private static final boolean debug = false;

    /**
     * Redirects System.out and System.err to in-memory log + UI (JTextArea if bound).
     */
    public static void redirectOutputTo() {
        // Interrupt old log thread if running
        if (!debug) {
            if (logThread != null && logThread.isAlive()) {
                logThread.interrupt();
            }

            try {
                PipedOutputStream pos = new PipedOutputStream();
                PipedInputStream pis = new PipedInputStream(pos);
                BufferedReader reader = new BufferedReader(new InputStreamReader(pis, StandardCharsets.UTF_8));

                logThread = new Thread(() -> {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            appendToLog(line);
                        }
                    } catch (IOException e) {
                        // appendToLog("ℹ️ Log stream closed after automation ended.");
                    }
                }, "LogRedirectThread");
                logThread.setDaemon(true);
                logThread.start();

                PrintStream redirected = new PrintStream(pos, true, StandardCharsets.UTF_8.name());
                System.setOut(redirected);
                System.setErr(redirected);

            } catch (IOException e) {
                originalErr.println("❌ Failed to redirect log output: " + e.getMessage());
            }
        }
    }

    /**
     * Binds the log to a live JTextArea for UI viewing.
     */
    public static void bindTextArea(JTextArea textArea) {
        currentLogArea = textArea;
        currentLogArea.setText(sessionLog.toString());
    }

    /**
     * Appends a new log line (timestamped) to the session and UI.
     */
    private static void appendToLog(String line) {
        line = line.trim();
        if (line.isEmpty()) return;

        String timestamped = "[" + LocalTime.now().format(timeFormatter) + "] " + line + "\n";
        sessionLog.append(timestamped);

        if (currentLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                currentLogArea.append(timestamped);
                currentLogArea.setCaretPosition(currentLogArea.getDocument().getLength());
            });
        }
    }

    /**
     * Clears the in-memory session log.
     */
    public static void clearSession() {
        sessionLog.setLength(0);
        if (currentLogArea != null) {
            currentLogArea.setText("");
        }
    }

    /**
     * Restores original console output (used after redirect).
     */
    public static void restoreOriginalOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.out.println("🌀 Output restored to original streams.");
    }
}
