package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import static gitlet.Utils.*;

/**
 *  Represents a local representation of the Remote repository. Provides a variety of
 *  helper methods for pushing, fetching, updating branches, and saving remotes.
 *
 *  @author Collin Bowers
 */
public class Remote implements Serializable {
    public static final File REMOTES_DIR = join(Repository.OBJECTS_DIR, "remotes");
    private String name;
    private File path;

    public Remote(String name, File path) {
        this.name = name;
        this.path = path;
    }

    public void saveRemote() {
        File remoteToBeSaved = new File(REMOTES_DIR, name);
        writeObject(remoteToBeSaved, this);
    }

    public static Remote loadRemote(String remoteName) {
        File remoteToBeLoaded = new File(REMOTES_DIR, remoteName);
        if (!remoteToBeLoaded.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        Remote loadedRemote = readObject(remoteToBeLoaded, Remote.class);
        return loadedRemote;
    }

    /** Point the REMOTE_HEAD at the specified remote branch name. */
    public void updateRemoteHead(String branchName) {
        File REMOTE_HEAD = join(Repository.GITLET_DIR, "REMOTE_HEAD");
        File remoteBranchDirPath = join(Branch.HEADS_DIR, name, branchName);
        writeObject(REMOTE_HEAD, remoteBranchDirPath);
    }

    /** Return the head commit from the remote branch. */
    public Commit getRemoteHeadCommit(String branchName) {
        File remoteBranchPath = join(path, "refs", "heads", branchName);
        Branch remoteBranch = readObject(remoteBranchPath, Branch.class);

        String commitSHA1 = remoteBranch.getCommitSHA1();
        File remoteCommitPath = join(path, "objects", "commits", commitSHA1);
        Commit remoteCommit = readObject(remoteCommitPath, Commit.class);
        return remoteCommit;
    }

    /** Create a local version of the remote branch and
     *  point it at the head commit of the remote branch. */
    public void createRemoteBranch(String branchName) {
        File remoteBranchDir = join(Branch.HEADS_DIR, name);
        remoteBranchDir.mkdirs();
        String remoteHeadSHA1 = getRemoteHeadCommit(branchName).getCurrSHA1();
        Branch remote = new Branch(branchName, remoteHeadSHA1);
        remote.updateRemoteBranch(name, remoteHeadSHA1);
    }

    /** Update the remote branch to point to the given commit SHA1 ID. */
    public void updateRemoteBranch(String branchName, String commitSHA1) {
        File remoteBranchPath = join(path, "refs", "heads", branchName);
        Branch remoteBranch = readObject(remoteBranchPath, Branch.class);
        remoteBranch.updateBranch(commitSHA1, remoteBranchPath);
    }

    /** Update the local representation of the remote branch to point to
     *  the given commit SHA1 ID. */
    public void updateLocalRemoteBranch(String branchName, String commitSHA1) {
        File remoteBranchDir = join(Branch.HEADS_DIR, name, branchName);
        Branch remote = Branch.loadBranch(remoteBranchDir);
        remote.updateRemoteBranch(name, commitSHA1);
    }

    /** Append the current branch's commits to the end of the
     *  given branch at the given remote. Also resets the
     *  remote head to the front of the appended commits. */
    public void pushCommits(Commit remoteCommit, Commit headCommit, String branchName) {
        String currCommitSHA1 = headCommit.getCurrSHA1();
        String remoteCommitSHA1 = remoteCommit.getCurrSHA1();
        while (currCommitSHA1 != null && !currCommitSHA1.equals(remoteCommitSHA1)) {
            Commit currCommit = Commit.loadCommit(currCommitSHA1);
            pushCommit(currCommit);
            currCommitSHA1 = currCommit.getParentSHA1();
        }

        if (currCommitSHA1 == null) {
            System.out.println("Please pull down remote changes before pushing.");
            System.exit(0);
        }

        updateRemoteBranch(branchName, headCommit.getCurrSHA1());
        updateLocalRemoteBranch(branchName, headCommit.getCurrSHA1());
    }

    /** Push the given commit to the remote repository. */
    private void pushCommit(Commit currCommit) {
        File remoteCommitPath = join(path, "objects", "commits", currCommit.getCurrSHA1());
        writeObject(remoteCommitPath, currCommit);
    }

    /** Starting at the head commit in the specified remote branch,
     *  copy over all commits and blobs not in the local repo */
    public void fetchContent(String branchName) {
        Commit remoteCommit = getRemoteHeadCommit(branchName);
        String remoteCommitSHA1 = remoteCommit.getCurrSHA1();

        while (remoteCommitSHA1 != null && !join(Commit.COMMITS_DIR, remoteCommitSHA1).exists()) {
            remoteCommit = Commit.loadRemoteCommit(remoteCommitSHA1, path);
            fetchCommit(remoteCommit);
            remoteCommitSHA1 = remoteCommit.getParentSHA1();
        }

        HashMap<String, String> blobs = remoteCommit.getTrackedFiles();
        fetchBlobs(blobs);
    }

    /** Fetch the given commit from the remote repository and
     *  add that commit to the local repository. */
    private void fetchCommit(Commit remoteCommit) {
        File localCommitPath = join(Commit.COMMITS_DIR, remoteCommit.getCurrSHA1());
        writeObject(localCommitPath, remoteCommit);
    }

    /** Fetch all blobs from the remote repository
     *  that are not in the local repository. */
    public void fetchBlobs(HashMap<String, String> blobs) {
        for (HashMap.Entry<String, String> file : blobs.entrySet()) {
            String blobSHA1 = file.getValue();
            if (!join(Blob.BLOBS_DIR, blobSHA1).exists()) {
                Blob remoteBlob = Blob.loadRemoteBlob(blobSHA1, path);
                remoteBlob.saveBlob();
            }
        }
    }

}