// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   NewFile.java

package datastructures;

import java.io.*;

public class NewFile
{

    public NewFile(String s)
    {
        if(s.lastIndexOf('/') == s.length())
            path = s;
        else
            path = (new StringBuilder()).append(s).append("/").toString();
    }

    public void createDir(String s)
    {
        File file = new File((new StringBuilder()).append(path).append(s).toString());
        if(file.mkdir())
            System.out.println((new StringBuilder()).append("Directory ").append(s).append(" created in ").append(path).toString());
        path = (new StringBuilder()).append(path).append(s).toString();
    }

    private boolean checkFile(File file)
    {
        if(file != null)
        {
            try
            {
                file.createNewFile();
            }
            catch(IOException ioexception)
            {
                System.err.println((new StringBuilder()).append("Error creating ").append(file.toString()).toString());
            }
            if(file.isFile() && file.canWrite() && file.canRead())
                return true;
        }
        return false;
    }

    public String createFile(String s)
    {
        if(checkFile(new File((new StringBuilder()).append(path).append("/").append(s).toString())))
            System.out.println((new StringBuilder()).append(s).append(" created").toString());
        return (new StringBuilder()).append(path).append("/").append(s).toString();
    }

    private String path;
}
