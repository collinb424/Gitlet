# Gitlet 
A lightweight version control system that provides much of the key functionality of Git.  
  
  
Example usage of a few basic commands:


https://user-images.githubusercontent.com/92958582/184517671-088acd20-8843-4f8e-ad82-62bf5265963b.mp4

  
    
## Main Functionality
- Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.
- Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.
- Viewing the history of your backups. In Gitlet, you view this history in something called the log.
- Maintaining related sequences of commits, called branches.
- Merging changes made in one branch into another.
- Allowing for remote collaboration with other people.

## How to Use
The ability to compile Java is required.  

The gitlet folder must be in the current directory.  

To initialize gitlet for version control in the current directory, use the following command:
```
java gitlet.Main init
```

For subsequent commands, use the following syntax:
```
java gitlet.Main [command name]
```

Supported commands:
- init
- add [file name]
- commit [message]
- rm [file name]
- log
- global-log
- find [commit message]
- status
- checkout -- [file name]
- checkout [commit id] -- [file name]
- checkout [branch name]
- branch [branch name]
- rm-branch [branch name]
- reset [commit id]
- merge [branch name]
- add-remote [remote name] [name of remote directory]/.gitlet
- rm-remote [remote name]
- push [remote name] [remote branch name]
- fetch [remote name] [remote branch name]
- pull [remote name] [remote branch name]  
  
More information on the commands can be found [here](https://sp21.datastructur.es/materials/proj/proj2/proj2). Note: this link gave no hints on how to actually implement or design the program—I thought of the design and implemented the code myself.
