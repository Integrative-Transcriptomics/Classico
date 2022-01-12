// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Project.java

package project;

import datastructures.*;
import java.io.*;
import java.util.*;

public class Project {

    public Project(String s, String s1, String s2) {
        claden = new HashMap();
        supportTree = new HashMap();
        notSupportTree = new HashMap();
        supportSplitKeys = new HashMap();
        notSupportSplitKeys = new HashMap();
        snp = new SNPTable(s);
        tree = new NewickTree(s1);
        filepath = new NewFile(s2);
        // filepath.createDir("Ergebnis");
    }

    public Project(SNPTable snptable, NewickTree newicktree, String s) {
        claden = new HashMap();
        supportTree = new HashMap();
        notSupportTree = new HashMap();
        supportSplitKeys = new HashMap();
        notSupportSplitKeys = new HashMap();
        snp = snptable;
        tree = newicktree;
        filepath = new NewFile(s);
        // filepath.createDir("Ergebnis");
    }

    public void compute() {
        int i;
        for (Iterator iterator = snp.getSNPs().iterator(); iterator.hasNext(); evaluateCladen(i)) {
            i = ((Integer) iterator.next()).intValue();
            label(tree, Integer.valueOf(i));
            computeCladen(tree.getRoot(), i, true);
        }

        splitKeys(filepath.createFile("supportSplitKeys.txt"), true);
        splitKeys(filepath.createFile("notSupportSplitKeys.txt"), false);
        nameAndID(filepath.createFile("IDdistribution.txt"));
    }

    public void computeCladen(Node node, int i, boolean flag) {
        if (node.getLabel().containsKey(Integer.valueOf(i))) {
            Set set = (Set) node.getLabel().get(Integer.valueOf(i));
            switch (set.size()) {
                case 0: // '\0'
                    System.err.println("Project:86 - Key is given, but no string provided");
                    break;

                case 1: // '\001'
                    setClade(i, node, set.toString(), claden);
                    break;

                // case 2: // '\002'
                // if (flag && ((Set) node.getLabel().get(Integer.valueOf(i))).contains("N")) {
                // if (node.getId() != tree.getRoot().getId()) {
                // set.remove("N");
                // // set.remove(".");
                // setClade(i, node, set.toString(), claden);
                // }
                // break;
                // }
                // // fall through

                default:
                    if (!node.getChildren().isEmpty()) {
                        Node node1;
                        for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext(); computeCladen(node1,
                                i, flag))
                            node1 = (Node) iterator.next();

                    } else {
                        System.err.println("Project:110 - Leaf has two bases");
                    }
                    break;
            }
        } else {
            System.err.println((new StringBuilder()).append("Project:130 - Key not found").append(i).append("_")
                    .append(node.getId()).append("_").append(node.getName()).toString());
        }
    }

    public void evaluateCladen(int i) {
        HashMap hashmap = (HashMap) claden.get(Integer.valueOf(i));
        ArrayList arraylist = new ArrayList();
        if (hashmap != null) {
            String s;
            for (Iterator iterator = hashmap.keySet().iterator(); iterator.hasNext(); arraylist
                    .add(Integer.valueOf(((List) hashmap.get(s)).size())))
                s = (String) iterator.next();

            // Collections.sort(arraylist);
            int j = ((Integer) arraylist.get(arraylist.size() - 1)).intValue();
            if (j == ((Integer) arraylist.get(0)).intValue())
                j++;
            Iterator iterator1 = hashmap.keySet().iterator();
            do {
                if (!iterator1.hasNext())
                    break;
                String s1 = (String) iterator1.next();
                List list = (List) hashmap.get(s1);
                if (!"[.]".equals(s1) & !"[N]".equals(s1))
                    if (list.size() == 1) {
                        Iterator iterator2 = list.iterator();
                        while (iterator2.hasNext()) {
                            Node node = (Node) iterator2.next();
                            setClade(i, node, s1, supportTree);
                        }
                    } else {
                        Iterator iterator3 = list.iterator();
                        while (iterator3.hasNext()) {
                            Node node1 = (Node) iterator3.next();
                            setClade(i, node1, s1, notSupportTree);
                        }
                    }
            } while (true);
        }
    }

    public void splitKeys(String s, boolean flag) {
        Map map;
        if (flag) {
            for (Iterator iterator = supportTree.keySet().iterator(); iterator.hasNext();) {
                int i = ((Integer) iterator.next()).intValue();
                Iterator iterator2 = ((HashMap) supportTree.get(Integer.valueOf(i))).keySet().iterator();
                while (iterator2.hasNext()) {
                    String s1 = (String) iterator2.next();
                    Iterator iterator6 = ((List) ((HashMap) supportTree.get(Integer.valueOf(i))).get(s1)).iterator();
                    while (iterator6.hasNext()) {
                        Node node = (Node) iterator6.next();
                        setInt(node.getId(), i, supportSplitKeys);
                    }
                }
            }

            map = supportSplitKeys;
        } else {
            for (Iterator iterator1 = notSupportTree.keySet().iterator(); iterator1.hasNext();) {
                int j = ((Integer) iterator1.next()).intValue();
                Iterator iterator3 = ((HashMap) notSupportTree.get(Integer.valueOf(j))).keySet().iterator();
                while (iterator3.hasNext()) {
                    String s2 = (String) iterator3.next();
                    Iterator iterator7 = ((List) ((HashMap) notSupportTree.get(Integer.valueOf(j))).get(s2)).iterator();
                    while (iterator7.hasNext()) {
                        Node node1 = (Node) iterator7.next();
                        setInt(node1.getId(), j, notSupportSplitKeys);
                    }
                }
            }

            map = notSupportSplitKeys;
        }
        try {
            FileWriter filewriter = new FileWriter(s);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            ArrayList arraylist = new ArrayList();
            int k;
            for (Iterator iterator4 = map.keySet().iterator(); iterator4.hasNext(); arraylist.add(Integer.valueOf(k)))
                k = ((Integer) iterator4.next()).intValue();

            Collections.sort(arraylist);
            for (Iterator iterator5 = arraylist.iterator(); iterator5.hasNext(); bufferedwriter.write("\n")) {
                int l = ((Integer) iterator5.next()).intValue();
                Node node2 = tree.getNode(l);
                if (node2.getId() != tree.getRoot().getId())
                    bufferedwriter.write((new StringBuilder()).append(node2.getParent().getId()).append("->").append(l)
                            .append("\t").toString());
                else
                    bufferedwriter.write((new StringBuilder()).append("Root ").append(l).append("\t").toString());
                bufferedwriter.write((new StringBuilder()).append(((List) map.get(Integer.valueOf(l))).size())
                        .append("\t").toString());
                Collections.sort((List) map.get(Integer.valueOf(l)));
                int i1;
                for (Iterator iterator8 = ((List) map.get(Integer.valueOf(l))).iterator(); iterator8
                        .hasNext(); bufferedwriter.write((new StringBuilder()).append(i1).append(":")
                                .append(((Set) tree.getNode(l).getLabel().get(Integer.valueOf(i1))).toString())
                                .toString()))
                    i1 = ((Integer) iterator8.next()).intValue();

            }

            bufferedwriter.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    public void label(NewickTree newicktree, Integer integer) {
        Object obj = snp.getSampleNames().iterator();
        label0: do {
            if (!((Iterator) (obj)).hasNext())
                break;
            String s = (String) ((Iterator) (obj)).next();
            String s1 = snp.getSnp(integer, s);
            String s2 = snp.getReferenceSnp(integer);
            // if (s1.equals(s2))
            // s1 = ".";
            Iterator iterator1 = newicktree.getNodeList().iterator();
            Node node1;
            do {
                if (!iterator1.hasNext())
                    continue label0;
                node1 = (Node) iterator1.next();
            } while (!node1.getName().equals(s));
            node1.setLabel(integer.intValue(), s1);
        } while (true);
        obj = new ArrayList();
        Iterator iterator = newicktree.getNodeList().iterator();
        do {
            if (!iterator.hasNext())
                break;
            Node node = (Node) iterator.next();
            if (!node.getLabel().containsKey(integer) && node.getChildren().isEmpty())
                ((List) (obj)).add(node);
        } while (true);
        if (((List) (obj)).isEmpty())
            ;
    }

    public void setClade(int i, Node node, String s, Map map) {
        if (map.containsKey(Integer.valueOf(i))) {
            if (((HashMap) map.get(Integer.valueOf(i))).containsKey(s)) {
                ((List) ((HashMap) map.get(Integer.valueOf(i))).get(s)).add(node);
            } else {
                ArrayList arraylist = new ArrayList();
                arraylist.add(node);
                ((HashMap) map.get(Integer.valueOf(i))).put(s, arraylist);
            }
        } else {
            HashMap hashmap = new HashMap();
            ArrayList arraylist1 = new ArrayList();
            arraylist1.add(node);
            hashmap.put(s, arraylist1);
            map.put(Integer.valueOf(i), hashmap);
        }
    }

    public void getResults() {
        toFile(filepath.createFile("claden.txt"), claden);
        toFile(filepath.createFile("supportTree.txt"), supportTree);
        toFile(filepath.createFile("notSupportTree.txt"), notSupportTree);
    }

    public void toFile(String s, Map map) {
        try {
            FileWriter filewriter = new FileWriter(s);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            for (Iterator iterator = map.keySet().iterator(); iterator.hasNext(); bufferedwriter.write("\n")) {
                int i = ((Integer) iterator.next()).intValue();
                bufferedwriter.write((new StringBuilder()).append(i).append(":\n").toString());
                for (Iterator iterator1 = ((HashMap) map.get(Integer.valueOf(i))).keySet().iterator(); iterator1
                        .hasNext();) {
                    String s1 = (String) iterator1.next();
                    bufferedwriter.write((new StringBuilder()).append(s1).append(":\n").toString());
                    Iterator iterator2 = ((List) ((HashMap) map.get(Integer.valueOf(i))).get(s1)).iterator();
                    while (iterator2.hasNext()) {
                        Node node = (Node) iterator2.next();
                        bufferedwriter.write((new StringBuilder()).append(node.getId()).append("-")
                                .append(node.getName()).append("-").append(node.getLabel().get(Integer.valueOf(i)))
                                .append(getChildren(node, i)).append("\n").toString());
                    }
                }

            }

            bufferedwriter.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    private String getChildren(Node node, int i) {
        String s = ":";
        for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext();) {
            Node node1 = (Node) iterator.next();
            s = (new StringBuilder()).append(s).append(node1.getId()).append("-").append(node1.getName()).append("-")
                    .append(node1.getLabel().get(Integer.valueOf(i))).append(",").toString();
        }

        if (s.equals(":"))
            return "";
        else
            return s.substring(0, s.length() - 1);
    }

    public void setInt(int i, int j, Map map) {
        if (map.containsKey(Integer.valueOf(i))) {
            ((List) map.get(Integer.valueOf(i))).add(Integer.valueOf(j));
        } else {
            ArrayList arraylist = new ArrayList();
            arraylist.add(Integer.valueOf(j));
            map.put(Integer.valueOf(i), arraylist);
        }
    }

    public void showPositions(List list) {
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            int i = ((Integer) iterator.next()).intValue();
            if (snp.getSNPs().contains(Integer.valueOf(i)) || i == -1)
                showCladeAtPosition(i);
            else
                System.out.println(
                        (new StringBuilder()).append("Position ").append(i).append(" not found in table.").toString());
        }

    }

    public void showCladeAtPosition(int i) {
        String s;
        if (i == -1)
            s = filepath.createFile("IDTree.nwk");
        else
            s = filepath.createFile((new StringBuilder()).append("labeledTree").append(i).append(".nwk").toString());
        try {
            FileWriter filewriter = new FileWriter(s);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            bufferedwriter.write(tree.toPositionString(i, false, null));
            bufferedwriter.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    public void nameAndID(String s) {
        try {
            FileWriter filewriter = new FileWriter(s);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            for (Iterator iterator = tree.getNodeList().iterator(); iterator.hasNext();) {
                Node node = (Node) iterator.next();
                if (node.getChildren().isEmpty()) {
                    bufferedwriter.write((new StringBuilder()).append(node.getId()).append("\t").append(node.getName())
                            .append("\n").toString());
                } else {
                    String s1 = "";
                    for (Iterator iterator1 = node.getChildren().iterator(); iterator1.hasNext();) {
                        Node node1 = (Node) iterator1.next();
                        s1 = (new StringBuilder()).append(s1).append(node1.getId()).append(",").toString();
                    }

                    bufferedwriter.write(
                            (new StringBuilder()).append(node.getId()).append("\t").append(s1).append("\n").toString());
                }
            }

            bufferedwriter.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    public void getUniqueSubtreeSNPs(String s, int i) {
        Object obj = new ArrayList();
        Node node = tree.getNode(i);
        obj = giveSubtreeNodes(node);
        getCommonSNP(s, ((List) (obj)));
    }

    private List giveSubtreeNodes(Node node) {
        ArrayList arraylist = new ArrayList();
        arraylist.add(node);
        Node node1;
        for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext(); arraylist
                .addAll(giveSubtreeNodes(node1)))
            node1 = (Node) iterator.next();

        return arraylist;
    }

    private String getStrains(int i) {
        Node node = tree.getNode(i);
        String s = "";
        if (node.getChildren().isEmpty())
            return (new StringBuilder()).append(node.getName()).append(", ").toString();
        for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext();) {
            Node node1 = (Node) iterator.next();
            s = (new StringBuilder()).append(s).append(getStrains(node1.getId())).toString();
        }

        return s;
    }

    public void commonSNPs(List list) {
        String s = filepath.createFile("CommonSamples.txt");
        ArrayList arraylist = new ArrayList();
        ArrayList arraylist1 = new ArrayList();
        int i;
        for (Iterator iterator = list.iterator(); iterator.hasNext(); arraylist.add(tree.getNode(i)))
            i = ((Integer) iterator.next()).intValue();

        Iterator iterator1 = arraylist.iterator();
        label0: do {
            if (!iterator1.hasNext())
                break;
            Node node = (Node) iterator1.next();
            Node node1 = node;
            if (node.getChildren().isEmpty()) {
                if (node.getParent() != null)
                    do {
                        if (!ancestor(arraylist, node1.getParent()))
                            break;
                        node1 = node1.getParent();
                    } while (node1 != null);
                if (ancestor(arraylist, node1)) {
                    List list1 = getAllNodesOfSubtree(node1);
                    Iterator iterator2 = list1.iterator();
                    do {
                        Node node2;
                        do {
                            if (!iterator2.hasNext())
                                continue label0;
                            node2 = (Node) iterator2.next();
                        } while (arraylist1.contains(node2));
                        arraylist1.add(node2);
                    } while (true);
                }
                arraylist1.add(node);
            } else {
                System.out.println((new StringBuilder()).append("Knoten ").append(node.getId())
                        .append(" ist eine Klade kein Individuum").toString());
            }
        } while (true);
        getCommonSNP(s, arraylist1);
    }

    public void getCommonSNP(String s, List list) {
        try {
            FileWriter filewriter = new FileWriter(s);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            bufferedwriter.write(">SupportTreeSNPs\n");
            ArrayList arraylist = new ArrayList(supportTree.keySet());
            Collections.sort(arraylist);
            String s1 = "";
            for (Iterator iterator = arraylist.iterator(); iterator.hasNext();) {
                int i = ((Integer) iterator.next()).intValue();
                Iterator iterator2 = ((HashMap) supportTree.get(Integer.valueOf(i))).keySet().iterator();
                while (iterator2.hasNext()) {
                    String s2 = (String) iterator2.next();
                    Iterator iterator4 = ((List) ((HashMap) supportTree.get(Integer.valueOf(i))).get(s2)).iterator();
                    while (iterator4.hasNext()) {
                        Node node = (Node) iterator4.next();
                        if (list.contains(node))
                            if (node.getChildren().isEmpty()) {
                                bufferedwriter.write((new StringBuilder()).append(i).append("\t").append(s2)
                                        .append("\t").append(node.getName()).append("\n").toString());
                            } else {
                                s1 = getStrains(node.getId());
                                bufferedwriter.write((new StringBuilder()).append(i).append("\t").append(s2)
                                        .append("\t").append(s1.substring(0, s1.length() - 2)).append("\n").toString());
                            }
                    }
                }
            }

            bufferedwriter.write(">NotSupportTreeSNPs\n");
            arraylist = new ArrayList(notSupportTree.keySet());
            Collections.sort(arraylist);
            s1 = "";
            for (Iterator iterator1 = arraylist.iterator(); iterator1.hasNext();) {
                int j = ((Integer) iterator1.next()).intValue();
                Iterator iterator3 = ((HashMap) notSupportTree.get(Integer.valueOf(j))).keySet().iterator();
                while (iterator3.hasNext()) {
                    String s3 = (String) iterator3.next();
                    if (list.containsAll((Collection) ((HashMap) notSupportTree.get(Integer.valueOf(j))).get(s3))) {
                        for (Iterator iterator5 = ((List) ((HashMap) notSupportTree.get(Integer.valueOf(j))).get(s3))
                                .iterator(); iterator5.hasNext();) {
                            Node node1 = (Node) iterator5.next();
                            s1 = (new StringBuilder()).append(s1).append(getStrains(node1.getId())).toString();
                        }

                        bufferedwriter.write((new StringBuilder()).append(j).append("\t").append(s3).append("\t")
                                .append(s1.substring(0, s1.length() - 2)).append("\n").toString());
                    }
                }
            }

            bufferedwriter.close();
        } catch (IOException ioexception) {
        }
    }

    public boolean ancestor(List list, Node node) {
        boolean flag = true;
        Iterator iterator = node.getChildren().iterator();
        do {
            if (!iterator.hasNext())
                break;
            Node node1 = (Node) iterator.next();
            if (!flag)
                break;
            if (node1.getChildren().isEmpty()) {
                boolean flag1 = false;
                Iterator iterator1 = list.iterator();
                do {
                    if (!iterator1.hasNext())
                        break;
                    Node node2 = (Node) iterator1.next();
                    flag1 = node1.getId() == node2.getId();
                } while (!flag1);
                if (!flag1)
                    flag = false;
            } else {
                flag = ancestor(list, node1);
            }
        } while (true);
        return flag;
    }

    public List getAllNodesOfSubtree(Node node) {
        ArrayList arraylist = new ArrayList();
        if (node.getChildren().isEmpty()) {
            arraylist.add(node);
        } else {
            arraylist.add(node);
            Node node1;
            for (Iterator iterator = node.getChildren().iterator(); iterator.hasNext(); arraylist
                    .addAll(getAllNodesOfSubtree(node1)))
                node1 = (Node) iterator.next();

        }
        return arraylist;
    }

    public void treeSNPs(int i) {
        label0: {
            List list = tree.getNodeList();
            Node node1;
            for (Iterator iterator = list.iterator(); iterator.hasNext(); node1.setPosSNP(null))
                node1 = (Node) iterator.next();

            Node node;
            for (node = (Node) list.get(0); node.getParent() != null; node = node.getParent())
                ;
            try {
                HashMap hashmap = new HashMap();
                if (supportTree.containsKey(Integer.valueOf(i)))
                    hashmap = (HashMap) supportTree.get(Integer.valueOf(i));
                if (notSupportTree.containsKey(Integer.valueOf(i)))
                    hashmap.putAll((Map) notSupportTree.get(Integer.valueOf(i)));
                if (hashmap.isEmpty()) {
                    System.err.println((new StringBuilder()).append("Kein Key mit dem Wert ").append(i)
                            .append(" in notSupportTree und SupportTree enthalten.").toString());
                    break label0;
                }
                String s = filepath
                        .createFile((new StringBuilder()).append("Clade-Tree").append(i).append(".nwk").toString());
                FileWriter filewriter = new FileWriter(s);
                Object obj = hashmap.keySet().iterator();
                do {
                    if (!((Iterator) (obj)).hasNext())
                        break;
                    String s1 = (String) ((Iterator) (obj)).next();
                    Iterator iterator1 = ((List) hashmap.get(s1)).iterator();
                    label1: do {
                        if (!iterator1.hasNext())
                            break;
                        Node node2 = (Node) iterator1.next();
                        Iterator iterator2 = list.iterator();
                        Node node3;
                        do {
                            if (!iterator2.hasNext())
                                continue label1;
                            node3 = (Node) iterator2.next();
                        } while (node2.getId() != node3.getId());
                        node3.setPosSNP(s1);
                    } while (true);
                } while (true);
                obj = new BufferedWriter(filewriter);
                ((BufferedWriter) (obj)).write(tree.toPositionString(i, true, node));
                ((BufferedWriter) (obj)).close();
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
            }
        }
    }

    public NewFile getFilepath() {
        return filepath;
    }

    public Map getSupportTree() {
        return supportTree;
    }

    public SNPTable snp;
    private NewickTree tree;
    private NewFile filepath;
    private Map claden;
    private Map supportTree;
    private Map notSupportTree;
    private Map supportSplitKeys;
    private Map notSupportSplitKeys;
}
