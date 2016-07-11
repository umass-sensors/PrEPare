package edu.umass.cs.prepare.storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous file writer implementation.
 * @see <a href="http://stackoverflow.com/questions/6206472/what-is-the-best-way-to-write-to-a-file-in-a-parallel-thread-in-java">AsyncFileWriter</a>
 */
class AsyncFileWriter implements FileWriter, Runnable {

    /** The file writer. **/
    private final Writer out;

    /** The queue of strings written to the file. **/
    private final BlockingQueue<CharSequence> queue = new LinkedBlockingQueue<>();

    /** Indicates {@link #open()} has been called. **/
    private volatile boolean started = false;

    /** Indicates {@link #close()} has been called. **/
    private volatile boolean stopped = false;

    public AsyncFileWriter(File file) throws IOException {
        this.out = new BufferedWriter(new java.io.FileWriter(file));
    }

    /**
     * Appends the specified character sequence to the file
     * @param seq the character sequence to be appended to the file
     * @return the file writer object
     */
    public FileWriter append(CharSequence seq) {
        if (!started) {
            throw new IllegalStateException("open() call expected before append()");
        }
        try {
            queue.put(seq);
        } catch (InterruptedException ignored) {
        }
        return this;
    }

    /**
     * Starts writing incoming data to disk on a separate thread.
     */
    public void open() {
        this.started = true;
        new Thread(this).start();
    }

    public void run() {
        while (!stopped) {
            try {
                CharSequence item = queue.poll(100, TimeUnit.MICROSECONDS);
                if (item != null) {
                    try {
                        out.append(item);
                    } catch (IOException ignore) {}
                }
            } catch (InterruptedException ignore) {}
        }
        try {
            out.close();
        } catch (IOException ignore) {}
    }

    /**
     * Closes the file writer.
     */
    public void close() {
        this.stopped = true;
    }
}