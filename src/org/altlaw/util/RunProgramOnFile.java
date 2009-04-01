package org.altlaw.util;

import java.io.File;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;


/** Function to run an external program on an input blob, first
 * writing the input to a temporary file. */
public class RunProgramOnFile {
    private static final byte versionID = 1;

    private File executable;

    /** Creates the function.  You must also call setExecutable. */
    public RunProgramOnFile() {
    }

    /** Sets the executable this function will invoke. */
    public void setExecutable(File executable) {
        this.executable = executable;
    }

    /** Writes the input to a temporary file, and then runs the
     * executable with that file as its argument. */
    public byte[] exec(byte[] input) throws Exception {
        File tmpFile = File.createTempFile("run_on_file", null);
        FileUtils.writeByteArrayToFile(tmpFile, input);

        CommandLine line = new CommandLine(executable);
        line.addArgument(tmpFile.getAbsolutePath());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(os));
        int exitValue = executor.execute(line);

        tmpFile.delete();

        return os.toByteArray();
    }

    /** Main function for testing at command line. */
    public static final void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("\nUsage: java RunProgramOnFile <executable> <file>\n" +
                               "Run the executable on a file.  If file is -, use STDIN.\n");
            System.exit(1);
        }

        try {
            RunProgramOnFile me = new RunProgramOnFile();
            me.setExecutable(new File(args[0]));

            byte[] data;
            if (args[1].equals("-")) {
                data = IOUtils.toByteArray(System.in);
            } else {
                data = FileUtils.readFileToByteArray(new File(args[1]));
            }

            System.out.println(new String(me.exec(data)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
