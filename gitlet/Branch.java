package gitlet;

import java.io.File;
import java.io.Serializable;
import static gitlet.Utils.*;

/**
 *  Represents a branch object. Provides a variety of helper methods
 *  for creating branches, updating which commit they point to, and
 *  changing which branch the HEAD pointer points to.
 *
 *  @author Collin Bowers
 */
public class Branch implements Serializable {
    public static final File REFS_DIR = join(Repository.GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    private String branchName;
    private String commitID;

    public Branch(String branchName, String commitID) {
        this.branchName = branchName;
        this.commitID = commitID;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getCommitSHA1() {
        return commitID;
    }

    public void createBranch() {
        File branchToBeSaved = new File(HEADS_DIR, branchName);
        writeObject(branchToBeSaved, this);
    }

    public static Branch loadBranch(File branchToBeLoaded) {
        return readObject(branchToBeLoaded, Branch.class);
    }

    /** Update the current branch's head commit to be the one with the specified SHA1 ID. */
    public void updateBranch(String sha1, File path) {
        commitID = sha1;
        writeObject(path, this);
    }

    /** Update the local version of the remote branch to point to the given SHA1 ID. */
    public void updateRemoteBranch(String remoteName, String sha1) {
        File branchToBeUpdated = join(HEADS_DIR, remoteName, branchName);
        commitID = sha1;
        writeObject(branchToBeUpdated, this);
    }

    /** Point HEAD to the current branch. */
    public void updateHead() {
        File HEAD = join(Repository.GITLET_DIR, "HEAD");
        File pathToBranch = join(HEADS_DIR, branchName);
        writeObject(HEAD, pathToBranch);
    }

    /** Point HEAD to the specified branch. */
    public void updateHead(String branchName) {
        File HEAD = join(Repository.GITLET_DIR, "HEAD");
        File pathToBranch = join(HEADS_DIR, branchName);
        writeObject(HEAD, pathToBranch);
    }

    /** Returns the filepath to the current branch. */
    public static File getCurrBranch() {
        File HEAD = join(Repository.GITLET_DIR, "HEAD");
        return readObject(HEAD, File.class);
    }

    /** Exit the program if the branch name is not unique. */
    public static void checkIfUniqueName(String name) {
        if (join(HEADS_DIR, name).exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
    }

}
