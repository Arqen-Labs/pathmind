package com.pathmind.execution;

import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.process.IMineProcess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

/**
 * Watches the player's inventory during mining runs that target a specific quantity
 * and stops Baritone once the requested amount has been gathered.
 */
public final class MineQuantityMonitor {
    private static final MineQuantityMonitor INSTANCE = new MineQuantityMonitor();

    private boolean active;
    private Item targetItem;
    private int targetCount;
    private IBaritone baritone;
    private IMineProcess mineProcess;
    private boolean mineProcessStarted;
    private String blockDescription;

    private MineQuantityMonitor() {
        this.active = false;
    }

    public static MineQuantityMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Begins monitoring the active mine process for the requested quantity.
     */
    public void begin(MinecraftClient client, IBaritone baritone, IMineProcess mineProcess, Item targetItem, int targetCount, String blockDescription) {
        cancel();

        if (client == null || baritone == null || mineProcess == null || targetItem == null || targetCount <= 0) {
            return;
        }

        this.baritone = baritone;
        this.mineProcess = mineProcess;
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.blockDescription = blockDescription;
        this.mineProcessStarted = mineProcess.isActive();
        this.active = true;
        System.out.println("MineQuantityMonitor: tracking " + blockDescription + " up to " + targetCount + " items");
    }

    /**
     * Processes a client tick and performs any required inventory checks.
     */
    public void tick(MinecraftClient client) {
        if (!active) {
            return;
        }

        try {
            if (client == null || client.player == null || targetItem == null) {
                cancel();
                return;
            }

            if (mineProcess == null) {
                cancel();
                return;
            }

            if (mineProcess.isActive()) {
                mineProcessStarted = true;
            } else if (mineProcessStarted) {
                cancel();
                return;
            }

            int currentCount = client.player.getInventory().count(targetItem);
            if (currentCount >= targetCount) {
                System.out.println("MineQuantityMonitor: target met for " + blockDescription + " (" + currentCount + "/" + targetCount + ")");
                stopMining();
                cancel();
            }
        } catch (Throwable t) {
            System.err.println("MineQuantityMonitor: Error while monitoring inventory: " + t.getMessage());
            t.printStackTrace();
            cancel();
        }
    }

    /**
     * Stops monitoring immediately without issuing commands.
     */
    public void cancel() {
        if (!active && baritone == null && mineProcess == null && targetItem == null) {
            return;
        }

        this.active = false;
        this.baritone = null;
        this.mineProcess = null;
        this.targetItem = null;
        this.targetCount = 0;
        this.blockDescription = null;
        this.mineProcessStarted = false;
    }

    private void stopMining() {
        if (baritone == null) {
            return;
        }

        final IBaritone baritoneRef = baritone;
        IMineProcess activeProcess = mineProcess != null ? mineProcess : baritoneRef.getMineProcess();
        if (activeProcess != null && activeProcess.isActive()) {
            try {
                activeProcess.cancel();
            } catch (Exception e) {
                System.err.println("MineQuantityMonitor: Failed to cancel active mine process: " + e.getMessage());
            }
        }

        try {
            IPathingBehavior pathingBehavior = baritoneRef.getPathingBehavior();
            if (pathingBehavior != null) {
                pathingBehavior.cancelEverything();
            }
        } catch (Exception e) {
            System.err.println("MineQuantityMonitor: Failed to cancel Baritone pathing: " + e.getMessage());
        }
    }
}
