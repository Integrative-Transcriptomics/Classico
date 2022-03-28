// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Project.java

package project;

import datastructures.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Project {

    public Project(String s, String s1, String s2) {
        cladenGlobalHashMap = new HashMap();
        supportTree = new HashMap();
        notSupportTree = new HashMap();
        supportSplitKeys = new HashMap();
        notSupportSplitKeys = new HashMap();
        mainSNPHashMap = new SNPTable(s);
        tree = new NewickTree(s1);
        filepath = new NewFile(s2);
        // filepath.createDir("Ergebnis");
    }

    public Project(SNPTable snptable, NewickTree newicktree, String s) {
        cladenGlobalHashMap = new HashMap();
        supportTree = new HashMap();
        notSupportTree = new HashMap();
        supportSplitKeys = new HashMap();
        notSupportSplitKeys = new HashMap();
        mainSNPHashMap = snptable;
        tree = newicktree;
        filepath = new NewFile(s);
        // filepath.createDir("Ergebnis");
    }

    public void compute() {
        int snpPosition;
        for (Iterator snpIterator = mainSNPHashMap.getSNPs().iterator(); snpIterator
                .hasNext(); evaluateCladen(snpPosition)) {
            snpPosition = ((Integer) snpIterator.next()).intValue();
            label(tree, Integer.valueOf(snpPosition));
            computeCladen(tree.getRoot(), snpPosition, true);
        }

        splitKeys(filepath.createFile("supportSplitKeys.txt"), true);
        splitKeys(filepath.createFile("notSupportSplitKeys.txt"), false);
        nameAndID(filepath.createFile("IDdistribution.txt"));
    }

    /**
     * TODO: THIS IS THE FUNCTION THAT NEEDS TO BE CHANGED
     * 
     * @param currentCladeRoot
     * @param snpPosition
     * @param flag
     */
    public void computeCladen(Node currentCladeRoot, int snpPosition, boolean flag) {
        if (currentCladeRoot.getLabel().containsKey(Integer.valueOf(snpPosition))) {
            // TODO: Take a look if we can avoid this set
            Object tempList = currentCladeRoot.getLabel().get(Integer.valueOf(snpPosition));
            // System.out.println(tempList);
            Set set = (Set) tempList;
            // System.out.println(currentCladeRoot.getLabel().get(Integer.valueOf(snpPosition)));
            // System.out.println("I am here" + set);
            switch (set.size()) {
                case 0: // '\0'
                    System.err.println("Project:86 - Key is given, but no string provided");
                    break;

                case 1: // '\001'
                    setClade(snpPosition, currentCladeRoot, set.toString(), cladenGlobalHashMap);
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
                    if (!currentCladeRoot.getChildren().isEmpty()) {
                        Node node1;
                        for (Iterator iterator = currentCladeRoot.getChildren().iterator(); iterator
                                .hasNext(); computeCladen(node1,
                                        snpPosition, flag))
                            node1 = (Node) iterator.next();

                    } else {
                        System.err.println("Project:110 - Leaf has two bases");
                    }
                    break;
            }
        } else {
            System.err.println((new StringBuilder()).append("Project:130 - Key not found").append(snpPosition)
                    .append("_")
                    .append(currentCladeRoot.getId()).append("_").append(currentCladeRoot.getName()).toString());
        }
    }

    /**
     * 
     * This recieves already the computed clade.
     * It is executed at the end of each iteration in a for loop
     * 
     * @param snpPosition
     */
    public void evaluateCladen(int snpPosition) {
        HashMap hashmap = (HashMap) cladenGlobalHashMap.get(Integer.valueOf(snpPosition));
        ArrayList countsAllelesFromSet = new ArrayList();
        if (hashmap != null) {
            String s;
            for (Iterator iterator = hashmap.keySet().iterator(); iterator.hasNext(); countsAllelesFromSet
                    .add(Integer.valueOf(((List) hashmap.get(s)).size())))
                s = (String) iterator.next();
            // System.out.println(countsAllelesFromSet);
            // Collections.sort(arraylist);
            int j = ((Integer) countsAllelesFromSet.get(countsAllelesFromSet.size() - 1)).intValue();
            if (j == ((Integer) countsAllelesFromSet.get(0)).intValue())
                j++;
            Iterator SNPAlleleIterator = hashmap.keySet().iterator();
            do {
                if (!SNPAlleleIterator.hasNext())
                    break;
                String SNPAllele = (String) SNPAlleleIterator.next();

                // System.out.println("test S1: " + SNPAllele);
                List nodesWithSNP = (List) hashmap.get(SNPAllele);
                // System.out.println(nodesWithSNP);
                if (!"[.]".equals(SNPAllele))
                    if (nodesWithSNP.size() == 1) {
                        Iterator nodesIteratorSupport = nodesWithSNP.iterator();
                        while (nodesIteratorSupport.hasNext()) {
                            Node node = (Node) nodesIteratorSupport.next();
                            setClade(snpPosition, node, SNPAllele, supportTree);
                        }
                    } else {
                        // Iterator nodesIteratorPossiblyNotSupport = nodesWithSNP.iterator();
                        // TODO: could we include here a check of the distribution among the tree?
                        System.out.println(snpPosition + SNPAllele);
                        List recheckDistribution = new ArrayList(nodesWithSNP);
                        // Checks for cases that might be labelled as supporting
                        boolean recheckSupportTree = checkCentipede(recheckDistribution,
                                tree.getRoot());
                        System.out.println("As supportive:" + recheckSupportTree);
                        // This iterator referes to the one used to write down the SNPs
                        Iterator nodesIteratorPossiblyNotSupportWrite = nodesWithSNP.iterator();

                        while (nodesIteratorPossiblyNotSupportWrite.hasNext()) {
                            Node node1 = (Node) nodesIteratorPossiblyNotSupportWrite.next();
                            if (recheckSupportTree) {
                                setClade(snpPosition, node1, SNPAllele, supportTree);

                            } else {
                                setClade(snpPosition, node1, SNPAllele, notSupportTree);

                            }

                        }
                    }
            } while (true);
        }
    }

    /**
     * Object to pass recursion information
     */
    class RecursionInformation {
        boolean isSupportive; // Is the SNP supporting
        List nodesToSearch; // Nodes left to search
        List foundNodes; // Nodes already found
        Integer passedInternal; // Number of nodes that have been passed
        List cladeRootStart; // Root of clade where the node could be supporting

        RecursionInformation(boolean supportive, List toSearch, List found, Integer internalCount, List cladeRoot) {
            isSupportive = supportive;
            nodesToSearch = toSearch;
            foundNodes = found;
            passedInternal = internalCount;
            cladeRootStart = cladeRoot;
        }
    }

    /**
     * Trying to implement a test to catch the centipede example,
     * where one single mutation could make a whole allele non-supporting.
     * 
     * @param nodesToTest: list of nodes that should be tested
     * @param rootNode:    start of the tree
     * @return should the node be labelled as supporting?
     */
    public boolean checkCentipede(List nodesToTest, Node rootNode) {
        List collectedNodes = new ArrayList();
        // Create recursion information object
        RecursionInformation startRecursion = new RecursionInformation(false, nodesToTest, collectedNodes, 0,
                new ArrayList());
        RecursionInformation result = checkCentipedeHelper(startRecursion, rootNode, 0);
        System.out.println("Recursive Count: " + result.passedInternal);
        return result.isSupportive & result.passedInternal == 0;
    }

    /*
     * public boolean checkCentipedeHelper(List nodesLeft, List collectedNodes, Node
     * currentCladeRoot) {
     * List nodesIncoming = new ArrayList<>(nodesLeft);
     * // List nodesIncoming = nodesLeft;
     * 
     * Integer nodesIncomingSize = nodesLeft.size();
     * // System.out.println(nodesIncoming);
     * boolean foundInList = nodesLeft.contains(currentCladeRoot);
     * 
     * if (foundInList) {
     * nodesIncoming.remove(currentCladeRoot);
     * collectedNodes.add(currentCladeRoot);
     * }
     * System.out.println(nodesIncoming);
     * 
     * if (nodesIncoming.size() == 0) {
     * return true;
     * } else if (currentCladeRoot.getChildren().size() == 0) {
     * return false;
     * } else if (nodesIncoming.size() == nodesIncomingSize & collectedNodes.size()
     * > 0) {
     * return false;
     * } else {
     * boolean childResult = false;
     * Iterator childrenOfNode = currentCladeRoot.getChildren().iterator();
     * while (childrenOfNode.hasNext()) {
     * Node newChild = (Node) childrenOfNode.next();
     * System.out.println(nodesIncoming);
     * List nodesLeftChild = new ArrayList<>(nodesIncoming);
     * System.out.println(nodesLeftChild);
     * childResult = checkCentipedeHelper(nodesLeftChild, collectedNodes, newChild);
     * if (childResult) {
     * break;
     * }
     * }
     * 
     * return childResult;
     * }
     * }
     */
    /**
     * Recursion function for the identification of supporting late mutations
     * 
     * @param recursionState   // current results of recursion
     * @param currentCladeRoot // current node
     * @param childrenNextNode // Number of total leaves in sibling node
     * @return Recursion function after trespasing the tree.
     */
    public RecursionInformation checkCentipedeHelper(RecursionInformation recursionState, Node currentCladeRoot,
            Integer childrenNextNode) {

        // System.out.println(recursionState.passedInternal);
        // System.out.println(recursionState.foundNodes);
        // List nodesLeft = recursionState.nodesToSearch;

        // Initialize objects to avoid modifying wrong lists

        Integer recursiveCount = recursionState.passedInternal;
        List collectedNodes = new ArrayList(recursionState.foundNodes);
        List nodesIncoming = new ArrayList(recursionState.nodesToSearch);
        List cladeRootStart = new ArrayList(recursionState.cladeRootStart);

        // System.out.println(recursiveCount);
        // System.out.println(nodesLeft);
        // System.out.println(collectedNodes);
        // System.out.println(nodesIncoming);
        // List nodesIncoming = nodesLeft;
        // System.out.println(nodesIncoming);
        // if (nodesIncoming.size() == 0) {
        // return recursionState;
        // }

        // Is the current node found in the list? Is it a leaf?
        boolean foundInList = nodesIncoming.contains(currentCladeRoot);
        boolean isLeaf = currentCladeRoot.getChildren().size() == 0;
        // System.out.println("Enter rec state" + recursionState.isSupportive);

        if (foundInList) {
            // recursiveCount = collectedNodes.size() > 0 ?
            // Math.max(recursionState.passedInternal - 1, 0)
            // recursiveCount = collectedNodes.size() > 0 ?
            // Math.max(recursionState.passedInternal - 1, 0)
            recursiveCount = collectedNodes.size() > 0 ? recursionState.passedInternal
                    : 0;
            nodesIncoming.remove(currentCladeRoot);
            collectedNodes.add(currentCladeRoot);
            // System.out.println("State nodes incoming: " + nodesIncoming);
            return new RecursionInformation(nodesIncoming.size() == 0, nodesIncoming, collectedNodes, recursiveCount,
                    cladeRootStart);
        } else {
            if (isLeaf) {
                // Add recursiveCount only if we have already started "collecting" nodes,
                // if there are still nodes to look for
                // and if the next child is not a leaf
                // The last case allows for the centipede example to also have late mutation in
                // a paired leaf.
                if (cladeRootStart.size() > 0 & nodesIncoming.size() > 0 & childrenNextNode > 1) {
                    recursiveCount = recursiveCount + 1;
                }
                // System.out.println(nodesIncoming);
                // System.out.println(nodesIncoming.size() > 0);

                return new RecursionInformation(recursionState.isSupportive, nodesIncoming, collectedNodes,
                        recursiveCount, cladeRootStart);
            } else {
                // if (collectedNodes.size() > 0 & nodesIncoming.size() > 0) {
                // recursiveCount = recursiveCount + 1;
                // return new RecursionInformation(false, nodesIncoming, collectedNodes,
                // recursiveCount);
                // }
                // System.out.println(getTotalNumberChildren(currentCladeRoot));
                List childrenOfNodeList = currentCladeRoot.getChildren();
                // System.out.println(getTotalNumberChildren(currentCladeRoot));
                // System.out.println(childrenOfNodeList);
                Collections.sort(childrenOfNodeList,
                        // (Node node1, Node node2) -> node1.getChildren().size() -
                        // (node2.getChildren().size()));
                        (Node node1, Node node2) -> getTotalNumberChildren(node1) - getTotalNumberChildren(node2));

                // Collections.reverse(childrenOfNodeList);
                List<Integer> copyChildren = new ArrayList<Integer>();
                childrenOfNodeList.forEach((child) -> copyChildren.add(getTotalNumberChildren((Node) child)));
                // .collect(Collectors.toList());
                // System.out.println(copyChildren);
                Iterator childrenOfNode = childrenOfNodeList.iterator();
                List nodesLeftChild = new ArrayList(nodesIncoming);
                List collectedNodesChild = new ArrayList(collectedNodes);
                List cladeRootStartChild = new ArrayList(cladeRootStart);

                RecursionInformation returnedChild = new RecursionInformation(recursionState.isSupportive,
                        nodesLeftChild,
                        collectedNodesChild,
                        recursiveCount, cladeRootStartChild);
                Integer indexNumberChildren = 1;
                while (childrenOfNode.hasNext()) {

                    Node newChild = (Node) childrenOfNode.next();

                    // RecursionInformation giveToChild = new
                    // RecursionInformation(recursionState.isSupportive,
                    // nodesLeftChild,
                    // collectedNodesChild,
                    // recursiveCount);

                    returnedChild = checkCentipedeHelper(returnedChild, newChild,
                            indexNumberChildren > copyChildren.size() - 1 ? 0 : copyChildren.get(indexNumberChildren));

                    // boolean childResult = returnedChild.isSupportive;
                    // checkCentipedeHelper(nodesLeftChild, collectedNodes, newChild);
                    if (returnedChild.nodesToSearch.size() == 0) {
                        break;
                    }
                    // nodesLeftChild = returnedChild.nodesToSearch;
                    // collectedNodesChild = returnedChild.foundNodes;
                    // recursiveCount = returnedChild.passedInternal;
                    // System.out.println(returnedChild.cladeRootStart);

                    if (returnedChild.foundNodes.size() > 0 & returnedChild.cladeRootStart.size() == 0) {
                        returnedChild.cladeRootStart.add(currentCladeRoot);
                        // returnedChild.cladeRootStart = cladeRootStartChild;
                    }
                    indexNumberChildren++;

                    // System.out.println(returnedChild.cladeRootStart);

                }
                if (returnedChild.cladeRootStart.size() > 0 &
                        returnedChild.nodesToSearch.size() > 0) {

                    returnedChild.passedInternal = returnedChild.passedInternal + 1;
                    // // return new RecursionInformation(returnedChild.isSupportive,
                    // nodesLeftChild,
                    // // collectedNodesChild,
                    // // recursiveCount + 1);
                }
                if (returnedChild.cladeRootStart.contains(currentCladeRoot)) {
                    returnedChild.cladeRootStart.remove(currentCladeRoot);
                    // returnedChild.cladeRootStart = cladeRootStartChild;

                    // collectedNodesChild = new ArrayList();
                    // returnedChild.foundNodes = new ArrayList();

                }
                // System.out.println(collectedNodesChild);
                // System.out.println(childrenOfNodeList);
                // List checkIntersection = new ArrayList(collectedNodesChild);
                // System.out.println(checkIntersection);
                // checkIntersection.retainAll(childrenOfNodeList);

                // else {
                // System.out.println(collectedNodes);
                // System.out.println(returnedChild.foundNodes);
                // if (collectedNodesChild.size() > 0) {
                // returnedChild.passedInternal = recursiveCount + 1;

                // }
                return returnedChild;
            }
        }
    }

    static public Integer getTotalNumberChildren(Node n) {
        if (n.getChildren().size() == 0) {
            return 1;
        } else {
            List childrenOfNodeList = n.getChildren();
            Iterator childrenOfNode = childrenOfNodeList.iterator();
            Integer numberOfChildren = 0;
            while (childrenOfNode.hasNext()) {
                Node childOfNode = (Node) childrenOfNode.next();
                numberOfChildren += getTotalNumberChildren(childOfNode);
            }
            return numberOfChildren;
        }

    }

    public List getListFromIterator(Iterator nodesToTest) {
        List deiteratedList = new ArrayList();
        nodesToTest.forEachRemaining(deiteratedList::add);
        return deiteratedList;
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

    /**
     * TODO: WHAT DOES THIS FUNCTION DO?
     * 
     * 
     * TODO: THIS IS I THINK THE FORWARD PASS OF PARSIMONY
     * 
     * @param newicktree
     * @param snpPosition
     */
    public void label(NewickTree newicktree, Integer snpPosition) {
        Object taxaNameCollector = mainSNPHashMap.getSampleNames().iterator();

        // So this first part is filling the nodes of the newick with the information of
        // the SNP table

        label0: do {
            if (!((Iterator) (taxaNameCollector)).hasNext())
                break;
            String taxaName = (String) ((Iterator) (taxaNameCollector)).next();
            String currentAlleleSNP = mainSNPHashMap.getSnp(snpPosition, taxaName);
            String referenceSNP = mainSNPHashMap.getReferenceSnp(snpPosition);
            // if (s1.equals(s2))
            // s1 = ".";
            Iterator treeIterator = newicktree.getNodeList().iterator();
            Node nodeOfNewickTree;
            // Find which node in the tree is being handled here
            do {
                if (!treeIterator.hasNext())
                    continue label0;
                nodeOfNewickTree = (Node) treeIterator.next();
            } while (!nodeOfNewickTree.getName().equals(taxaName));
            nodeOfNewickTree.setLabel(snpPosition.intValue(), currentAlleleSNP);
        } while (true);

        // Now
        taxaNameCollector = new ArrayList();
        Iterator secondNewickIterator = newicktree.getNodeList().iterator();
        do {
            if (!secondNewickIterator.hasNext())
                break;
            Node node = (Node) secondNewickIterator.next();
            if (!node.getLabel().containsKey(snpPosition) && node.getChildren().isEmpty())
                ((List) (taxaNameCollector)).add(node);
        } while (true);
        if (((List) (taxaNameCollector)).isEmpty())
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

    /**
     * Export the files to the corresponding files
     */
    public void getResults() {
        toFile(filepath.createFile("claden.txt"), cladenGlobalHashMap);
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
            if (mainSNPHashMap.getSNPs().contains(Integer.valueOf(i)) || i == -1)
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

    public SNPTable mainSNPHashMap;
    private NewickTree tree;
    private NewFile filepath;
    private Map cladenGlobalHashMap;
    private Map supportTree;
    private Map notSupportTree;
    private Map supportSplitKeys;
    private Map notSupportSplitKeys;
}
