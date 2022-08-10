package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

/**
 *  Represents a blob object. Provides helper methods for
 *  saving and loading blobs and retrieving various
 *  information about them.
 *
 *  @author Collin Bowers
 */
public class Blob implements Serializable {

    public static final File BLOBS_DIR = join(Repository.OBJECTS_DIR, "blobs");
    private String name;
    private String contents;
    private String sha1;


    public Blob(String fileName, String contents, String sha1) {
        this.name = fileName;
        this.contents = contents;
        this.sha1 = sha1;
    }

    public String getName() {
        return name;
    }

    public String getSHA1() {
        return sha1;
    }

    public String getContents() {
        return contents;
    }

    public void saveBlob() {
        File blobToBeSaved = new File(BLOBS_DIR, sha1);
        writeObject(blobToBeSaved, this);
    }

    public static Blob loadBlob(String sha1) {
        if (sha1 == null) {
            return null;
        }

        File blobToBeLoaded = new File(BLOBS_DIR, sha1);
        Blob loadedBlob = readObject(blobToBeLoaded, Blob.class);
        return loadedBlob;
    }

    public static Blob loadRemoteBlob(String sha1, File remotePath) {
        File remoteBlobPath = join(remotePath, "objects", "blobs", sha1);
        Blob remoteBlob = readObject(remoteBlobPath, Blob.class);
        return remoteBlob;
    }

    /** Return the given filename without its file extension. */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
        else {
            System.out.println("Please enter file name with a file extension.");
            System.exit(0);
            return null;
        }
    }
}
