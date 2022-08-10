package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.Utils.*;

/**
 *  Represents a commit object. Provides a variety of helper methods
 *  for working with commits, saving commits, retrieving commits, and
 *  updating commit information.
 *
 *  @author Collin Bowers
 */
public class Commit implements Serializable {

    public static final File COMMITS_DIR = join(Repository.OBJECTS_DIR, "commits");
    private static final long serialVersionUID = -7360817244070965409L;
    private String message;
    private String date;
    private String parentSHA1;
    private String otherParentSHA1;
    private String currSHA1;
    private HashMap<String, String> trackedFiles;

    public Commit(String message, String date, String parentSHA1, String otherParentSHA1) {
        this.message = message;
        this.date = date;
        this.parentSHA1 = parentSHA1;
        setOtherParentSHA1(otherParentSHA1);
        this.currSHA1 = null;
        this.trackedFiles = new HashMap<>();
    }

    public void setOtherParentSHA1(String otherParentSHA1) {
        if (otherParentSHA1 != null) {
            this.otherParentSHA1 = otherParentSHA1;
        }
        else {
            this.otherParentSHA1 = null;
        }
    }

    public String getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public String getParentSHA1() {
        return parentSHA1;
    }

    public String getOtherParentSHA1() {
        return otherParentSHA1;
    }

    public String getCurrSHA1() {
        return currSHA1;
    }

    public HashMap<String, String> getTrackedFiles() {
        return trackedFiles;
    }

    public String saveCommit() {
        byte[] serializedCommit = serialize(this);
        currSHA1 = sha1(serializedCommit);

        File commitToBeSaved = new File(COMMITS_DIR, currSHA1);
        writeObject(commitToBeSaved, this);

        return currSHA1;
    }

    public static Commit loadCommit(String sha1) {
        if (sha1 == null || !(new File(COMMITS_DIR, sha1)).exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        File commitToBeLoaded = new File(COMMITS_DIR, sha1);
        Commit loadedCommit = readObject(commitToBeLoaded, Commit.class);
        return loadedCommit;
    }

    public static Commit loadRemoteCommit(String sha1, File remotePath) {
        File remoteCommitPath = join(remotePath, "objects", "commits", sha1);
        Commit remoteCommit = readObject(remoteCommitPath, Commit.class);
        return remoteCommit;
    }

    /** Update the files that the commit is tracking. */
    public void updateTrackedFiles() {
        Commit parentCommit = loadCommit(parentSHA1);
        this.trackedFiles = parentCommit.trackedFiles;

        ArrayList<String> addedFiles = new ArrayList<>(plainFilenamesIn(Stage.ADDITION_DIR));

        for (String fileName : addedFiles) {
            Stage stagedFile = Stage.loadStagedFile(fileName, "add");
            Blob addedFile = stagedFile.getCurrFile();

            updateHashMap(addedFile);
        }

        ArrayList<String> removedFiles = new ArrayList<>(plainFilenamesIn(Stage.REMOVAL_DIR));
        for (String fileName : removedFiles) {
            Stage stagedFile = Stage.loadStagedFile(fileName, "remove");
            Blob removedFile = stagedFile.getCurrFile();

            trackedFiles.remove(removedFile.getName());
        }
    }

    /** Update the hashmap to include the specified blob if the blob is not already in it. */
    private void updateHashMap(Blob addedFile) {
        String name = addedFile.getName();
        String sha1 = addedFile.getSHA1();
        if (trackedFiles.containsKey(name) && trackedFiles.get(name).equals(sha1)) {
            return;
        }
        else {
            trackedFiles.put(name, sha1);
            addedFile.saveBlob();
        }
    }

    /** Return the current head commit. */
    public static Commit getHeadCommit() {
        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);
        String commitSHA1 = currBranch.getCommitSHA1();
        return Commit.loadCommit(commitSHA1);
    }

    /** Return the current head commit's SHA1 ID. */
    public static String getHeadCommitSHA1() {
        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);
        return currBranch.getCommitSHA1();
    }

    /** Return the head commit of the specified branch. */
    public static Commit getBranchHeadCommit(String branchName) {
        File pathToBranch = join(Branch.HEADS_DIR, branchName);
        Branch currBranch = Branch.loadBranch(pathToBranch);
        String commitSHA1 = currBranch.getCommitSHA1();
        return Commit.loadCommit(commitSHA1);
    }

    /** If the commitID is abbreviated, this returns the closest match in the commits directory. */
    public static String handleShortenedIDs(String commitID) {
        if (commitID.length() == UID_LENGTH) {
            return commitID;
        }

        ArrayList<String> commitSHA1s = new ArrayList<>(plainFilenamesIn(Commit.COMMITS_DIR));
        for (String sha1 : commitSHA1s) {
            String shortenedSHA1 = sha1.substring(0, commitID.length());
            if (shortenedSHA1.equals(commitID)) {
                return sha1;
            }
        }
        return null;
    }

    /** Return true if file has not been updated. */
    public boolean checkIfUnupdatedFile(Blob addedFile) {
        String name = addedFile.getName();
        String sha1 = addedFile.getSHA1();
        return trackedFiles.containsKey(name) && trackedFiles.get(name).equals(sha1);
    }

}
