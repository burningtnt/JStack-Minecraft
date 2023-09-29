package net.burningtnt.tdparser.infos.stacktrace;

import java.util.List;

public final class FunctionStackTraceElement extends AbstractStackTraceElement {
    private final List<String> path;

    private final String source;

    public FunctionStackTraceElement(List<String> path, String source) {
        this.path = path;
        this.source = source;
    }

    public List<String> getPath() {
        return this.path;
    }

    public String getSource() {
        return this.source;
    }
}
