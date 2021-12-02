// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   FastAEntry.java

package datastructures;


public class FastAEntry
{

    public FastAEntry(String s, String s1)
    {
        header = "";
        sequence = "";
        header = s;
        sequence = s1;
    }

    public String getIthChar(Integer integer)
    {
        return (new StringBuilder()).append("").append(sequence.charAt(integer.intValue())).toString();
    }

    public String getHeader()
    {
        return header;
    }

    public void setHeader(String s)
    {
        header = s;
    }

    public String getSequence()
    {
        return sequence;
    }

    public void setSequence(String s)
    {
        sequence = s;
    }

    public int getSequenceLength()
    {
        return sequence.length();
    }

    public String toString()
    {
        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.append(">");
        stringbuffer.append(header);
        stringbuffer.append("\n");
        String as[] = sequence.split("(?<=\\G.{80})");
        for(int i = 0; i < as.length; i++)
        {
            stringbuffer.append(as[i]);
            stringbuffer.append("\n");
        }

        return stringbuffer.toString();
    }

    private String header;
    private String sequence;
}
