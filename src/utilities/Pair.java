// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Pair.java

package utilities;


public class Pair
    implements Comparable
{

    public Pair(Comparable comparable, Comparable comparable1)
    {
        first = comparable;
        second = comparable1;
    }

    public Comparable getFirst()
    {
        return first;
    }

    public Comparable getSecond()
    {
        return second;
    }

    public String toString()
    {
        return (new StringBuilder()).append("(").append(first).append("/").append(second).append(")").toString();
    }

    public int compareTo(Pair pair)
    {
        int i = first.compareTo(pair.getFirst());
        if(i == 0)
            return second.compareTo(pair.getSecond());
        else
            return i;
    }

    public int compareTo(Object obj)
    {
        return compareTo((Pair)obj);
    }

    private Comparable first;
    private Comparable second;
}
