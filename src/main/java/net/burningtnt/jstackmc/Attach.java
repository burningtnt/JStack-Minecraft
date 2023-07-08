package net.burningtnt.jstackmc;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

public final class Attach {
    private Attach() {
    }

    @FunctionalInterface
    public interface VirtualMachineProvider {
        @NotNull
        VirtualMachine provide() throws AttachNotSupportedException, IOException;
    }

    public static String attachVM(VirtualMachineDescriptor descriptor, String command) {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(descriptor, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(VirtualMachineDescriptor descriptor, String command, Appendable appendable) {
        attachVM(() -> descriptor.provider().attachVirtualMachine(descriptor.id()), command, appendable);
    }

    public static String attachVM(long lvmid, String command) {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(lvmid, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(long lvmid, String command, Appendable appendable) {
        attachVM(() -> VirtualMachine.attach(String.valueOf(lvmid)), command, appendable);
    }

    public static String attachVM(VirtualMachineProvider virtualMachineProvider, String command) {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(virtualMachineProvider, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(VirtualMachineProvider virtualMachineProvider, String command, Appendable appendable) {
        try {
            VirtualMachine vm = virtualMachineProvider.provide();

            try (InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(((sun.tools.attach.HotSpotVirtualMachine) vm).executeJCmd(command)), StandardCharsets.UTF_8)) {
                char[] dataCache = new char[256];
                int status;

                do {
                    status = inputStreamReader.read(dataCache);

                    if (status > 0) {
                        appendable.append(CharBuffer.wrap(dataCache, 0, status));
                    }
                } while (status > 0);
            } finally {
                vm.detach();
            }
        } catch (Throwable throwable) {
            Logger.error("An Exception happened while attaching vm", throwable);
            try {
                appendable.append('\n');
                throwable.printStackTrace(new PrintWriter(new Writer() {
                    @Override
                    public void write(char @NotNull [] cbuf, int off, int len) throws IOException {
                        appendable.append(CharBuffer.wrap(cbuf, off, len));
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public void close() {
                    }
                }));
                appendable.append('\n');
            } catch (IOException e) {
                Logger.error("An IOException happened while writing exception which happened while attaching vm", e);
            }
        }
    }
}
