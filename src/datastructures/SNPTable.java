// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SNPTable.java

package datastructures;

import java.io.*;
import java.util.*;

// Referenced classes of package datastructures:
//            FastAEntry

public class SNPTable
{

    public SNPTable(String s)
    {
        snpPosNum = new HashMap();
        snps = new HashMap();
        snpPos = new ArrayList();
        sampleNames = new LinkedList();
        inputFile = s;
        readSNPTable();
    }

    public SNPTable(Map map, FastAEntry fastaentry)
    {
        snpPosNum = new HashMap();
        snps = new HashMap();
        snpPos = new ArrayList();
        sampleNames = new LinkedList();
        String s;
        ArrayList arraylist2;
        for(Iterator iterator = map.keySet().iterator(); iterator.hasNext(); snps.put(formatName(s), arraylist2))
        {
            s = (String)iterator.next();
            sampleNames.add(formatName(s));
            Map map1 = (Map)map.get(s);
            ArrayList arraylist1 = new ArrayList(map1.keySet());
            Collections.sort(arraylist1);
            if(snpPos.isEmpty())
            {
                snpPos = arraylist1;
                for(int i = 1; i <= arraylist1.size(); i++)
                    snpPosNum.put((Integer)arraylist1.get(i - 1), Integer.valueOf(i));

            }
            arraylist2 = new ArrayList();
            Integer integer1;
            for(Iterator iterator2 = arraylist1.iterator(); iterator2.hasNext(); arraylist2.add((String)map1.get(integer1)))
                integer1 = (Integer)iterator2.next();

        }

        ArrayList arraylist = new ArrayList();
        Integer integer;
        for(Iterator iterator1 = snpPos.iterator(); iterator1.hasNext(); arraylist.add(fastaentry.getIthChar(Integer.valueOf(integer.intValue() - 1))))
            integer = (Integer)iterator1.next();

        snps.put("Ref", arraylist);
    }

    private void readSNPTable()
    {
        try
        {
            BufferedReader bufferedreader = new BufferedReader(new FileReader(inputFile));
            String s = "";
            Integer integer = Integer.valueOf(0);
            String as[] = new String[0];
            do
            {
                String s1;
                if((s1 = bufferedreader.readLine()) == null)
                    break;
                if(s1.length() != 0)
                {
                    String as1[] = s1.split("\t");
                    if(integer.intValue() > 0)
                    {
                        Integer integer1 = Integer.valueOf(Integer.parseInt(as1[0]));
                        snpPosNum.put(integer1, integer);
                        snpPos.add(integer1);
                        for(int i = 0; i < as.length; i++)
                        {
                            String s2 = formatName(as[i]);
                            String s3 = as1[i + 1];
                            if(snps.containsKey(s2))
                            {
                                List list = (List)snps.get(s2);
                                list.add(s3);
                                snps.put(s2, list);
                            } else
                            {
                                ArrayList arraylist = new ArrayList();
                                arraylist.add(s3);
                                snps.put(s2, arraylist);
                            }
                        }

                    } else
                    {
                        as = (String[])Arrays.copyOfRange(as1, 1, as1.length);
                        String as2[] = as;
                        int j = as2.length;
                        for(int k = 0; k < j; k++)
                        {
                            String s4 = as2[k];
                            if(!"Ref".equals(s4))
                                sampleNames.add(formatName(s4));
                        }

                    }
                    Integer integer2 = integer;
                    Integer integer3 = integer = Integer.valueOf(integer.intValue() + 1);
                    Integer _tmp = integer2;
                }
            } while(true);
        }
        catch(IOException ioexception)
        {
            ioexception.printStackTrace();
        }
    }

    public String formatName(String s)
    {
        StringBuilder stringbuilder = new StringBuilder(s);
        for(int i = stringbuilder.indexOf(" "); i != -1; i = stringbuilder.indexOf(" "))
            stringbuilder.replace(i, i + 1, "_");

        for(int j = stringbuilder.indexOf("'"); j != -1; j = stringbuilder.indexOf("'"))
            stringbuilder.delete(j, j + 1);

        return stringbuilder.toString();
    }

    public List getSNPs()
    {
        return snpPos;
    }

    public String getSnp(Integer integer, String s)
    {
        if(snpPosNum.containsKey(integer))
        {
            Integer integer1 = (Integer)snpPosNum.get(integer);
            return (String)((List)snps.get(s)).get(integer1.intValue() - 1);
        } else
        {
            return null;
        }
    }

    public String getReferenceSnp(Integer integer)
    {
        return getSnp(integer, "Ref");
    }

    public List getSampleNames()
    {
        return sampleNames;
    }

    public String getRefCall(Integer integer)
    {
        return getSnp(integer, "Ref");
    }

    public void add(String s, Map map)
    {
        if(!sampleNames.contains(formatName(s)))
        {
            sampleNames.add(formatName(s));
            ArrayList arraylist = new ArrayList();
            for(Iterator iterator = snpPos.iterator(); iterator.hasNext();)
            {
                Integer integer = (Integer)iterator.next();
                if(map.containsKey(integer))
                    arraylist.add((String)map.get(integer));
                else
                    arraylist.add("N");
            }

            snps.put(formatName(s), arraylist);
        } else
        {
            System.err.println("could not add sample to snpTable");
            System.err.println((new StringBuilder()).append("sample name already exists: ").append(formatName(s)).toString());
        }
    }

    public String toString()
    {
        StringBuffer stringbuffer = new StringBuffer();
        stringbuffer.append("Position");
        stringbuffer.append("\t");
        stringbuffer.append("Ref");
        String s;
        for(Iterator iterator = sampleNames.iterator(); iterator.hasNext(); stringbuffer.append(s))
        {
            s = (String)iterator.next();
            stringbuffer.append("\t");
        }

        stringbuffer.append("\n");
        for(Iterator iterator1 = snpPos.iterator(); iterator1.hasNext(); stringbuffer.append("\n"))
        {
            Integer integer = (Integer)iterator1.next();
            stringbuffer.append(integer);
            stringbuffer.append("\t");
            stringbuffer.append(getReferenceSnp(integer));
            String s1;
            for(Iterator iterator2 = sampleNames.iterator(); iterator2.hasNext(); stringbuffer.append(getSnp(integer, s1)))
            {
                s1 = (String)iterator2.next();
                stringbuffer.append("\t");
            }

        }

        return stringbuffer.toString();
    }

    public String toAlignment()
    {
        return toAlignment(((Set) (new HashSet())));
    }

    public String toAlignment(Set set)
    {
        StringBuffer stringbuffer = new StringBuffer();
label0:
        for(Iterator iterator = sampleNames.iterator(); iterator.hasNext(); stringbuffer.append("\n"))
        {
            String s = (String)iterator.next();
            stringbuffer.append(">");
            stringbuffer.append(s.replace(" ", "_"));
            stringbuffer.append("\n");
            int i = 0;
            Iterator iterator1 = snpPos.iterator();
            do
            {
                if(!iterator1.hasNext())
                    continue label0;
                Integer integer = (Integer)iterator1.next();
                if(!set.contains(integer))
                {
                    String s1 = getSnp(integer, s);
                    if(".".equals(s1))
                        stringbuffer.append(getSnp(integer, "Ref"));
                    else
                        stringbuffer.append(s1);
                    if(++i % 80 == 0)
                        stringbuffer.append("\n");
                }
            } while(true);
        }

        return stringbuffer.toString();
    }

    public List compareTo(SNPTable snptable)
    {
        LinkedList linkedlist = new LinkedList();
        List list = snptable.getSampleNames();
        ArrayList arraylist = new ArrayList();
        Object obj = sampleNames.iterator();
        do
        {
            if(!((Iterator) (obj)).hasNext())
                break;
            String s = (String)((Iterator) (obj)).next();
            if(list.contains(formatName(s)))
                arraylist.add(formatName(s));
        } while(true);
        obj = snptable.getSNPs();
        TreeSet treeset = new TreeSet();
        treeset.addAll(snpPos);
        treeset.addAll(((java.util.Collection) (obj)));
        for(Iterator iterator = treeset.iterator(); iterator.hasNext();)
        {
            Integer integer = (Integer)iterator.next();
            if(snpPos.contains(integer) && ((List) (obj)).contains(integer))
            {
                Iterator iterator1 = arraylist.iterator();
                while(iterator1.hasNext()) 
                {
                    String s1 = (String)iterator1.next();
                    String s4 = getSnp(integer, s1);
                    String s7 = snptable.getSnp(integer, s1);
                    if(!s4.equals(s7))
                        linkedlist.add((new StringBuilder()).append("=\t").append(integer).append("\t").append(s1).append("\t").append(s4).append("/").append(s7).toString());
                }
            } else
            if(snpPos.contains(integer))
            {
                Iterator iterator2 = sampleNames.iterator();
                while(iterator2.hasNext()) 
                {
                    String s2 = (String)iterator2.next();
                    String s5 = getSnp(integer, s2);
                    if(!"N".equals(s5) && !".".equals(s5))
                        linkedlist.add((new StringBuilder()).append("+\t").append(integer).append("\t").append(s2).append("\t").append(s5).toString());
                }
            } else
            {
                Iterator iterator3 = snptable.getSampleNames().iterator();
                while(iterator3.hasNext()) 
                {
                    String s3 = (String)iterator3.next();
                    String s6 = snptable.getSnp(integer, s3);
                    if(!"N".equals(s6) && !".".equals(s6))
                        linkedlist.add((new StringBuilder()).append("-\t").append(integer).append("\t").append(s3).append("\t").append(s6).toString());
                }
            }
        }

        return linkedlist;
    }

    private String inputFile;
    private Map snpPosNum;
    private Map snps;
    private List snpPos;
    private List sampleNames;
}
