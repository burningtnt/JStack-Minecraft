package net.burningtnt.tdparser.stacktrace;

public class WaitingStackTraceElement extends AbstractStackTraceElement {
    private final String target;

    public WaitingStackTraceElement(String target) {
        this.target = target;
    }

    public String getTarget() {
        return this.target;
    }
}
