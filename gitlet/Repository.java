package gitlet;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import static gitlet.Utils.*;


/**
 *  This class represents a Gitlet repository and is where the main logic of the program lives.
 *  The command the user chooses in main calls its respective method in this class.
 *  Most of the implementation for a given command is either in this class or in
 *  a variety of helper methods from other classes which are called to make the command work properly.
 *
 *  @author Collin Bowers
 */
public class Repository {

    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /** Initialize the .gitlet directory with an initial commit and a master branch pointing to it. */
    public static void init() {
        createDirs();

        Commit initial = new Commit("initial commit", createDate(true), null, null);
        String sha1 = initial.saveCommit();

        Branch master = new Branch("master", sha1);
        master.createBranch();
        master.updateHead();
    }

    /** Create the necessary subdirectories within the .gitlet directory. */
    private static void createDirs() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        GITLET_DIR.mkdir();
        Commit.COMMITS_DIR.mkdirs();
        Blob.BLOBS_DIR.mkdirs();
        Stage.ADDITION_DIR.mkdirs();
        Stage.REMOVAL_DIR.mkdirs();
        Branch.HEADS_DIR.mkdirs();
        Remote.REMOTES_DIR.mkdirs();
    }

    /** Format the date properly for committing. */
    private static String createDate(boolean initialCommit) {
        Date date = (initialCommit) ? new Date(0) : new Date();
        SimpleDateFormat dt = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        return dt.format(date);
    }

    /** Stage the given file for addition. */
    public static void add(String fileName) {
        Blob file = initBlob(fileName);
        Stage add = new Stage(file, "add");
        add.handleAdding();
    }

    /** Initialize a blob object for the given file. */
    private static Blob initBlob(String fileName) {
        File filePath = join(CWD, fileName);
        if (!filePath.isFile()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String contents = readContentsAsString(filePath);
        String sha1 = sha1(contents);
        return new Blob(fileName, contents, sha1);
    }

    /** Save a snapshot of the tracked files in the current commit and those in the staging area. */
    public static void commit(String message, String otherParent) {
        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);
        String parentSHA1 = currBranch.getCommitSHA1();

        handleCommitFailureCases(message);
        Commit currCommit = new Commit(message, createDate(false), parentSHA1, otherParent);
        currCommit.updateTrackedFiles();
        String sha1 = currCommit.saveCommit();

        currBranch.updateBranch(sha1, pathToBranch);
        Stage.clearStagedFiles();
    }

    /** Committing fails if the staging area is empty or if there is no message provided. */
    private static void handleCommitFailureCases(String message) {
        Stage.isStagingAreaEmpty();

        if (message.length() == 0) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
    }

    /** Remove the file from the CWD and from being tracked. */
    public static void remove(String fileName) {
        Stage remove = new Stage(fileName, "remove");
        remove.handleRemoving();
    }

    /** Display each info for each commit along the path from the head commit to the initial commit. */
    public static void log() {
        String currCommitSHA1 = Commit.getHeadCommitSHA1();

        do {
            Commit currCommit = Commit.loadCommit(currCommitSHA1);
            String otherParentSHA1 = currCommit.getOtherParentSHA1();
            if (otherParentSHA1 != null) {
                printCommitInfo(currCommit, true);
            }
            else {
                printCommitInfo(currCommit, false);
            }
            currCommitSHA1 = currCommit.getParentSHA1();
        } while (currCommitSHA1 != null);
    }

    /** Display information for all commits in an unspecified order. */
    public static void globalLog() {
        ArrayList<String> commits = new ArrayList<>(plainFilenamesIn(Commit.COMMITS_DIR));

        for (String commitSHA1 : commits) {
            Commit currCommit = Commit.loadCommit(commitSHA1);
            printCommitInfo(currCommit, false);
        }
    }

    /** For log and global log commands, print the desired info for the specified commit. */
    private static void printCommitInfo(Commit currCommit, boolean merge) {
        System.out.println("===");
        System.out.println("commit " + currCommit.getCurrSHA1());
        if (merge) {
            String parentSHA1 = currCommit.getParentSHA1();
            String otherParentSHA1 = currCommit.getOtherParentSHA1();
            System.out.println("Merge: " + parentSHA1.substring(0, 7) + " " + otherParentSHA1.substring(0, 7));
        }
        System.out.println("Date: " + currCommit.getDate());
        System.out.println(currCommit.getMessage() + "\n");
    }

    /** Print the SHA1 IDs for all commits that have the given message. */
    public static void find(String message) {
        ArrayList<String> commits = new ArrayList<>(plainFilenamesIn(Commit.COMMITS_DIR));
        boolean foundMessage = false;
        for (String commitSHA1 : commits) {
            Commit currCommit = Commit.loadCommit(commitSHA1);
            if (currCommit.getMessage().equals(message)) {
                System.out.println(commitSHA1);
                foundMessage = true;
            }
        }
        if (!foundMessage) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Display all branches and files in the staging area. */
    public static void status() {
        System.out.println("=== Branches ===");
        printBranches();

        System.out.println("\n=== Staged Files ===");
        printStageDir("add");

        System.out.println("\n=== Removed Files ===");
        printStageDir("remove");

        System.out.println();
    }

    /** Print name of all branches and an asterisk next to the current branch. */
    private static void printBranches() {
        Branch currBranch = Branch.loadBranch(Branch.getCurrBranch());
        System.out.println("*" + currBranch.getBranchName());
        ArrayList<String> branches = new ArrayList<>(plainFilenamesIn(Branch.HEADS_DIR));
        for (String branchName : branches) {
            if (branchName.equals(currBranch.getBranchName())) {
                continue;
            }
            System.out.println(branchName);
        }
    }

    /** Print the name of all files staged for addition or removal based on which is specified. */
    private static void printStageDir(String type) {
        ArrayList<String> stagedFiles;
        if (type.equals("add")) {
            stagedFiles = new ArrayList<>(plainFilenamesIn(Stage.ADDITION_DIR));
        }
        else {
            stagedFiles = new ArrayList<>(plainFilenamesIn(Stage.REMOVAL_DIR));
        }

        for (String fileName : stagedFiles) {
            Stage file = Stage.loadStagedFile(fileName, type);
            System.out.println(file.getCurrFile().getName());
        }
    }

    /** Checkout the file with the given name. */
    public static void checkoutFile(String fileName) {
        Commit headCommit = Commit.getHeadCommit();
        replaceFile(fileName, headCommit);
    }

    /** Checkout the file with the given name from the specified commit. */
    public static void checkoutOlderFile(String commitID, String fileName) {
        commitID = Commit.handleShortenedIDs(commitID);
        Commit currCommit = Commit.loadCommit(commitID);
        replaceFile(fileName, currCommit);
    }

    /** Checkout the specified branch and all files tracked by that branch's head commit. */
    public static void checkoutBranch(String branchName) {
        File pathToDesiredBranch = join(Branch.HEADS_DIR, branchName);
        File pathToCurrBranch = Branch.getCurrBranch();

        checkFailureCases(pathToDesiredBranch, pathToCurrBranch, branchName);

        Commit desiredCommit = Commit.getBranchHeadCommit(branchName);
        changeCWD(desiredCommit);

        Stage.clearStagedFiles();

        Branch newBranch = Branch.loadBranch(pathToDesiredBranch);
        newBranch.updateBranch(desiredCommit.getCurrSHA1(), pathToDesiredBranch);
        newBranch.updateHead(branchName);
    }

    /** Replace the file in the CWD with the version of the file from the given commit. */
    private static void replaceFile(String fileName, Commit currCommit) {
        String fileSHA1 = currCommit.getTrackedFiles().get(fileName);
        if (fileSHA1 == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        Blob currFile = Blob.loadBlob(fileSHA1);
        String contents = currFile.getContents();

        File file = new File(CWD, fileName);
        restrictedDelete(file);
        writeContents(file, contents);
    }

    /** Main driver for checking out all files in the specified branch. */
    private static void changeCWD(Commit desiredCommit) {
        HashMap<String, String> desiredTrackedFiles = desiredCommit.getTrackedFiles();
        for (HashMap.Entry<String, String> file : desiredTrackedFiles.entrySet()) {
            replaceFile(file.getKey(), desiredCommit);
        }

        ArrayList<String> cwdFiles = new ArrayList<>(plainFilenamesIn(CWD));
        for (String fileName : cwdFiles) {
            if (!desiredTrackedFiles.containsKey(fileName)) {
                restrictedDelete(join(CWD, fileName));
            }
        }
    }

    /** If the path to the specified branch doesn't exist or the specified branch is the current branch,
     *  inform the user and exit the program. */
    private static void checkFailureCases(File pathToDesiredBranch, File pathToCurrBranch, String branchName) {
        if (!pathToDesiredBranch.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        } else if (pathToCurrBranch.equals(pathToDesiredBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        Commit desiredCommit = Commit.getBranchHeadCommit(branchName);
        handleUntrackedFiles(desiredCommit);
    }

    /* If there are untracked files in the CWD, the program stops execution and informs the user. */
    private static void handleUntrackedFiles(Commit desiredCommit) {
        Commit headCommit = Commit.getHeadCommit();

        HashMap<String, String> headTrackedFiles = headCommit.getTrackedFiles();
        HashMap<String, String> desiredTrackedFiles = desiredCommit.getTrackedFiles();
        ArrayList<String> cwdFiles = new ArrayList<>(plainFilenamesIn(CWD));

        for (String fileName : cwdFiles) {
            String fileSHA1 = sha1(readContentsAsString(join(CWD, fileName)));
            if (!headTrackedFiles.containsKey(fileName) && (!fileSHA1.equals(desiredTrackedFiles.get(fileName)))) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Create a branch with the specified name.
     *  Note: Does not switch HEAD to that branch. */
    public static void branch(String branchName) {
        Branch.checkIfUniqueName(branchName);

        String headCommitSHA1 = Commit.getHeadCommitSHA1();
        Branch b = new Branch(branchName, headCommitSHA1);
        b.createBranch();
    }

    /** Remove the branch with the specified name. */
    public static void removeBranch(String branchName) {
        checkIfBranchExists(branchName);

        File branchPath = join(Branch.HEADS_DIR, branchName);
        if (Branch.getCurrBranch().equals(branchPath)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        branchPath.delete();
    }

    /** Exit the program if a branch with that name already exists. */
    private static void checkIfBranchExists(String branchName) {
        if (!join(Branch.HEADS_DIR, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /** Reverts the CWD back to its state from the specified commit. */
    public static void reset(String commitID) {
        commitID = Commit.handleShortenedIDs(commitID);
        Commit desiredCommit = Commit.loadCommit(commitID);
        handleUntrackedFiles(desiredCommit);

        changeCWD(desiredCommit);

        Stage.clearStagedFiles();

        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);
        currBranch.updateBranch(desiredCommit.getCurrSHA1(), pathToBranch);
    }

    /** Merges files from the given branch into the current branch. */
    public static void merge (String branchName) {
        handleMergeFailureCases(branchName);
        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);

        Commit LCA = findLCA(branchName);
        Commit otherCommit = Commit.getBranchHeadCommit(branchName);
        Commit headCommit = Commit.getHeadCommit();

        HashMap<String, String> headTrackedFiles = headCommit.getTrackedFiles();
        HashMap<String, String> otherTrackedFiles = otherCommit.getTrackedFiles();
        HashMap<String, String> lcaTrackedFiles = LCA.getTrackedFiles();

        ArrayList<String> blobs = new ArrayList<>(plainFilenamesIn(Blob.BLOBS_DIR));
        HashSet<String> seenBlobs = new HashSet<>();
        for (String blobSHA1 : blobs) {
            Blob currBlob = Blob.loadBlob(blobSHA1);
            String name = currBlob.getName();
            if (seenBlobs.contains(name)) {
                continue;
            }
            seenBlobs.add(name);
            String headSHA1 = headTrackedFiles.get(name);
            String otherSHA1 = otherTrackedFiles.get(name);
            String lcaSHA1 = lcaTrackedFiles.get(name);
            handleMerging(headSHA1, otherSHA1, lcaSHA1, branchName);
        }

        commit("Merged " + branchName + " into " + currBranch.getBranchName() + ".", otherCommit.getCurrSHA1());
    }

    /** Abort the merge if a branch with the specified name does not exist, if there are uncommitted changes,
     *  or if the branch to be merged into is the same as the current branch. */
    private static void handleMergeFailureCases(String branchName) {
        checkIfBranchExists(branchName);

        File pathToOtherBranch = join(Branch.HEADS_DIR, branchName);
        File pathToBranch = Branch.getCurrBranch();
        Branch currBranch = Branch.loadBranch(pathToBranch);
        handleUntrackedFiles(Commit.getBranchHeadCommit(branchName));
        if (Stage.ADDITION_DIR.list().length > 0 || Stage.REMOVAL_DIR.list().length > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        else if (pathToBranch.equals(pathToOtherBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /** Find the latest common ancestor commit, which is the most recent commit from
     *  which there is a path from both branch heads. */
    private static Commit findLCA(String branchName) {
        String otherCommitSHA1 = Commit.getBranchHeadCommit(branchName).getCurrSHA1();
        HashSet<String> commitSHA1s = new HashSet<>();
        do {
            Commit otherCommit = Commit.loadCommit(otherCommitSHA1);
            commitSHA1s.add(otherCommitSHA1);
            otherCommitSHA1 = otherCommit.getParentSHA1();
        } while (otherCommitSHA1 != null);

        String currCommitSHA1 = Commit.getHeadCommitSHA1();
        while (!commitSHA1s.contains(currCommitSHA1)) {
            Commit currCommit = Commit.loadCommit(currCommitSHA1);
            currCommitSHA1 = currCommit.getParentSHA1();
        }
        String LCA = currCommitSHA1;

        handleLCASpecialCases(LCA, branchName);
        return Commit.loadCommit(LCA);
    }

    /** If the latest common ancestor is the same commit as the other branch's
     *  head commit, the merge is already complete, so we simply exit. If the latest
     *  common ancestor is the current branch's head commit, then we check out
     *  the other branch and then exit. */
    private static void handleLCASpecialCases(String LCA, String branchName) {
        String otherCommitSHA1 = Commit.getBranchHeadCommit(branchName).getCurrSHA1();
        String currCommitSHA1 = Commit.getHeadCommitSHA1();

        if (LCA.equals(otherCommitSHA1)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        else if (LCA.equals(currCommitSHA1)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded");
            System.exit(0);
        }
    }

    /** Handle all the special cases associated with merging. The logic is primarily based on
     *  checking if the file under consideration has been modified, removed, or created in
     *  the head commit, other branch head commit, or latest common ancestor commit. */
    private static void handleMerging(String headSHA1, String otherSHA1, String lcaSHA1, String branchName) {
        Blob headFile = Blob.loadBlob(headSHA1);
        Blob otherFile = Blob.loadBlob(otherSHA1);
        Blob lcaFile = Blob.loadBlob(lcaSHA1);

        if (lcaFile != null && (headFile == null && otherFile == null)) {
            return;
        }
        else if (headFile != null && (otherFile == null && lcaFile == null)) {
            return;
        }
        else if (otherFile != null && (headFile == null && lcaFile == null)) {
            replaceFile(otherFile.getName(), Commit.getBranchHeadCommit(branchName));
            Stage add = new Stage(otherFile, "add");
            add.handleAdding();
        }
        else if (Objects.equals(headSHA1, lcaSHA1) && otherFile == null) {
            remove(headFile.getName());
        }
        else if (Objects.equals(otherSHA1, lcaSHA1) && headFile == null) {
            return;
        }
        else if (!Objects.equals(otherSHA1, lcaSHA1) && Objects.equals(headSHA1, lcaSHA1)) {
            Stage add = new Stage(otherFile, "add");
            add.handleAdding();
        }
        else if (!Objects.equals(headSHA1, lcaSHA1) && Objects.equals(otherSHA1, lcaSHA1)) {
            Stage add = new Stage(headFile, "add");
            add.handleAdding();
        }
        else if (Objects.equals(headSHA1, otherSHA1) && !Objects.equals(headSHA1, lcaSHA1)) {
            return;
        }
        else if (!Objects.equals(headSHA1, otherSHA1)) {
            handleMergeConflict(headFile, otherFile);
        }
    }

    /** If the file has been changed in different ways in the head commit and
     *  other branch's head commit, inform the user and change the file contents
     *  so that the user can see how the file was modified in each branch. */
    private static void handleMergeConflict(Blob headFile, Blob otherFile) {
        String contents = "<<<<<<<< HEAD\n";
        String headContents = (headFile == null) ? "" : headFile.getContents();
        String otherContents = (otherFile == null) ? "" : otherFile.getContents();
        contents += "contents of file in current branch" + headContents + "=======\n";
        contents += "contents of file in given branch" + otherContents + ">>>>>>>";

        File path = join(CWD, headFile.getName());
        writeContents(path, contents);

        Stage add = new Stage(headFile, "add");
        add.handleAdding();
        System.out.println("Encountered a merge conflict.");
    }

    /** Add a remote directory with the given name and path to be
     *  tracked by the current directory. */
    public static void addRemote(String remoteName, String remotePath) {
        remotePath = remotePath.replace("/", File.separator);
        Remote remote = new Remote(remoteName, new File(remotePath));
        remote.saveRemote();
    }

    /** Removes information in the local repository associated
     *  with the remote of the given name. */
    public static void removeRemote(String remoteName) {
        File remoteToBeRemoved = join(Remote.REMOTES_DIR, remoteName);
        remoteToBeRemoved.delete();
    }

    /** Appends the current branch's commits to the end of the
     *  specified branch at the specified remote repository. */
    public static void push(String remoteName, String remoteBranchName) {
        Remote remote = Remote.loadRemote(remoteName);
        remote.createRemoteBranch(remoteBranchName);
        remote.updateRemoteHead(remoteBranchName);

        Commit remoteCommit = remote.getRemoteHeadCommit(remoteBranchName);
        Commit headCommit = Commit.getHeadCommit();
        remote.pushCommits(remoteCommit, headCommit, remoteBranchName);
    }

    /** Brings down commits and blobs from the remote Gitlet repository
     *  into the local Gitlet repository (if not already there). */
    public static void fetch(String remoteName, String remoteBranchName) {
        Remote remote = Remote.loadRemote(remoteName);
        remote.createRemoteBranch(remoteBranchName);
        remote.fetchContent(remoteBranchName);
        String remoteHeadSHA1 = remote.getRemoteHeadCommit(remoteBranchName).getCurrSHA1();
        remote.updateLocalRemoteBranch(remoteBranchName, remoteHeadSHA1);
    }

    /** Fetches the given remote branch from the given remote repository
     *  and then merges that fetch into the current branch. */
    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + File.separator + remoteBranchName);
    }

}
