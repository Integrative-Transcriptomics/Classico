import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;


public class SNPTree{

    public Node root;
    public HashMap<String, Integer> speciesToColumn;
    public HashMap<Phyly,Output> mapOutputFiles;
    public List<Phyly> specifiedClades;
    public int position;
    public int depth;
    public List<Node> leafs = new ArrayList<>();
    public HashMap<Phyly, ArrayList<SNPType>> alleleStatistics;
    public HashMap<Node,ArrayList<Node>> cladesUnresolvedBases;

    public SNPTree(String filepath, List<Phyly> specifiedClades){
        this.root = parseNewickTree(Paths.get(filepath));
        this.specifiedClades = specifiedClades;
        this.cladesUnresolvedBases = new HashMap<>();
    }


    // ==========================================================================
    // METHODS FOR NEWICK TREE PARSING
    // ==========================================================================

    /**
     * parse Newick file into tree structure
     * 
     * @param path path to Newick file
     * @return root of the tree
     */
    public Node parseNewickTree(Path path){
        // --------------------------------------------------------
        // parse newick tree to internal tree structure
        // --------------------------------------------------------
        List<String> lines;
        try {
            // assumes that each Newick file only contains one Newick tree
            lines = Files.readAllLines(path);
            String line = String.join("",lines);
            root = new Node(this);
            // remove ; from end
            parseSubtree(line.substring(0,line.length() - 1), this.root, 1, 0);
            
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        // --------------------------------------------------------
        // map leafs to each internal node in post order traversal
        // --------------------------------------------------------
        Class[] parameterTypes = new Class[0];
        Method updateLeafsMethod;
        try {
            // get update leafs method
            updateLeafsMethod = Node.class.getMethod("updateLeafs", parameterTypes);
            // there are no input arguments for the update leafs method
            Object [] args = new Object[]{};
            // execute update leafs method for each node in tree in post order traversal
            postOrderTraversal(updateLeafsMethod, this.root, args);
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return root;
    }

    /**
     * parse Newick subtree into tree data strucutre
     * 
     * @param subtree newick string containing the subtree
     * @param node already parsed root of the subtree
     * @param id internal ID for next node
     * @param currDepth current depth of the subtree
     * @return maximum ID of subtree = ID of root
     */
    public int parseSubtree(String subtree, Node node, int id, int currDepth){
       
        // check if there are any subtrees in the current subtree
        int idxStart = subtree.indexOf('(');
        int idxEnd = subtree.lastIndexOf(')');
        String currNode = subtree;

        // 1st case: there are subtrees in the current newick string
        if (idxStart != -1 && idxEnd != -1){
            // get all children by splitting the current newick string into all subtrees
            ArrayList<String> children = splitSubtree(subtree.substring(idxStart + 1, idxEnd));
            // for all children construct a node, connect it with the parent and parse further subtrees of the child
            for (String child: children){
                Node childNode = new Node(node, currDepth + 1);
                node.addChild(childNode);
                id = parseSubtree(child, childNode, id, currDepth + 1);
            }
            // the current node is the parent of all subtrees and is described after the closing bracket in the Newick format
            currNode = subtree.substring(idxEnd + 1);
        }
        // 2nd case: there are no substrees, so this node is a leaf
        else{
            this.leafs.add(node);
        }
        // update maximum depth of the tree
        if (currDepth > this.depth){
            this.depth = currDepth;
        }
        // set name, ID and distance to parent
        if (currNode.contains(":")){
            String[] nodeAttributes = currNode.split(":");
            node.setName(nodeAttributes[0]);
            node.setDistance(Float.parseFloat(nodeAttributes[1]));
            node.setID(id);
        }
        else{
            node.setName(null);
            node.setDistance(Float.NaN);
            node.setID(id);
        }
        return id + 1;
    }

    /**
     * Splits a Newick subtree (T1, T2, ..., Tn) into all branches T1, T2, ..., Tn.
     *
     * @param subtree String of subtree in Newick format
     * @return List of branches
     */

    private ArrayList<String> splitSubtree(String subtree){
        
        ArrayList<String> splits = new ArrayList<String>();
        // general method: 
        // subtrees are separated by commata
        // if there is a comma it needs to be checked whether the amount of opening and closing brackets before the comma is equal
        // if this is the case, then this comma separates two subtrees, otherwise it is a sepatation that is deeper in the tree
        int counterStart = 0;
        int counterEnd = 0;
        int idxStart = 0;
        for(int i = 0; i < subtree.length(); i++)
            switch(subtree.charAt(i))
            {
            case '(':
                counterStart++;
                break;

            case ')':
                counterEnd++;
                break;
            case ',':
                if(counterStart == counterEnd){
                    splits.add(subtree.substring(idxStart, i));
                    idxStart = i + 1;
                }
                break;
            default:
                break;
            }
        splits.add(subtree.substring(idxStart));
        return splits;
    }

    
    // =========================================================================
    // METHOD FOR SNP PROPAGATION AND CLADE IDENTIFICATION
    // =========================================================================

    /**
     * Performs Fitch-like algorithm on tree and extracts mono-/poly-/paraphyletic groups.
     * @param snpList list of SNPs that should be applied to the tree
     */

    public void propagateSNPs(List<SNPType> snpList){
        // define parameter types for forwardPass method
        Class[] parameterTypes = new Class[2];
        parameterTypes[0] = List.class;
        parameterTypes[1] = HashMap.class;
        Method forwardPassMethod;
        try {
            // get forwardPass method
            forwardPassMethod = Node.class.getMethod("forwardPass", parameterTypes);
            // collect input arguments
            Object [] args = new Object[]{snpList, speciesToColumn};
            // execute forwardPass for each node in tree in post order traversal
            postOrderTraversal(forwardPassMethod, this.root, args);
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // define parameters for backwardPass method
        parameterTypes = new Class[1];
        parameterTypes[0] = HashMap.class;
        Method backwardPassMethod;
        try {
            // get backwardPass method
            backwardPassMethod = Node.class.getMethod("backwardPass", parameterTypes);
            // collect input arguments
            Object [] args = new Object[]{totalSNPCountBySNPList(snpList)};
            // execute addSNPs for each node in tree in post order
            preOrderTraversal(backwardPassMethod, this.root, args);
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }

    // =============================================================
    // general post order and pre order traversal methods
    // =============================================================

    /**
     * Applies a <code>method</code> to each node of the SNPtree of this instance. This is done in post-order traversal.
     * 
     * @param method the applied functionality
     * @param node the node the functionality is applied to
     * @param args the arguments the method is processing
     */
    public void postOrderTraversal(Method method, Node node, Object[] args){
        if (node.hasChildren()){
            for (Node child: node.getChildren()){
                postOrderTraversal(method, child, args);                
            }
        }
        try {
            method.invoke(node, args);            
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Applies a <code>method</code> to each node of the SNPtree of this instance. This is done in pre-order traversal.
     * 
     * @param method the applied functionality
     * @param node the node the functionality is applied to
     * @param args the arguments the method is processing
     */
    public void preOrderTraversal(Method method, Node node, Object[] args){
        try {
            method.invoke(node, args);            
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // pre order traversal is only used for backward pass
        // so here it can be checked that the traversal breaks for nodes that are the root of a monophyletic or polyphyletic clade
        if (node.getParent() == null || ( node.hasChildren() && (node.getPhyly() != Phyly.mono && node.getPhyly() != Phyly.poly))){
            for (Node child: node.getChildren()){
                preOrderTraversal(method, child, args);                
            }
        }
    }


    // ====================================================================
    // OTHER METHODS
    // ====================================================================

    /**
     *  initialize allele statistic with empty lists for all phylies
     */
    public void initializeAlleleStatistic(){
        alleleStatistics = new HashMap<>();    
        for (Phyly phyly: Phyly.values()){
            alleleStatistics.put(phyly, new ArrayList<>());
        }
        
    }

    /**
     *  initialize clades of unresolved bases with empty list
     */
    public void initializeUnresolvedClades(){
        this.cladesUnresolvedBases = new HashMap<>();
    }

    /**
     * map each species name to the corresponding column of the SNP table
     * 
     * @param snpTableSpecies column names (species names) of the SNP table     
     */
    public void mapSpeciesToColumn(ArrayList<String> snpTableSpecies){

        this.speciesToColumn = new HashMap<>();
        int colIdx = 0;
        for (String currSnpTableSpecies: snpTableSpecies){
            this.speciesToColumn.put(currSnpTableSpecies, colIdx);
            colIdx += 1;
        }    
    }

    /**
     * @param position SNP position in genome
     */
    public void setPosition(int position){
        this.position = position;
    }

    /**
     * @return SNP position in genome
     */
    public int getPosition(){
        return this.position;
    }

    /**
     * get total count for each SNP at current position
     * 
     * @param snpList SNPs for all species at current position
     * @return map linking SNP to count of this SNP
     */
    public static HashMap<SNPType, Integer> totalSNPCountBySNPList(List<SNPType> snpList) {
        HashMap<SNPType, Integer> totalSNPCount = new HashMap<>();
        for (SNPType snp : snpList) {
            if (!totalSNPCount.containsKey(snp)) {
                totalSNPCount.put(snp, 1);
            } else {
                totalSNPCount.put(snp, totalSNPCount.get(snp) + 1);
            }
        }
        return totalSNPCount;
    }

    /**
     * @return phyly types specified by user
     */
    public List<Phyly> getSpecifiedClades(){
        return this.specifiedClades;
    }

    /** stores the tree structure and leaf names with the corresponding node ID
     * @param outputDirectory directory where the tree structure should be stored in
     */
    public void saveIDDistribution(String filepath){
        try {
            FileWriter writerIDDistribution = new FileWriter(filepath);
            Class[] parameterTypes = new Class[1];
            parameterTypes[0] = FileWriter.class;
            Method writeNodeToIDDistributionFile;
            try {
                writeNodeToIDDistributionFile = Node.class.getMethod("writeNodeToIDDistributionFile", parameterTypes);
                // execute writeNodeToIDDistrubutionFile for each node in tree in post order
                Object [] args = new Object[]{writerIDDistribution};
                postOrderTraversal(writeNodeToIDDistributionFile, this.root, args);
                writerIDDistribution.close();
            } catch (NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
