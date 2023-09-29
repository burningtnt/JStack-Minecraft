package net.burningtnt.tdparser.infos;

import net.burningtnt.tdparser.stacktrace.AbstractStackTraceElement;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record ThreadInfo(String threadName, @Nullable Long mainID, boolean isDaemon, @Nullable Integer priority,
                         int osPriority, double cpu, double elapsed, long tid, @Nullable Short nid,
                         @Nullable Long threadID, ThreadStateInfo state,
                         @Nullable List<AbstractStackTraceElement> stackTraceElements) {
    public ThreadInfo(String threadName, @Nullable Long mainID, boolean isDaemon, Integer priority, int osPriority, double cpu, double elapsed, long tid, @Nullable Short nid, @Nullable Long threadID, ThreadStateInfo state, @Nullable List<AbstractStackTraceElement> stackTraceElements) {
        this.threadName = threadName;
        this.mainID = mainID;
        this.isDaemon = isDaemon;
        this.priority = priority;
        this.osPriority = osPriority;
        this.cpu = cpu;
        this.elapsed = elapsed;
        this.tid = tid;
        this.nid = nid;
        this.threadID = threadID;
        this.state = state;
        this.stackTraceElements = stackTraceElements == null ? stackTraceElements : Collections.unmodifiableList(stackTraceElements);
    }
}
