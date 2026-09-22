package io.github.fishgames.vectrum.core.diagnosis;

import io.github.fishgames.vectrum.core.routing.ResourceFilter;

import java.util.ArrayList;
import java.util.List;

/** Derives the flow reasons of a port side from facts. */
public final class Diagnosis {
    private Diagnosis() {
    }

    /**
     * Reasons for a source side.
     * <ul>
     * <li>1. no network: {@link FlowReason#NO_NETWORK}</li>
     * <li>2. no target: no target, no receiver or dimension lock</li>
     * <li>3. empty source without a recent transfer: {@link FlowReason#SOURCE_EMPTY}</li>
     * <li>4. open targets: flowing, limit reached, or idle</li>
     * <li>5. no open target: full, filtered, unloaded, missing (each present kind)</li>
     * </ul>
     */
    public static List<FlowReason> forSource(SourceFacts facts) {
        List<FlowReason> reasons = new ArrayList<>();
        if (!facts.networkPresent()) {
            reasons.add(FlowReason.NO_NETWORK);
            return reasons;
        }
        if (facts.total() == 0) {
            if (facts.wireless() && facts.locked() > 0) {
                reasons.add(FlowReason.DIMENSION_LOCKED);
            } else {
                reasons.add(facts.wireless() ? FlowReason.NO_RECEIVER : FlowReason.NO_TARGET);
            }
            return reasons;
        }
        if (facts.sourceEmpty() && facts.lastMoved() <= 0) {
            reasons.add(FlowReason.SOURCE_EMPTY);
            return reasons;
        }
        if (facts.open() > 0) {
            if (facts.lastMoved() > 0) {
                reasons.add(FlowReason.FLOWING);
                if (facts.lastBudget() > 0 && facts.lastMoved() >= facts.lastBudget()) {
                    reasons.add(FlowReason.LIMIT_REACHED);
                }
            } else {
                reasons.add(FlowReason.IDLE);
            }
        } else {
            if (facts.full() > 0) {
                reasons.add(FlowReason.TARGET_FULL);
            }
            if (facts.filtered() > 0) {
                reasons.add(FlowReason.FILTER_BLOCKS_ALL);
            }
            if (facts.unloaded() > 0) {
                reasons.add(FlowReason.TARGET_UNLOADED);
            }
            if (facts.missing() > 0 && reasons.isEmpty()) {
                reasons.add(FlowReason.NO_TARGET);
            }
        }
        if (facts.open() > 0 && facts.unloaded() > 0) {
            reasons.add(FlowReason.TARGET_UNLOADED);
        }
        if (facts.wireless() && facts.locked() > 0) {
            reasons.add(FlowReason.DIMENSION_LOCKED);
        }
        return reasons;
    }

    /** Reasons for a target side. */
    public static List<FlowReason> forTarget(TargetFacts facts) {
        List<FlowReason> reasons = new ArrayList<>();
        if (!facts.networkPresent()) {
            reasons.add(FlowReason.NO_NETWORK);
        } else if (facts.sources() == 0) {
            reasons.add(FlowReason.NO_SOURCE);
        } else if (facts.ownFull()) {
            reasons.add(FlowReason.TARGET_FULL);
        } else if (facts.receivedRecent()) {
            reasons.add(FlowReason.FLOWING);
        } else {
            reasons.add(FlowReason.IDLE);
        }
        return reasons;
    }

    /**
     * Whether two filters together pass nothing.
     * <ul>
     * <li>whitelist and whitelist: no common id</li>
     * <li>whitelist and blacklist: every listed id of the whitelist is blacklisted</li>
     * <li>blacklist and blacklist, or an empty filter: never</li>
     * </ul>
     */
    public static boolean passNothing(ResourceFilter a, ResourceFilter b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (!a.blacklist() && !b.blacklist()) {
            return a.ids().stream().noneMatch(b.ids()::contains);
        }
        if (!a.blacklist()) {
            return b.ids().containsAll(a.ids());
        }
        if (!b.blacklist()) {
            return a.ids().containsAll(b.ids());
        }
        return false;
    }
}
