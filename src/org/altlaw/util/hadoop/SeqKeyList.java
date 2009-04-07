/* SeqKeyList.java - print list of keys in a SequenceFile
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

/* From hadoop-*-core.jar, http://hadoop.apache.org/
 * Developed with Hadoop 0.16.3. */
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;


/** Prints all keys in a SequenceFile.
 *
 * @author Stuart Sierra (mail@stuartsierra.com)
 */
public class SeqKeyList {

    private String inputFile;
    private LocalSetup setup;

    public SeqKeyList() throws Exception {
        setup = new LocalSetup();
    }

    /** Set the name of the input sequence file.
     *
     * @param filename   a local path string
     */
    public void setInput(String filename) {
        inputFile = filename;
    }

    /** Runs the process. Keys are printed to standard output;
     * information about the sequence file is printed to standard
     * error. */
    public void execute() throws Exception {
        Path path = new Path(inputFile);
        SequenceFile.Reader reader = 
            new SequenceFile.Reader(setup.getLocalFileSystem(), path, setup.getConf());

        try {
            System.err.println("Key type is " + reader.getKeyClassName());
            System.err.println("Value type is " + reader.getValueClassName());
            if (reader.isCompressed()) {
                System.err.println("Values are compressed.");
                if (reader.isBlockCompressed()) {
                    System.err.println("Records are block-compressed.");
                }
                System.err.println("Compression type is " + reader.getCompressionCodec().getClass().getName());
            }
            System.err.println("");

            Writable key = (Writable)(reader.getKeyClass().newInstance());
            while (reader.next(key)) {
                System.out.println(key.toString());
            }
        } finally {
            reader.close();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            exitWithHelp();
        }

        try {
            SeqKeyList me = new SeqKeyList();
            me.setInput(args[0]);
            me.execute();
        } catch (Exception e) {
            e.printStackTrace();
            exitWithHelp();
        }
    }

    /** Prints usage instructions to standard error and exits. */
    public static void exitWithHelp() {
        System.err.println("Usage: java org.altlaw.util.hadoop.SeqKeyList <sequence-file>\n" +
                           "Prints a list of keys in the sequence file, one per line.");
        System.exit(1);
    }
}
