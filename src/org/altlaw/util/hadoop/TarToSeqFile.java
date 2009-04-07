/* TarToSeqFile.java - Convert tar files into Hadoop SequenceFiles.
 *
 * Copyright (C) 2008 Stuart Sierra
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * http:www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.altlaw.util.hadoop;

/* From ant.jar, http://ant.apache.org/ */
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/* From hadoop-*-core.jar, http://hadoop.apache.org/
 * Developed with Hadoop 0.16.3. */
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;


/** Utility to convert tar files into Hadoop SequenceFiles.  The tar
 * files may be compressed with GZip or BZip2.  The output
 * SequenceFile will be stored with BLOCK compression.  Each key (a
 * Text) in the SequenceFile is the name of the file in the tar
 * archive, and its value (a BytesWritable) is the contents of the
 * file.
 *
 * <p>This class can be run at the command line; run without
 * arguments to get usage instructions.
 *
 * @author Stuart Sierra (mail@stuartsierra.com)
 * @see <a href="http://hadoop.apache.org/core/docs/r0.16.3/api/org/apache/hadoop/io/SequenceFile.html">SequenceFile</a>
 * @see <a href="http://hadoop.apache.org/core/docs/r0.16.3/api/org/apache/hadoop/io/Text.html">Text</a>
 * @see <a href="http://hadoop.apache.org/core/docs/r0.16.3/api/org/apache/hadoop/io/BytesWritable.html">BytesWritable</a>
 */
public class TarToSeqFile {

    private File inputFile;
    private File outputFile;
    private LocalSetup setup;

    /** Sets up Configuration and LocalFileSystem instances for
     * Hadoop.  Throws Exception if they fail.  Does not load any
     * Hadoop XML configuration files, just sets the minimum
     * configuration necessary to use the local file system.
     */
    public TarToSeqFile() throws Exception {
        setup = new LocalSetup();
    }

    /** Sets the input tar file. */
    public void setInput(File inputFile) {
        this.inputFile = inputFile;
    }

    /** Sets the output SequenceFile. */
    public void setOutput(File outputFile) {
        this.outputFile = outputFile;
    }

    /** Performs the conversion. */
    public void execute() throws Exception {
        TarInputStream input = null;
        SequenceFile.Writer output = null;
        try {
            input = openInputFile();
            output = openOutputFile();
            TarEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory()) { continue; }
                String filename = entry.getName();
                byte[] data = TarToSeqFile.getBytes(input, entry.getSize());
                
                Text key = new Text(filename);
                BytesWritable value = new BytesWritable(data);
                output.append(key, value);
            }
        } finally {
            if (input != null) { input.close(); }
            if (output != null) { output.close(); }
        }
    }

    private TarInputStream openInputFile() throws Exception {
        InputStream fileStream = new FileInputStream(inputFile);
        String name = inputFile.getName();
        InputStream theStream = null;
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            theStream = new GZIPInputStream(fileStream);
        } else if (name.endsWith(".tar.bz2") || name.endsWith(".tbz2")) {
            /* Skip the "BZ" header added by bzip2. */
            fileStream.skip(2);
            theStream = new CBZip2InputStream(fileStream);
        } else {
            /* Assume uncompressed tar file. */
            theStream = fileStream;
        }
        return new TarInputStream(theStream);
    }

    private SequenceFile.Writer openOutputFile() throws Exception {
        Path outputPath = new Path(outputFile.getAbsolutePath());
        return SequenceFile.createWriter(setup.getLocalFileSystem(), setup.getConf(),
                                         outputPath,
                                         Text.class, BytesWritable.class,
                                         SequenceFile.CompressionType.BLOCK);
    }

    /** Reads all bytes from the current entry in the tar file and
     * returns them as a byte array.
     *
     * @see http://www.exampledepot.com/egs/java.io/File2ByteArray.html
     */
    private static byte[] getBytes(TarInputStream input, long size) throws Exception {
        if (size > Integer.MAX_VALUE) {
            throw new Exception("A file in the tar archive is too large.");
        }
        int length = (int)size;
        byte[] bytes = new byte[length];

        int offset = 0;
        int numRead = 0;

        while (offset < bytes.length &&
               (numRead = input.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("A file in the tar archive could not be completely read.");
        }

        return bytes;
    }

    /** Runs the converter at the command line. */
    public static void main(String[] args) {
        if (args.length != 2) {
            exitWithHelp();
        }

        try {
            TarToSeqFile me = new TarToSeqFile();
            me.setInput(new File(args[0]));
            me.setOutput(new File(args[1]));
            me.execute();
        } catch (Exception e) {
            e.printStackTrace();
            exitWithHelp();
        }
    }

    public static void exitWithHelp() {
        System.err.println("Usage: java org.altlaw.util.hadoop.TarToSeqFile <tarfile> <output>\n\n" +
                           "<tarfile> may be GZIP or BZIP2 compressed, must have a\n" +
                           "recognizable extension .tar, .tar.gz, .tgz, .tar.bz2, or .tbz2.");
        System.exit(1);
    }
}
