package net.burningtnt.jstackmc;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftVMScanner {
    private MinecraftVMScanner() {
    }

    private static final String[] minecraftPackageNameList = {
            "net.fabricmc",
            "net.minecraftforge",
            "net.minecraft",
            "cpw.mods",
            "org.jackhuang.hmcl",
            "com.intellij.idea"
    };

    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile("java_command: (([a-zA-Z][a-zA-Z0-9_]*\\.)+[a-zA-Z][a-zA-Z0-9_]*)");

    public static int getMinecraftVM() throws IOException {
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String res = Attach.attachVM(descriptor, "VM.command_line");
            Matcher m = MAIN_CLASS_PATTERN.matcher(res);
            if (m.find()) {
                String mainClass = m.group(1);

                for (String prefix : minecraftPackageNameList) {
                    if (mainClass.startsWith(prefix)) {
                        return Integer.parseInt(descriptor.id());
                    }
                }
            }
        }

        return -1;
    }
}
