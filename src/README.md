# src

This directory contains the source code of CLASSICO.

`classicoV2.jar` is the precompiled version of the tool.

`Main.java` is the main Java class, that is executed. The `Args.java` class contains the input arguments and command line specifications using the `jcommander` library. The `SNPTree.java` class specifies the tree structure and the `Node.class` the nodes in this tree structure. Those two classes are also the classes in which the main functionalities of CLASSICO are implemented. The `Output.java` class constructs the output files for the phyletic groups and the `SNPType.class` and `Phyly.class` are only enum classes.