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

