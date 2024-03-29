package net.burningtnt.jstackmc;

import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JVMDescriptorAnalyzer {
    private JVMDescriptorAnalyzer() {
    }

    private static final String[] MINECRAFT_PACKAGES = {
            "net.fabricmc",
            "net.minecraftforge",
            "net.minecraft",
            "cpw.mods",
            "org.jackhuang.hmcl",
            "oolloo.jlw.Wrapper",
            "com.intellij.idea"
    };

    private static final String CURRENT_JVM = String.valueOf(ProcessHandle.current().pid());

    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile("java_command: (([a-zA-Z][a-zA-Z0-9_]*\\.)+[a-zA-Z][a-zA-Z0-9_]*)");

    public static boolean isMinecraftJVM(JVMCommandExecutor jvmCommandExecutor) throws IOException {
        Matcher m = MAIN_CLASS_PATTERN.matcher(jvmCommandExecutor.executeCommandAsCharSequence("VM.command_line"));
        if (m.find()) {
            String mainClass = m.group(1);

            for (String prefix : MINECRAFT_PACKAGES) {
                if (mainClass.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isCurrentJVM(VirtualMachineDescriptor descriptor) {
        return descriptor.id().equals(CURRENT_JVM);
    }
}
