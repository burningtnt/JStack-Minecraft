package net.burningtnt.tdparser.infos;

import java.util.Map;

public enum ThreadStateInfo {
    RUNNABLE, WAITING_ON_OBJECT_MONITOR, TIME_WAITING_ON_OBJECT_MONITOR, WAITING_PARKING, TIMED_WAITING_PARKING, UNKNOWN_NATIVE;

    private static final Map<String, ThreadStateInfo> states = Map.of(
            "RUNNABLE", RUNNABLE,
            "WAITING (on object monitor)", WAITING_ON_OBJECT_MONITOR,
            "TIMED_WAITING (on object monitor)", TIME_WAITING_ON_OBJECT_MONITOR,
            "WAITING (parking)", WAITING_PARKING,
            "TIMED_WAITING (parking)", TIMED_WAITING_PARKING
    );

    public static Map<String, ThreadStateInfo> getStates() {
        return states;
    }
}
