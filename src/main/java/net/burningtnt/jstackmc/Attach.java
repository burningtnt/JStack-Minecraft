package net.burningtnt.jstackmc;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.CharBuffer;

public final class Attach {
    private Attach() {
    }

    @FunctionalInterface
    public interface VirtualMachineProvider {
        @NotNull
        VirtualMachine provide() throws AttachNotSupportedException, IOException;
    }

    public static String attachVM(VirtualMachineDescriptor descriptor, String command) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(descriptor, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(VirtualMachineDescriptor descriptor, String command, Appendable appendable) throws IOException {
        attachVM(() -> descriptor.provider().attachVirtualMachine(descriptor.id()), command, appendable);
    }

    public static String attachVM(long lvmid, String command) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(lvmid, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(long lvmid, String command, Appendable appendable) throws IOException {
        attachVM(() -> VirtualMachine.attach(String.valueOf(lvmid)), command, appendable);
    }

    public static String attachVM(VirtualMachineProvider virtualMachineProvider, String command) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        attachVM(virtualMachineProvider, command, stringBuilder);
        return stringBuilder.toString();
    }

    public static void attachVM(VirtualMachineProvider virtualMachineProvider, String command, Appendable appendable) throws IOException {
        @Nullable String vmID = null;
        try {
            VirtualMachine vm = virtualMachineProvider.provide();
            vmID = vm.id();

            try (InputStreamReader inputStreamReader = new InputStreamReader(new BufferedInputStream(((sun.tools.attach.HotSpotVirtualMachine) vm).executeJCmd(command)))) {
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
            Logger.error(String.format("An Exception happened while attaching vm %s", vmID == null ? "UNKNOWN" : vmID), throwable);
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
        }
    }
}
