package org.dev.utility.dev_utility.utils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationUtil {
    
  public static void serialize(Object appData, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(appData);
            System.out.println(appData.getClass()+" has been serialized to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
  }
  
  public static Object deserialize(String filename) {
    Object appData = null;
    try (FileInputStream fileIn = new FileInputStream(filename);
         ObjectInputStream in = new ObjectInputStream(fileIn)) {
      appData = in.readObject();
        System.out.println(appData.getClass()+" has been deserialized from " + filename);
    } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
    }
    return appData;
}

}
