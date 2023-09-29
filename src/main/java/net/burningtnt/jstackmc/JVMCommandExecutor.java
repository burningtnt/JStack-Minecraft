package net.burningtnt.jstackmc;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.AttachOperationFailedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

public abstract class JVMCommandExecutor implements AutoCloseable {
    protected final VirtualMachine virtualMachine;

    protected JVMCommandExecutor(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    public abstract InputStream executeCommandAsInputStream(String command) throws IOException;

    public abstract CharSequence executeCommandAsCharSequence(String command) throws IOException;

    public abstract String executeCommandAsString(String command) throws IOException;

    public final void close() throws IOException {
        this.virtualMachine.detach();
    }

    private static VirtualMachine attach(VirtualMachineDescriptor descriptor, int retryTime) throws IOException {
        Throwable[] errors = new Throwable[retryTime];
        for (int i = 0; i < retryTime; i++) {
            try {
                return descriptor.provider().attachVirtualMachine(descriptor);
            } catch (IOException | AttachNotSupportedException e) {
                errors[i] = e;
            }
        }

        IOException exception = new IOException(String.format("Cannot attach VM %s.", descriptor.id()));
        for (Throwable t : errors) {
            exception.addSuppressed(t);
        }

        throw exception;
    }

    public static JVMCommandExecutor of(VirtualMachineDescriptor descriptor, int retryTime) throws IOException {
        VirtualMachine virtualMachine = attach(descriptor, retryTime);
        try {
            if (virtualMachine instanceof sun.tools.attach.HotSpotVirtualMachine) {
                return new JVMCommandExecutor(virtualMachine) {
                    @Override
                    public InputStream executeCommandAsInputStream(String command) throws IOException {
                        return ((sun.tools.attach.HotSpotVirtualMachine) this.virtualMachine).executeJCmd(command);
                    }

                    @Override
                    public CharSequence executeCommandAsCharSequence(String command) throws IOException {
                        StringBuilder stringBuilder = new StringBuilder(1024);
                        try (InputStreamReader reader = new InputStreamReader(this.executeCommandAsInputStream(command))) {
                            CharBuffer buffer = CharBuffer.allocate(1024);

                            while (reader.read(buffer) > 0) {
                                buffer.flip();
                                stringBuilder.append(buffer);
                                buffer.clear();
                            }
                        }
                        return stringBuilder;
                    }

                    @Override
                    public String executeCommandAsString(String command) throws IOException {
                        return this.executeCommandAsCharSequence(command).toString();
                    }
                };
            } else {
                throw new AttachOperationFailedException(String.format("Unsupported implementation %s of com.sun.tools.attach.VirtualMachine.", virtualMachine.getClass().getName()));
            }
        } catch (Throwable t) {
            try {
                virtualMachine.detach();
            } catch (IOException ignored) {
            }
            throw t;
        }
    }
}
