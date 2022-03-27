// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Node.java

package datastructures;

import java.util.*;

public class Node {

    public boolean equals(Node node) {
        return id == node.id;
    }

    public Node(String s, double d) {
        label = new HashMap();
        children = new ArrayList();
        name = s;
        length = d;
    }

    public String toNewickString() {
        Locale.setDefault(Locale.ENGLISH);
        String s = "";
        if (!children.isEmpty()) {
            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
                Node node = (Node) iterator.next();
                if (s.equals(""))
                    s = node.toNewickString();
                else
                    s = (new StringBuilder()).append(s).append(",").append(node.toNewickString()).toString();
            }

            return (new StringBuilder()).append("(").append(s).append(")").append(name).append(":")
                    .append(String.format("%.3f", new Object[] {
                            Double.valueOf(length)
                    })).toString();
        } else {
            return (new StringBuilder()).append(name).append(":").append(String.format("%.3f", new Object[] {
                    Double.valueOf(length)
            })).toString();
        }
    }

    public String toNewickPositionString(int i, boolean flag) {
        Locale.setDefault(Locale.ENGLISH);
        String s = "";
        if (!children.isEmpty()) {
            for (Iterator iterator = children.iterator(); iterator.hasNext();) {
                Node node = (Node) iterator.next();
                if (s.equals(""))
                    s = node.toNewickPositionString(i, flag);
                else
                    s = (new StringBuilder()).append(s).append(",").append(node.toNewickPositionString(i, flag))
                            .toString();
            }

            String s1 = "";
            if (flag) {
                if (posSNP != null)
                    s1 = (new StringBuilder()).append("<>").append(id).append("_").append(posSNP).toString();
                else
                    s1 = (new StringBuilder()).append("").append(id).toString();
            } else {
                s1 = (new StringBuilder()).append(id).append(getPositionLabel(i)).toString();
            }
            return (new StringBuilder()).append("(").append(s).append(")").append(s1).append(":")
                    .append(String.format("%.3f", new Object[] {
                            Double.valueOf(length)
                    })).toString();
        }
        String s2 = "";
        if (flag) {
            String s3 = getPositionLabel(i);
            if (posSNP != null)
                s3 = (new StringBuilder()).append("<>").append(name).append("_").append(id).append(s3).toString();
            else
                s3 = (new StringBuilder()).append(name).append("_").append(id).append(s3).toString();
            return (new StringBuilder()).append(s3).append(":").append(String.format("%.3f", new Object[] {
                    Double.valueOf(length)
            })).toString();
        } else {
            String s4 = getPositionLabel(i);
            return (new StringBuilder()).append(name).append("_").append(id).append(s4).append(":")
                    .append(String.format("%.3f", new Object[] {
                            Double.valueOf(length)
                    })).toString();
        }
    }

    private String getPositionLabel(int i) {
        String s = "";
        if (i != -1) {
            Object aobj[] = ((Set) label.get(Integer.valueOf(i))).toArray();
            int j = aobj.length;
            for (int k = 0; k < j; k++) {
                Object obj = aobj[k];
                s = (new StringBuilder()).append(s).append("_").append(obj).toString();
            }

        }
        return s;
    }

    public String toString() {
        return (new StringBuilder()).append(name).append(":").append(String.format("%.8f", new Object[] {
                Double.valueOf(length)
        })).append("-").append(id).toString();
    }

    public void addChild(Node node) {
        children.add(node);
    }

    public int hashCode() {
        int i = 1;
        i = 31 * i + id;
        return i;
    }

    public List getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public double getLength() {
        return length;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node node) {
        parent = node;
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        id = i;
    }

    // public Integer getTotalNumberChildren() {

    // if (children.size() == 0) {
    // return 1;
    // } else {
    // List childrenOfNodeList = children;
    // Iterator childrenOfNode = childrenOfNodeList.iterator();
    // Integer numberOfChildren = 0;
    // while (childrenOfNode.hasNext()) {
    // Node childOfNode = (Node) childrenOfNode.next();
    // numberOfChildren += childOfNode.getTotalNumberChildren();
    // }
    // return numberOfChildren;
    // }

    // }

    public Map getLabel() {
        return label;
    }

    public void setLabel(int i, String s) {
        if (label.containsKey(Integer.valueOf(i))) {
            ((Set) label.get(Integer.valueOf(i))).add(s);
        } else {
            HashSet hashset = new HashSet();
            hashset.add(s);
            label.put(Integer.valueOf(i), hashset);
        }
        if (parent != null)
            parent.setLabel(i, s);
    }

    public void setPosSNP(String s) {
        posSNP = s;
    }

    public void setName(String s) {
        name = s;
    }

    private Map label;
    private String name;
    public String posSNP;
    private int id;
    private double length;
    private Node parent;
    private List children;
}
