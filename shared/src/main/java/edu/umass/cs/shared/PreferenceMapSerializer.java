package edu.umass.cs.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 */
public class PreferenceMapSerializer {

    public static byte[] serialize(Map<String, ?> preferenceMap) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(preferenceMap);
        return byteOut.toByteArray();

    }

    public static Map<String, ?> deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        Map<String, ?> deserializedPreferenceMap = (Map<String, ?>) in.readObject();
        return deserializedPreferenceMap;
    }
}
