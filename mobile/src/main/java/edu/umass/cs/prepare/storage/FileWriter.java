package edu.umass.cs.prepare.storage;

public interface FileWriter {
    FileWriter append(CharSequence seq);
    void close();
}