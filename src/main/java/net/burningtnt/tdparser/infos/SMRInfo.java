package net.burningtnt.tdparser.infos;

import java.util.Collections;
import java.util.Set;

public record SMRInfo(long threadList, int length, Set<Long> elements) {
    public SMRInfo(long threadList, int length, Set<Long> elements) {
        this.threadList = threadList;
        this.length = length;
        this.elements = Collections.unmodifiableSet(elements);
    }
}
