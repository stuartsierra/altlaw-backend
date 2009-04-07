package org.altlaw.util.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class FileWritable implements WritableComparable {
    private BytesWritable bytes;
    private Text name;

    public FileWritable() {
        this.name = new Text();
        this.bytes = new BytesWritable();
    }

    public FileWritable(Text name, BytesWritable bytes) {
        this.name = name;
        this.bytes = bytes;
    }

    public Text getName() {
        return this.name;
    }

    public BytesWritable getBytes() {
        return this.bytes;
    }

    public void setName(Text name) {
        this.name = name;
    }

    public void setBytes(BytesWritable bytes) {
        this.bytes = bytes;
    }

    public void readFields(DataInput in) throws IOException {
        name.readFields(in);
        bytes.readFields(in);
    }
  
    public void write(DataOutput out) throws IOException {
        name.write(out);
        bytes.write(out);
    }
  
    public int hashCode() {
        return bytes.hashCode();
    }

    public int compareTo(Object that) {
        return bytes.compareTo(that);
    }

    public boolean equals(Object that) {
        if (that instanceof FileWritable) {
            return this.compareTo(that) == 0;
        }
        return false;
    }

    public String toString() {
        String nameStr = name.toString();
        String contentStr = new String(bytes.get(), 0, 20);
        StringBuffer sb = new StringBuffer(nameStr.length() + 20);
        sb.append(nameStr);
        sb.append(' ');
        sb.append(contentStr);
        return sb.toString();
    }
}