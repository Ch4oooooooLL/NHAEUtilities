package com.github.nhaeutilities.modules.patternrouting.core;

public final class HatchRoutingCandidate {

    public final Object hatch;
    public final HatchAssignmentData assignment;
    public final boolean hasPatterns;
    public final boolean full;

    public HatchRoutingCandidate(Object hatch, HatchAssignmentData assignment, boolean hasPatterns, boolean full) {
        this.hatch = hatch;
        this.assignment = assignment != null ? assignment : HatchAssignmentData.EMPTY;
        this.hasPatterns = hasPatterns;
        this.full = full;
    }

    public static HatchRoutingCandidate empty(HatchAssignmentData assignment) {
        return new HatchRoutingCandidate(null, assignment, false, false);
    }

    public static HatchRoutingCandidate withPatterns(HatchAssignmentData assignment) {
        return new HatchRoutingCandidate(null, assignment, true, false);
    }

    public static HatchRoutingCandidate full(HatchAssignmentData assignment) {
        return new HatchRoutingCandidate(null, assignment, false, true);
    }
}
