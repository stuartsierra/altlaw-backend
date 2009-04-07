/* LocalSetup.java -- support for the Hadoop API outside of Hadoop
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

/** Provides Hadoop configuration and local file system objects for
 * other classes.  This is for situations where you want to use some
 * part of the Hadoop code outside of the Hadoop Map/Reduce
 * framework.
 *
 * @author Stuart Sierra (mail@stuartsierra.com)
 */
public class LocalSetup {

    private FileSystem fileSystem;
    private Configuration config;

    /** Sets up Configuration and LocalFileSystem instances for
     * Hadoop.  Throws Exception if they fail.  Does not load any
     * Hadoop XML configuration files, just sets the minimum
     * configuration necessary to use the local file system.
     */
    public LocalSetup() throws Exception {
        config = new Configuration();

        /* Normally set in hadoop-default.xml, without it you get
         * "java.io.IOException: No FileSystem for scheme: file" */
        config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

        fileSystem = FileSystem.get(config);
        if (fileSystem.getConf() == null) {
            /* This happens if the FileSystem is not properly
             * initialized, causes NullPointerException later. */
            throw new Exception("LocalFileSystem configuration is null");
        }
    }

    /** Returns a Hadoop Configuration instance for use in Hadoop API
     * calls. */
    public Configuration getConf() {
        return config;
    }

    /** Returns a Hadoop FileSystem instance that provides access to
     * the local filesystem. */
    public FileSystem getLocalFileSystem() {
        return fileSystem;
    }
}