package net.burningtnt.tdparser;

import org.jetbrains.annotations.Nullable;

public class InvalidThreadDumpException extends Exception implements IThreadDumpAnalyzedResult {
    public InvalidThreadDumpException() {
        super();
    }

    public InvalidThreadDumpException(String message) {
        super(message);
    }

    public InvalidThreadDumpException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidThreadDumpException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    @Nullable
    public ThreadDumpReport getResult() {
        return null;
    }

    @Override
    @Nullable
    public InvalidThreadDumpException getException() {
        return this;
    }
}
