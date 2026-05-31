package com.fantasynations.validation;

import java.util.List;

/**
 * The nine formations the game accepts. A formation is described by the
 * number of players per outfield position; GK is always 1. The total must
 * always be 11.
 *
 *   GK-DEF-MID-FWD
 *
 *   1-3-4-3, 1-3-5-2, 1-3-6-1,
 *   1-4-3-3, 1-4-4-2, 1-4-5-1,
 *   1-5-2-3, 1-5-3-2, 1-5-4-1
 */
public record Formation(int def, int mid, int fwd) {

    public static final int GK = 1;
    public static final int LINEUP_SIZE = 11;

    public static final List<Formation> ALL = List.of(
            new Formation(3, 4, 3),
            new Formation(3, 5, 2),
            new Formation(3, 6, 1),
            new Formation(4, 3, 3),
            new Formation(4, 4, 2),
            new Formation(4, 5, 1),
            new Formation(5, 2, 3),
            new Formation(5, 3, 2),
            new Formation(5, 4, 1)
    );

    public String code() {
        return GK + "-" + def + "-" + mid + "-" + fwd;
    }

    public boolean matches(int gkCount, int defCount, int midCount, int fwdCount) {
        return gkCount == GK && defCount == def && midCount == mid && fwdCount == fwd;
    }

    public static boolean isValid(int gk, int def, int mid, int fwd) {
        if (gk + def + mid + fwd != LINEUP_SIZE) return false;
        if (gk != GK) return false;
        for (Formation f : ALL) {
            if (f.matches(gk, def, mid, fwd)) return true;
        }
        return false;
    }
}
