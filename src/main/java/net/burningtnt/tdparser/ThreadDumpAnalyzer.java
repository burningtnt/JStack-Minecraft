package net.burningtnt.tdparser;

import net.burningtnt.tdparser.infos.*;
import net.burningtnt.tdparser.results.IThreadDumpAnalyzedResult;
import net.burningtnt.tdparser.results.InvalidThreadDumpException;
import net.burningtnt.tdparser.results.ThreadDumpReport;
import net.burningtnt.tdparser.infos.stacktrace.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ThreadDumpAnalyzer {
    private ThreadDumpAnalyzer() {
    }

    public static ThreadDumpReport analyze(CharSequence report) throws InvalidThreadDumpException {
        List<List<String>> strings = processDump(report);

        if (strings.size() < 3) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the given text didn't contain enough data.");
        }

        DumpInfo dumpInfo = analyzeDumpInfo(strings.get(0));
        SMRInfo smrInfo = analyzeSMRInfo(strings.get(1));
        List<ThreadInfo> threadInfos = new ArrayList<>(smrInfo.length());
        for (int i = 2; i < strings.size() - 1; i++) {
            threadInfos.add(analyzeThreadInfo(strings.get(i)));
        }

        JNIRefInfo refInfo = analyzeJNIRefInfo(strings.get(strings.size() - 1));
        return verifyResult(new ThreadDumpReport(dumpInfo, smrInfo, threadInfos, refInfo));
    }

    public static IThreadDumpAnalyzedResult safeAnalyze(CharSequence report) {
        try {
            return analyze(report);
        } catch (InvalidThreadDumpException e) {
            return e;
        }
    }

    private static List<List<String>> processDump(CharSequence report) {
        List<List<String>> groups = new ArrayList<>();

        List<String> currentGroup = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < report.length(); i++) {
            char c = report.charAt(i);
            if (c == '\n') {
                if (currentLine.isEmpty()) {
                    if (!currentGroup.isEmpty()) {
                        groups.add(currentGroup);
                        currentGroup = new ArrayList<>();
                    }
                } else {
                    currentGroup.add(currentLine.toString());
                }
                currentLine.setLength(0);
            } else {
                currentLine.append(c);
            }
        }

        return Collections.unmodifiableList(groups);
    }

    private static final SimpleDateFormat TIME_STAMP_LINE_PATTERN = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static DumpInfo analyzeDumpInfo(List<String> headGroup) throws InvalidThreadDumpException {
        if (headGroup.size() != 2) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the head group is invalid.");
        }
        String timeStampLine = headGroup.get(0);
        String vmInfoLine = headGroup.get(1);

        Date date;
        try {
            date = TIME_STAMP_LINE_PATTERN.parse(timeStampLine);
        } catch (ParseException e) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the time stamp is invalid.", e);
        }

        if (!vmInfoLine.startsWith("Full thread dump ") || !vmInfoLine.endsWith(":")) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the VM name is invalid.");
        }
        String vmInfo = vmInfoLine.substring("Full thread dump ".length(), vmInfoLine.length() - 1);

        return new DumpInfo(date, vmInfo);
    }

    private static final Pattern SMR_PATTERN = Pattern.compile("^_java_thread_list=0x([\\da-f]{16}), length=(\\d+), elements=\\{$");

    private static final Pattern SMR_LINE_PATTERN = Pattern.compile("^0x([\\da-f]{16})(?:, 0x([\\da-f]{16})(?:, 0x([\\da-f]{16})(?:, 0x([\\da-f]{16}))?)?)?(,?)$");

    private static SMRInfo analyzeSMRInfo(List<String> smr) throws InvalidThreadDumpException {
        if (smr.size() < 3) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
        }
        if (!smr.get(0).equals("Threads class SMR info:")) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
        }
        if (!smr.get(smr.size() - 1).equals("}")) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
        }
        Matcher matcher = SMR_PATTERN.matcher(smr.get(1));
        if (!matcher.find()) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
        }
        long threadList = Long.parseLong(matcher.group(1), 16);
        int threadLength = Integer.parseInt(matcher.group(2), 10);
        Set<Long> elements = new HashSet<>(threadLength);
        for (int i = 0; i < threadLength / 4; i++) {
            Matcher lineMatcher = SMR_LINE_PATTERN.matcher(smr.get(i + 2));
            if (!lineMatcher.find()) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
            }
            for (int j = 1; j < 5; j++) {
                String value = lineMatcher.group(j);
                if (value == null) {
                    throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
                }
                elements.add(Long.parseLong(value, 16));
            }
            if (!",".equals(lineMatcher.group(5))) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
            }
        }
        int rest = threadLength % 4;
        if (rest != 0) {
            Matcher lineMatcher = SMR_LINE_PATTERN.matcher(smr.get(smr.size() - 2));
            if (!lineMatcher.find()) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
            }
            for (int i = 1; i < rest + 1; i++) {
                String value = lineMatcher.group(i);
                if (value == null) {
                    throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
                }
                elements.add(Long.parseLong(value, 16));
            }
            if (",".equals(lineMatcher.group(5))) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the SMR info is invalid.");
            }
        }
        return new SMRInfo(threadList, threadLength, elements);
    }

    private static final Pattern THREAD_INFO_HEAD_PATTERN = Pattern.compile("\"([^\"]*)\" (?:#(\\d+) )?(daemon )?(?:prio=(\\d+) )?os_prio=(-?\\d+) cpu=(\\d+\\.\\d+)ms elapsed=(\\d+\\.\\d+)s tid=0x([\\da-f]{16}) nid=0x([\\da-f]{0,4}) (?:runnable|waiting on condition|in Object\\.wait\\(\\)) {2}(?:\\[0x([\\da-f]{16})])?");

    private static final Pattern THREAD_STATE_PATTERN = Pattern.compile("^ {3}java\\.lang\\.Thread\\.State: (RUNNABLE|WAITING \\(on object monitor\\)|TIMED_WAITING \\(on object monitor\\)|WAITING \\(parking\\)|TIMED_WAITING \\(parking\\))$");

    private static final Pattern THREAD_STACK_PATTERN = Pattern.compile("\\t(?:at ((?:[^.\\n]+\\.)*[^.\\n]+)\\(([^)\\n\\r]*)\\)|- waiting on <([^>\\n]*)>|- locked <0x([\\da-f]{16})> \\((.*)\\)|- parking to wait for {2}<0x([\\da-f]{16})> \\((.*)\\))");

    private static ThreadInfo analyzeThreadInfo(List<String> threadinfos) throws InvalidThreadDumpException {
        if (threadinfos.size() == 0) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread info is invalid.");
        }
        Matcher infoMatcher = THREAD_INFO_HEAD_PATTERN.matcher(threadinfos.get(0));
        if (!infoMatcher.find()) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread info is invalid.");
        }
        String threadName = infoMatcher.group(1);
        Long mainID = Optional.ofNullable(infoMatcher.group(2)).map(Long::parseLong).orElse(null);
        boolean isDaemon = "daemon ".equals(infoMatcher.group(3));
        Integer priority = Optional.ofNullable(infoMatcher.group(4)).map(Integer::parseInt).orElse(null);
        int osPriority = Integer.parseInt(infoMatcher.group(5));
        double cpu = Double.parseDouble(infoMatcher.group(6));
        double elapsed = Double.parseDouble(infoMatcher.group(7));
        long tid = Long.parseLong(infoMatcher.group(8), 16);
        Short nid = Optional.ofNullable(infoMatcher.group(9)).map(s -> Short.parseShort(s, 16)).orElse(null);
        Long threadID = Optional.ofNullable(infoMatcher.group(10)).map(s -> Long.parseLong(s, 16)).orElse(null);

        ThreadStateInfo stateInfo;
        if (threadinfos.size() == 1) {
            stateInfo = ThreadStateInfo.UNKNOWN_NATIVE;
        } else {
            Matcher stateMatcher = THREAD_STATE_PATTERN.matcher(threadinfos.get(1));
            if (!stateMatcher.find()) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread state is invalid.");
            }
            stateInfo = ThreadStateInfo.getStates().get(stateMatcher.group(1));
            if (stateInfo == null) {
                throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread state is invalid.");
            }
        }

        List<AbstractStackTraceElement> stackTraceElements;
        if (threadID == null || threadID == 0L) {
            stackTraceElements = null;
        } else {
            stackTraceElements = new ArrayList<>(threadinfos.size() - 2);
            for (int i = 2; i < threadinfos.size(); i++) {
                Matcher stackMatcher = THREAD_STACK_PATTERN.matcher(threadinfos.get(i));
                if (!stackMatcher.find()) {
                    throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread trace stack is invalid.");
                }
                if (stackMatcher.group(1) != null) {
                    stackTraceElements.add(new FunctionStackTraceElement(List.of(stackMatcher.group(1).split("\\.")), stackMatcher.group(2)));
                } else if (stackMatcher.group(3) != null) {
                    stackTraceElements.add(new WaitingStackTraceElement(stackMatcher.group(3)));
                } else if (stackMatcher.group(4) != null) {
                    stackTraceElements.add(new LockedStackTraceElement(Long.parseLong(stackMatcher.group(4), 16), stackMatcher.group(5)));
                } else if (stackMatcher.group(6) != null) {
                    stackTraceElements.add(new ParkingStackTraceElement(Long.parseLong(stackMatcher.group(6), 16), stackMatcher.group(7)));
                } else {
                    throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the thread trace stack is invalid.");
                }
            }
        }

        return new ThreadInfo(threadName, mainID, isDaemon, priority, osPriority, cpu, elapsed, tid, nid, threadID, stateInfo, stackTraceElements);
    }


    private static final Pattern JNI_REF_PATTERN = Pattern.compile("^JNI global refs: (\\d+), weak refs: (\\d+)$");

    private static JNIRefInfo analyzeJNIRefInfo(List<String> ref) throws InvalidThreadDumpException {
        if (ref.size() != 1) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the JNI ref info is invalid.");
        }
        Matcher matcher = JNI_REF_PATTERN.matcher(ref.get(0));
        if (!matcher.find()) {
            throw new InvalidThreadDumpException("Cannot analyze the specific thread dump because the JNI ref info is invalid.");
        }
        int globalRefs = Integer.parseInt(matcher.group(1), 10);
        int weakRefs = Integer.parseInt(matcher.group(2), 10);
        return new JNIRefInfo(globalRefs, weakRefs);
    }

    private static ThreadDumpReport verifyResult(ThreadDumpReport result) throws InvalidThreadDumpException {
        Set<Long> threadIDs = result.threadInfos().stream().mapToLong(ThreadInfo::tid).filter(l -> l != 0L).boxed().collect(Collectors.toSet());
        for (Long value : result.smrInfo().elements()) {
            if (value == null || value.doubleValue() == 0L) {
                continue;
            }

            if (!threadIDs.contains(value)) {
                throw new InvalidThreadDumpException(String.format("Cannot analyze the specific thread dump because the threadID '0x%s' which is specified in SMR info didn't exist.", HexFormat.of().toHexDigits(value)));
            }
        }

        return result;
    }
}
