package net.burningtnt.tdparser;

import org.jetbrains.annotations.Nullable;

public interface IThreadDumpAnalyzedResult {
    boolean success();

    @Nullable
    ThreadDumpReport getResult();

    @Nullable
    InvalidThreadDumpException getException();
}
