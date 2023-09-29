package net.burningtnt.tdparser.stacktrace;

public class LockedStackTraceElement extends AbstractStackTraceElement {
    private final long position;

    private final String source;

    public LockedStackTraceElement(long position, String source) {
        this.position = position;
        this.source = source;
    }

    public long getPosition() {
        return this.position;
    }

    public String getSource() {
        return this.source;
    }
}
