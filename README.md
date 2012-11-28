# redundant-include #

redundant-include is a command line based tool to control same (reduandant) content which is spread over several different text files. 

## Getting started in one minute ##
Create *file1.txt*:
A redundant section is marked in the file with a special start- and end-marker.

>     Content before include.  
>
>     <!-- <INCLUDE file="include1.txt"> -->
>
>     Redundant content which is spread over several files.
>
>     <!-- </INCLUDE> -->   
> 
>     Content after include.


Start `rinc status` from the command line inside the directory where *file.txt* is located:
>     >rinc status
>     include1.txt status OK


Copy *file1.txt* to another file *file2.txt*.

Start `rinc status` from the command line:
>     >rinc status
>     include1.txt status OK

In file1.txt change the text inside the start marker `<!-- <INCLUDE file="include1.txt"> -->` and the end marker `<!-- </INCLUDE> -->` to some other text and start `rinc status` from the command line:
>     >rinc status
>     include1.txt status DIFFER
>     New hash 783f3e867d4d9207c295c18cdb56f57f
>     Old hash 1bbbd0569470e9afdd4d0c04c5d6000d
>     Newest version in
>     file1.txt
>     ---- CHANGE
>     [position: 3, size: 1, lines: [New text]]
>     ----

After that type `rinc merge`

>     >rinc merge
>     Hash to use: [783f3e867d4d9207c295c18cdb56f57f]
>     include1.txt [M]

After that *file1.txt* and *file2.txt* are up to date with the new content within the include markers.


##Commands##

Full command list

<!-- <INC file="status"> -->

     rinc status

<!-- </INC> -->
<!-- <INC file="show"> -->

     rinc show [<file>|<hash>]

<!-- </INC> -->
<!-- <INC file="merge"> -->

     rinc merge

<!-- </INC> -->
<!-- <INC file="reversemerge"> -->

     rinc reversemerge

<!-- </INC> -->
<!-- <INC file="resolve"> -->

     rinc resolve <hash> [<hash>...]

<!-- </INC> -->
<!-- <INC file="contract"> -->

     rinc contract

<!-- </INC> -->
<!-- <INC file="expand"> -->

     rinc expand

<!-- </INC> -->
<!-- <INC file="help"> -->

     rinc help

<!-- </INC> -->


###rinc status###

<!-- <INC file="status"> -->

     rinc status

<!-- </INC> -->

The (read-only) `status` command shows the status of every include-snippet in the current working directory. 
There are three different states: `OK`, `DIFFER`, `CONFLICT`. 

`OK` means that the include-snippet is equal in every found origin file.

`DIFFER` means that there are **two** different versions of the same include-snippet in the origin files. In case of this state the output shows the *new* and the *old* hash, which is the md5-sums of the new and the old include-content (the data between the two include-markers).
The definition of *new* and *old* is as follows:
The include-content version is *newer* if and only if it occurs in fewer origin files, than the other include-content version. If both include-content versions occur in the same number of origin files, the include-content version, which is included in the last modified origin file, is newer.

`CONFLICT` means that there are at least **three** different versions of the same include-snippet in the origin files. In this case the output shows the number of include-content versions (hashes of the include-content) exist and their association to the origin files. 


###rinc show###

<!-- <INC file="show"> -->

     rinc show [<file>|<hash>]

<!-- </INC> -->

The (read-only) `show` command shows the current content(s) and the hash(es) of a specified include-snippet. You can specify either the filename of the snippet-include or a specific hash, which is shown in the output of the `status` command in case of a `DIFFER` or a `CONFLICT` state.
When you supply the filename of the include-snippet you get all current existing include-contents with their hashes.


###rinc merge###

<!-- <INC file="merge"> -->

     rinc merge

<!-- </INC> -->

The (read and write) `merge` command merges origin files that have include-snippets which are in state `DIFFER`.
The result of the merge is that after the merge all include-snippets are in state `OK`. The merge operation copies the *new* include-content of a snippet to all occurences of this include-snippet in the origin files.


###rinc reversemerge###

<!-- <INC file="reversemerge"> -->

     rinc reversemerge

<!-- </INC> -->

The (read and write) `reversemerge` command is exactly the same as the `merge` command, but with the difference that it copies the *old* include-content of a snippet to all occurences of this include-snippet in the origin files.

Use this command to 

* revert changes of a *new* include-content.
* populate a stub-include-marker with the current content of the associated include-snippet.


###rinc resolve###

<!-- <INC file="resolve"> -->

     rinc resolve <hash> [<hash>...]

<!-- </INC> -->

The (read and write) `resolve` command can be used to solve include-snippets that are in state `CONFLICT`. 
You must supply one or more hashes to select the include-content version which should be applied to all origin files that include this include-snippet.


###rinc contract###

<!-- <INC file="contract"> -->

     rinc contract

<!-- </INC> -->

Use the (read and write) `contract` command to extract all include-snippets to real files that are grouped together under a directory in the current working directory named "redundant-includes". The origin files gets contracted because the include-snippets collapse to the include-markers only and a keyword which states that this include-snippet is currently contracted.

Use this command if you use a version control system and you dont want to commit changes to many files if you have only done changes to one include-content. This operation can be undone with the `expand`command.

The `contract` command is only applicable to a working directory where all include-snippets are in state `OK`. You have to merge or resolve conflicts before using the `contract` command.


##rinc expand ###

<!-- <INC file="expand"> -->

     rinc expand

<!-- </INC> -->

Use the (read and write) `expand` command to undo a `contract` command. It reads all extracted include-snippets from the directory "redundant-includes" from your current working directory and fills it in the include-markers in the origin files.

The `expand` command is only applicable if the working directory has been contracted first, which is determined by the existence of the "redundant-includes"-directory.


##rinc help##

<!-- <INC file="help"> -->

     rinc help

<!-- </INC> -->

Shows a brief help text of rinc commands.

<!---
##Todos##
* Show numbers of origin files in status
* support for same include in same origin file
* Test resolve with more than one conflict
-->
