package edu.umass.cs.prepare.storage;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * This class handles file input/output operations, such as saving the accelerometer/gyroscope
 * data and labels, opening/closing file readers/writers, and deleting the data storage location.
 */
class FileUtil {

    /** tag used for debugging purposes */
    private static final String TAG = FileUtil.class.getName();

    /** CSV extension */
    private static final String CSV_EXTENSION = ".csv";

    /**
     * Returns a file writer for a device
     * @param filename file name (without extension!)
     * @return the file writer for the particular filename
     */
    public static AsyncFileWriter getFileWriter(Context context, String filename, File directory){
        if(!directory.exists()) {
            if (directory.mkdirs()){
                Toast.makeText(context, String.format("Created directory %s", directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, String.format("Failed to create directory %s. Please set the directory in Settings",
                        directory.getAbsolutePath()), Toast.LENGTH_LONG).show();
            }
        }
        String fullFileName = filename + String.valueOf(System.currentTimeMillis()) + CSV_EXTENSION;

        AsyncFileWriter out = null;
        try{
            out = new AsyncFileWriter(new File(directory,fullFileName));
        }catch(IOException e){
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Deletes all the data from the given directory (be careful!!)
     * @return true if successfully deleted
     */
    public static boolean deleteData(File directory){
        boolean deleted = false;
        if(directory!=null){
            File files[] = directory.listFiles();
            if(files!=null){
                for(File file : files) {
                    if (!file.delete())
                        Log.d(TAG, "Deleting file failed: " + file.getName());
                }
            }
            deleted = directory.delete();
        }
        return deleted;
    }
}
