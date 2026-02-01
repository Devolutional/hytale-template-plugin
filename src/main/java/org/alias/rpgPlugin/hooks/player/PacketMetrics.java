package org.alias.rpgPlugin.hooks.player;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks packet counts and sizes over rolling time windows (1, 5, 10 minutes)
 * Thread-safe implementation using concurrent data structures
 */
public class PacketMetrics {

    // Time window constants (in milliseconds)
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long FIVE_MINUTES_MS = 300_000L;
    private static final long TEN_MINUTES_MS = 600_000L;

    // Store packet records with timestamps
    private final ConcurrentLinkedDeque<PacketRecord> packetHistory;

    // Running totals for quick access
    private final AtomicLong totalPackets;
    private final AtomicLong totalBytes;

    // Last cleanup time
    private volatile long lastCleanupTime;

    /**
     * Individual packet record with timestamp and size
     */
    private static class PacketRecord {
        final long timestamp;
        final long bytes;

        PacketRecord(long timestamp, long bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
    }

    /**
     * Metrics snapshot for a specific time window
     */
    public static class MetricsSnapshot {
        public final long packetCount;
        public final long totalBytes;
        public final double averagePacketSize;
        public final double packetsPerSecond;
        public final double bytesPerSecond;
        public final long periodMs;

        MetricsSnapshot(long packetCount, long totalBytes, long periodMs) {
            this.packetCount = packetCount;
            this.totalBytes = totalBytes;
            this.periodMs = periodMs;
            this.averagePacketSize = packetCount > 0 ? (double) totalBytes / packetCount : 0.0;

            double periodSeconds = periodMs / 1000.0;
            this.packetsPerSecond = periodSeconds > 0 ? packetCount / periodSeconds : 0.0;
            this.bytesPerSecond = periodSeconds > 0 ? totalBytes / periodSeconds : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "Packets: %,d | Bytes: %,d (%s) | Avg Size: %.1f bytes | Rate: %.2f pkt/s, %s/s",
                    packetCount,
                    totalBytes,
                    formatBytes(totalBytes),
                    averagePacketSize,
                    packetsPerSecond,
                    formatBytes((long) bytesPerSecond)
            );
        }

        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Complete metrics for all time windows
     */
    public static class AllMetrics {
        public final MetricsSnapshot oneMinute;
        public final MetricsSnapshot fiveMinutes;
        public final MetricsSnapshot tenMinutes;
        public final MetricsSnapshot lifetime;

        AllMetrics(MetricsSnapshot oneMinute, MetricsSnapshot fiveMinutes,
                   MetricsSnapshot tenMinutes, MetricsSnapshot lifetime) {
            this.oneMinute = oneMinute;
            this.fiveMinutes = fiveMinutes;
            this.tenMinutes = tenMinutes;
            this.lifetime = lifetime;
        }

        @Override
        public String toString() {
            return String.format(
                    "=== Packet Metrics ===\n" +
                            "  1 Minute:  %s\n" +
                            "  5 Minutes: %s\n" +
                            " 10 Minutes: %s\n" +
                            " Lifetime:   %s",
                    oneMinute,
                    fiveMinutes,
                    tenMinutes,
                    lifetime
            );
        }
    }

    public PacketMetrics() {
        this.packetHistory = new ConcurrentLinkedDeque<>();
        this.totalPackets = new AtomicLong(0);
        this.totalBytes = new AtomicLong(0);
        this.lastCleanupTime = System.currentTimeMillis();
    }

    /**
     * Record a packet transmission
     * @param bytes Size of the packet in bytes
     */
    public void recordPacket(long bytes) {
        long now = System.currentTimeMillis();

        // Add new record
        packetHistory.add(new PacketRecord(now, bytes));

        // Update totals
        totalPackets.incrementAndGet();
        totalBytes.addAndGet(bytes);

        // Periodic cleanup (every 30 seconds)
        if (now - lastCleanupTime > 30_000) {
            cleanup(now);
            lastCleanupTime = now;
        }
    }

    /**
     * Remove records older than 10 minutes
     */
    private void cleanup(long currentTime) {
        long cutoffTime = currentTime - TEN_MINUTES_MS;

        // Remove old records from the front of the deque
        while (!packetHistory.isEmpty()) {
            PacketRecord oldest = packetHistory.peekFirst();
            if (oldest != null && oldest.timestamp < cutoffTime) {
                packetHistory.pollFirst();
            } else {
                break;
            }
        }
    }

    /**
     * Calculate metrics for a specific time window
     */
    private MetricsSnapshot calculateMetrics(long windowMs) {
        long now = System.currentTimeMillis();
        long cutoffTime = now - windowMs;

        long count = 0;
        long bytes = 0;

        // Iterate through records in reverse (newest first)
        for (PacketRecord record : packetHistory) {
            if (record.timestamp >= cutoffTime) {
                count++;
                bytes += record.bytes;
            }
        }

        return new MetricsSnapshot(count, bytes, windowMs);
    }

    /**
     * Get metrics for the 1-minute window
     */
    public MetricsSnapshot getOneMinuteMetrics() {
        return calculateMetrics(ONE_MINUTE_MS);
    }

    /**
     * Get metrics for the 5-minute window
     */
    public MetricsSnapshot getFiveMinuteMetrics() {
        return calculateMetrics(FIVE_MINUTES_MS);
    }

    /**
     * Get metrics for the 10-minute window
     */
    public MetricsSnapshot getTenMinuteMetrics() {
        return calculateMetrics(TEN_MINUTES_MS);
    }

    /**
     * Get lifetime metrics (all time)
     */
    public MetricsSnapshot getLifetimeMetrics() {
        long now = System.currentTimeMillis();
        long oldestTime = packetHistory.isEmpty() ? now : packetHistory.peekFirst().timestamp;
        long period = now - oldestTime;

        return new MetricsSnapshot(
                totalPackets.get(),
                totalBytes.get(),
                Math.max(period, 1000) // Minimum 1 second to avoid division issues
        );
    }

    /**
     * Get all metrics at once (more efficient than calling individually)
     */
    public AllMetrics getAllMetrics() {
        long now = System.currentTimeMillis();

        long oneMinCutoff = now - ONE_MINUTE_MS;
        long fiveMinCutoff = now - FIVE_MINUTES_MS;
        long tenMinCutoff = now - TEN_MINUTES_MS;

        long oneMinCount = 0, oneMinBytes = 0;
        long fiveMinCount = 0, fiveMinBytes = 0;
        long tenMinCount = 0, tenMinBytes = 0;

        // Single pass through all records
        for (PacketRecord record : packetHistory) {
            if (record.timestamp >= oneMinCutoff) {
                oneMinCount++;
                oneMinBytes += record.bytes;
            }
            if (record.timestamp >= fiveMinCutoff) {
                fiveMinCount++;
                fiveMinBytes += record.bytes;
            }
            if (record.timestamp >= tenMinCutoff) {
                tenMinCount++;
                tenMinBytes += record.bytes;
            }
        }

        MetricsSnapshot oneMin = new MetricsSnapshot(oneMinCount, oneMinBytes, ONE_MINUTE_MS);
        MetricsSnapshot fiveMin = new MetricsSnapshot(fiveMinCount, fiveMinBytes, FIVE_MINUTES_MS);
        MetricsSnapshot tenMin = new MetricsSnapshot(tenMinCount, tenMinBytes, TEN_MINUTES_MS);
        MetricsSnapshot lifetime = getLifetimeMetrics();

        return new AllMetrics(oneMin, fiveMin, tenMin, lifetime);
    }

    /**
     * Reset all metrics
     */
    public void reset() {
        packetHistory.clear();
        totalPackets.set(0);
        totalBytes.set(0);
        lastCleanupTime = System.currentTimeMillis();
    }

    /**
     * Get current queue size (for debugging)
     */
    public int getHistorySize() {
        return packetHistory.size();
    }
}