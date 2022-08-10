package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import static gitlet.Utils.*;

/**
 *  Represents a stage object. Provides a variety of helper methods
 *  for staging files for addition and removal, loading and saving
 *  staged files, and retrieving various information about these files.
 *
 *  @author Collin Bowers
 */
public class Stage implements Serializable {
    public static final File STAGED_DIR = join(Repository.OBJECTS_DIR, "staged");
    public static final File ADDITION_DIR = join(STAGED_DIR, "addition");
    public static final File REMOVAL_DIR = join(STAGED_DIR, "removal");
    private Blob currFile;
    private String fileName;
    private File stageDir;

    public Stage(Blob file, String stageDir) {
        this.currFile = file;
        setStageDir(stageDir);
    }

    public Stage(String fileName, String stageDir) {
        this.fileName = fileName;
        setStageDir(stageDir);
    }

    /** Specify whether the file should be staged for addition or removal. */
    private void setStageDir(String stageDir) {
        if (stageDir.equals("add")) {
            this.stageDir = ADDITION_DIR;
        }
        else {
            this.stageDir = REMOVAL_DIR;
        }
    }

    public Blob getCurrFile() {
        return currFile;
    }

    /** Handle the main logic behind staging a file for addition. */
    public void handleAdding() {
        boolean changed = checkIfFileChanged();
        if (changed) {
            stageFile();
        }
        else if (!changed && isStaged()) {
            unstageFile();
        }
    }

    /** Handle the main logic behind staging a file for removal. */
    public void handleRemoving() {
        HashMap<String, String> trackedFiles = Commit.getHeadCommit().getTrackedFiles();

        String fileNameNoExtension = Blob.getFileNameWithoutExtension(fileName);
        File additionPath = join(ADDITION_DIR, fileNameNoExtension);
        File cwdPath = join(Repository.CWD, fileName);

        if (additionPath.isFile()) {
            Stage addedFile = loadStagedFile(fileNameNoExtension, "add");
            addedFile.unstageFile();
            this.currFile = addedFile.getCurrFile();
        }
        if (trackedFiles.containsKey(fileName)) {
            this.currFile = Blob.loadBlob(trackedFiles.get(fileName));
            stageFile();
            if (cwdPath.isFile()) {
                cwdPath.delete();
            }
        }
        if (currFile == null) {
            System.out.println("No reason to remove the file");
            System.exit(0);
        }
    }

    /** Return true if file has been changed. */
    private boolean checkIfFileChanged() {
        Commit currCommit = Commit.getHeadCommit();
        return !currCommit.checkIfUnupdatedFile(currFile);
    }

    /** Return true is file has been staged. */
    private boolean isStaged() {
        return new File(stageDir, currFile.getName()).isFile();
    }

    /** Stage the file in the specified staging directory. */
    private void stageFile() {
        String name = currFile.getName();
        name = Blob.getFileNameWithoutExtension(name);

        File stagedFile = new File(stageDir, name);
        writeObject(stagedFile, this);
    }

    /** Unstage the file from the specified staging directory. */
    private void unstageFile() {
        File stagedFile = join(stageDir, Blob.getFileNameWithoutExtension(currFile.getName()));
        stagedFile.delete();
    }

    /** Return the desired Stage object from the specified staging directory. */
    public static Stage loadStagedFile(String fileName, String stageDir) {
        File dir = (stageDir.equals("add")) ? ADDITION_DIR : REMOVAL_DIR;
        File fileToBeLoaded = join(dir, fileName);
        Stage loadedFile = readObject(fileToBeLoaded, Stage.class);
        return loadedFile;
    }

    /** Clear all files staged for addition and removal. */
    public static void clearStagedFiles() {
        ArrayList<String> addedFiles = new ArrayList<>(plainFilenamesIn(ADDITION_DIR));
        ArrayList<String> removedFiles = new ArrayList<>(plainFilenamesIn(REMOVAL_DIR));

        for (String fileName : addedFiles) {
            File filePath = join(ADDITION_DIR, fileName);
            filePath.delete();
        }
        for (String fileName : removedFiles) {
            File filePath = join(REMOVAL_DIR, fileName);
            filePath.delete();
        }
    }

    /** If the staging area is empty, the commit command fails. */
    public static void isStagingAreaEmpty () {
        if (ADDITION_DIR.list().length == 0 && REMOVAL_DIR.list().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
    }


}
