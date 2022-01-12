// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Main.java

package project;

import java.io.IOException;
import java.io.PrintStream;

// Referenced classes of package project:
//            Project

public class Main {

    public Main() {
    }

    public static void main(String args[])
            throws IOException {
        if (args.length == 3) {
            Project project = new Project(args[0], args[1], args[2]);
            project.compute();
        } else {
            System.err.println("Wrong input arguments: [SNP-Table] [Newick-file] [Output directory]");
        }
    }
}
