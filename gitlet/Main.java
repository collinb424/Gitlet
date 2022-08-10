package gitlet;

/**
 *  Main driver class for Gitlet, a subset of the Git version-control system.
 *  This class takes in the users' input from the command line and
 *  calls the corresponding command in the Repository class.
 *
 *  @author Collin Bowers
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];

        if (!Repository.GITLET_DIR.exists() && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        switch(firstArg) {
            case "init":
                validateNumArgs("init", args, 1);
                Repository.init();
                break;
            case "add":
                validateNumArgs("add", args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                validateNumArgs("commit", args, 2);
                Repository.commit(args[1], null);
                break;
            case "rm":
                validateNumArgs("commit", args, 2);
                Repository.remove(args[1]);
                break;
            case "log":
                validateNumArgs("log", args, 1);
                Repository.log();
                break;
            case "global-log":
                validateNumArgs("global-log", args, 1);
                Repository.globalLog();
                break;
            case "find":
                validateNumArgs("global-log", args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validateNumArgs("status", args, 1);
                Repository.status();
                break;
            case "checkout":
                handleCheckout(args);
                break;
            case "branch":
                validateNumArgs("branch", args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validateNumArgs("rm-branch", args, 2);
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                validateNumArgs("reset", args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validateNumArgs("merge", args, 2);
                Repository.merge(args[1]);
                break;
            case "add-remote":
                validateNumArgs("add-remote", args, 3);
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                validateNumArgs("rm-remote", args, 2);
                Repository.removeRemote(args[1]);
                break;
            case "push":
                validateNumArgs("push", args, 3);
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                validateNumArgs("fetch", args, 3);
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                validateNumArgs("pull", args, 3);
                Repository.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    public static void handleCheckout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            Repository.checkoutFile(args[2]);
        }
        else if (args.length == 4 && args[2].equals("--")) {
            Repository.checkoutOlderFile(args[1], args[3]);
        }
        else if (args.length == 2) {
            Repository.checkoutBranch(args[1]);
        }
        else {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: checkout."));
        }
    }

    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (cmd == "commit" && args.length == 1) {
            throw new RuntimeException(
                    String.format("Please enter a commit message."));
        }

        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }
}
