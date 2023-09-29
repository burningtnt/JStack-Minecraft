package net.burningtnt.tdparser.results;

import net.burningtnt.tdparser.infos.JNIRefInfo;
import net.burningtnt.tdparser.infos.SMRInfo;
import net.burningtnt.tdparser.infos.ThreadInfo;
import net.burningtnt.tdparser.infos.DumpInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ThreadDumpReport implements IThreadDumpAnalyzedResult {
    private final DumpInfo dumpInfo;
    private final SMRInfo smrInfo;
    private final List<ThreadInfo> threadInfos;
    private final JNIRefInfo refInfo;
    private final Map<String, ThreadInfo> nameLookup;
    private final Map<Long, ThreadInfo> tidLookup;

    public ThreadDumpReport(DumpInfo dumpInfo, SMRInfo smrInfo, List<ThreadInfo> threadInfos, JNIRefInfo refInfo) {
        this.dumpInfo = dumpInfo;
        this.smrInfo = smrInfo;
        this.threadInfos = Collections.unmodifiableList(threadInfos);
        this.refInfo = refInfo;

        Map<String, ThreadInfo> nameLookup = new HashMap<>();
        Map<Long, ThreadInfo> tidLookup = new HashMap<>();
        for (ThreadInfo ti : threadInfos) {
            nameLookup.putIfAbsent(ti.threadName(), ti);
            tidLookup.putIfAbsent(ti.tid(), ti);
        }
        this.nameLookup = nameLookup;
        this.tidLookup = tidLookup;
    }

    @Override
    public boolean success() {
        return true;
    }

    @Override
    @NotNull
    public ThreadDumpReport getResult() {
        return this;
    }

    @Override
    @Nullable
    public InvalidThreadDumpException getException() {
        return null;
    }

    public ThreadInfo lookupThread(String name) {
        return this.nameLookup.get(name);
    }

    public ThreadInfo lookupThread(long tid) {
        return this.tidLookup.get(tid);
    }

    public DumpInfo dumpInfo() {
        return dumpInfo;
    }

    public SMRInfo smrInfo() {
        return smrInfo;
    }

    public List<ThreadInfo> threadInfos() {
        return threadInfos;
    }

    public JNIRefInfo refInfo() {
        return refInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ThreadDumpReport) obj;
        return Objects.equals(this.dumpInfo, that.dumpInfo) &&
                Objects.equals(this.smrInfo, that.smrInfo) &&
                Objects.equals(this.threadInfos, that.threadInfos) &&
                Objects.equals(this.refInfo, that.refInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dumpInfo, smrInfo, threadInfos, refInfo);
    }

    @Override
    public String toString() {
        return "ThreadDumpReport[" +
                "dumpInfo=" + dumpInfo + ", " +
                "smrInfo=" + smrInfo + ", " +
                "threadInfos=" + threadInfos + ", " +
                "refInfo=" + refInfo + ']';
    }

}
