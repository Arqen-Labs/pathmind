package com.pathmind.execution;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;

/**
 * Centralizes access to Baritone so that we can gracefully handle missing or
 * misconfigured dependencies without crashing the client. When Baritone fails
 * to load (for example when the Nether pathfinder addon is absent) we record a
 * user-friendly error message that callers can surface to players.
 */
public final class BaritoneAccess {

    private static volatile String lastFailureMessage;
    private static volatile String lastFailureSignature;
    private static volatile Boolean mineSupportAvailable;
    private static volatile String mineSupportFailureMessage;
    private static final Object MINE_SUPPORT_LOCK = new Object();
    private static final String NETHER_PATHFINDER_CLASS = "dev.babbaj.pathfinder.NetherPathfinder";

    private BaritoneAccess() {
    }

    /**
     * Attempts to fetch the primary Baritone instance. Any {@link Throwable}
     * thrown during Baritone bootstrap is caught so Pathmind can continue
     * running and report a clear error back to the user instead of crashing
     * the game client.
     *
     * @return the primary Baritone instance, or {@code null} if Baritone could
     *         not be created
     */
    public static IBaritone tryGetBaritone() {
        try {
            return BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Throwable throwable) {
            recordFailure(throwable);
            return null;
        }
    }

    /**
     * Returns {@code true} when all of the dependencies required for Baritone's
     * mining features are present on the classpath. When dependencies are
     * missing we cache a descriptive message that callers can surface to the
     * player.
     */
    public static boolean isMineSupportAvailable() {
        Boolean cached = mineSupportAvailable;
        if (cached != null) {
            return cached;
        }

        synchronized (MINE_SUPPORT_LOCK) {
            if (mineSupportAvailable != null) {
                return mineSupportAvailable;
            }

            try {
                Class.forName(NETHER_PATHFINDER_CLASS, false, BaritoneAccess.class.getClassLoader());
                mineSupportFailureMessage = null;
                mineSupportAvailable = Boolean.TRUE;
            } catch (ClassNotFoundException classNotFoundException) {
                mineSupportAvailable = Boolean.FALSE;
                mineSupportFailureMessage = "Baritone cannot start a mine task because the Nether Pathfinder addon is missing. "
                        + "Install the dev.babbaj.pathfinder.NetherPathfinder dependency to use Mine nodes.";
            } catch (Throwable throwable) {
                mineSupportAvailable = Boolean.FALSE;
                String detail = throwable.getMessage();
                mineSupportFailureMessage = "Baritone cannot start a mine task (" + throwable.getClass().getSimpleName()
                        + (detail != null ? ": " + detail : "") + ")";
            }

            return mineSupportAvailable;
        }
    }

    /**
     * Provides the last cached failure explanation for missing Baritone mining
     * dependencies, or {@code null} when mining is supported.
     */
    public static String getMineSupportFailureMessage() {
        return mineSupportFailureMessage;
    }

    /**
     * Returns the most recent user-facing failure message for Baritone
     * initialization, or {@code null} if Baritone has not failed recently.
     */
    public static String getLastFailureMessage() {
        return lastFailureMessage;
    }

    private static void recordFailure(Throwable throwable) {
        String signature = buildSignature(throwable);
        if (signature != null && !signature.equals(lastFailureSignature)) {
            System.err.println("Pathmind: Failed to obtain Baritone instance: " + signature);
            throwable.printStackTrace();
            lastFailureSignature = signature;
        }
        lastFailureMessage = buildUserMessage(throwable);

        if (throwable instanceof NoClassDefFoundError) {
            String message = throwable.getMessage();
            if (message != null && message.contains("dev/babbaj/pathfinder/NetherPathfinder")) {
                synchronized (MINE_SUPPORT_LOCK) {
                    mineSupportAvailable = Boolean.FALSE;
                    mineSupportFailureMessage = "Baritone cannot start a mine task because the Nether Pathfinder addon is missing. "
                            + "Install the dev.babbaj.pathfinder.NetherPathfinder dependency to use Mine nodes.";
                }
            }
        }
    }

    private static String buildSignature(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        return throwable.getClass().getName() + ':' + (message != null ? message : "");
    }

    private static String buildUserMessage(Throwable throwable) {
        if (throwable instanceof NoClassDefFoundError) {
            String message = throwable.getMessage();
            if (message != null && message.contains("dev/babbaj/pathfinder/NetherPathfinder")) {
                return "Baritone cannot start because dev.babbaj.pathfinder.NetherPathfinder is missing. "
                        + "Install the Baritone Nether Pathfinder addon to use Mine nodes.";
            }
        }
        String simpleName = throwable != null ? throwable.getClass().getSimpleName() : "UnknownError";
        String detail = throwable != null && throwable.getMessage() != null ? (": " + throwable.getMessage()) : "";
        return "Baritone is unavailable (" + simpleName + detail + ")";
    }
}

