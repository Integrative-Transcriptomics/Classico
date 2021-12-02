// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   NewickTree.java

package datastructures;

import java.io.*;
import java.util.*;

// Referenced classes of package datastructures:
//            Node, SNPTable

public class NewickTree
{

    public NewickTree(String s)
    {
        nodeList = new ArrayList();
        inputFile = s;
        readNewickTree();
    }

    private void readNewickTree()
    {
        try
        {
            BufferedReader bufferedreader = new BufferedReader(new FileReader(inputFile));
            String s = "";
            String s1;
            String s2;
            for(s2 = ""; (s1 = bufferedreader.readLine()) != null; s2 = (new StringBuilder()).append(s2).append(s1).toString());
            root = readSubtree(s2.substring(0, s2.length() - 1));
            Iterator iterator = nodeList.iterator();
            do
            {
                if(!iterator.hasNext())
                    break;
                Node node = (Node)iterator.next();
                if(!node.getChildren().isEmpty())
                    node.setName("");
            } while(true);
        }
        catch(IOException ioexception)
        {
            ioexception.printStackTrace();
        }
    }

    private Node readSubtree(String s)
    {
        int i = s.indexOf('(');
        int j = s.lastIndexOf(')');
        int k = s.lastIndexOf(':');
        if(i != -1 && j != -1)
        {
            double d = 0.0D;
            String s1;
            if(k != -1 && k > j)
            {
                s1 = s.substring(j + 1, k);
                d = Double.parseDouble(s.substring(k + 1));
            } else
            {
                s1 = s.substring(j + 1);
            }
            String as[] = split(s.substring(i + 1, j));
            Node node1 = new Node(formatName(s1), d);
            String as1[] = as;
            int l = as1.length;
            for(int i1 = 0; i1 < l; i1++)
            {
                String s3 = as1[i1];
                Node node2 = readSubtree(s3);
                node1.addChild(node2);
                node2.setParent(node1);
            }

            node1.setId(nodeList.size() + 1);
            nodeList.add(node1);
            return node1;
        }
        if(i == j)
        {
            String s2 = s.substring(0, k);
            double d1 = Double.parseDouble(s.substring(k + 1));
            Node node = new Node(formatName(s2), d1);
            node.setId(nodeList.size() + 1);
            nodeList.add(node);
            return node;
        } else
        {
            throw new RuntimeException("unbalanced brackets");
        }
    }

    private static String[] split(String s)
    {
        ArrayList arraylist = new ArrayList();
        int i = 0;
        int j = 0;
        for(int k = 0; k < s.length(); k++)
            switch(s.charAt(k))
            {
            case 42: // '*'
            case 43: // '+'
            default:
                break;

            case 40: // '('
                j++;
                break;

            case 41: // ')'
                i++;
                break;

            case 44: // ','
                if(j == i)
                    arraylist.add(Integer.valueOf(k));
                break;
            }

        int l = arraylist.size() + 1;
        String as[] = new String[l];
        if(l == 1)
        {
            as[0] = s;
        } else
        {
            as[0] = s.substring(0, ((Integer)arraylist.get(0)).intValue());
            for(int i1 = 1; i1 < arraylist.size(); i1++)
                as[i1] = s.substring(((Integer)arraylist.get(i1 - 1)).intValue() + 1, ((Integer)arraylist.get(i1)).intValue());

            as[l - 1] = s.substring(((Integer)arraylist.get(arraylist.size() - 1)).intValue() + 1);
        }
        return as;
    }

    public String toString()
    {
        if(root != null)
        {
            String s = root.toNewickString();
            return (new StringBuilder()).append(s.substring(0, s.lastIndexOf(':'))).append(";").toString();
        } else
        {
            return "Empty tree cannot be exported.";
        }
    }

    public String toPositionString(int i, boolean flag, Node node)
    {
        Node node1;
        if(flag)
            node1 = node;
        else
            node1 = root;
        if(node1 != null)
        {
            String s = node1.toNewickPositionString(i, flag);
            return (new StringBuilder()).append(s.substring(0, s.lastIndexOf(':'))).append(";").toString();
        } else
        {
            return "Empty tree cannot be exported.";
        }
    }

    public List getNodeList()
    {
        return nodeList;
    }

    public void label(SNPTable snptable)
    {
label0:
        {
            Iterator iterator = snptable.getSNPs().iterator();
            do
            {
                if(!iterator.hasNext())
                    break label0;
                Integer integer = (Integer)iterator.next();
                Iterator iterator1 = snptable.getSampleNames().iterator();
label1:
                do
                {
                    if(!iterator1.hasNext())
                        break;
                    String s = (String)iterator1.next();
                    String s1 = snptable.getSnp(integer, s);
                    String s2 = snptable.getReferenceSnp(integer);
                    if(s1.equals("."))
                        s1 = s2;
                    Iterator iterator2 = nodeList.iterator();
                    Node node;
                    do
                    {
                        if(!iterator2.hasNext())
                            continue label1;
                        node = (Node)iterator2.next();
                    } while(!node.getName().equals(s));
                    node.setLabel(integer.intValue(), s1);
                } while(true);
            } while(true);
        }
    }

    public Node getRoot()
    {
        return root;
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

    public Node getNode(int i)
    {
        for(Iterator iterator = nodeList.iterator(); iterator.hasNext();)
        {
            Node node = (Node)iterator.next();
            if(node.getId() == i)
                return node;
        }

        return new Node("notFound", 0.0D);
    }

    public int getMinId(int i)
    {
        Node node = getNode(i);
        int j = 0x7fffffff;
        if(node.getChildren().isEmpty())
            return i;
        Iterator iterator = node.getChildren().iterator();
        do
        {
            if(!iterator.hasNext())
                break;
            Node node1 = (Node)iterator.next();
            if(node1.getId() < j)
                j = node1.getId();
        } while(true);
        return getMinId(j);
    }

    private String inputFile;
    private Node root;
    private List nodeList;
}
