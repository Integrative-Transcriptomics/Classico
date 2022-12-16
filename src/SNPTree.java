import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;


public class SNPTree{

    boolean isUsed;

    public Node root;
    public HashMap<String, Integer> speciesToColumn;
    
    public List<Phyly> specifiedClades;
    public int position;

    public SNPTree(String filepath, List<Phyly> specifiedClades){
        this.root = parseNewickTree(Paths.get(filepath));
        this.specifiedClades = specifiedClades;
    }

    public boolean isUsed(){
        return this.isUsed;
    }

    public void setIsUsed(boolean isUsed){
        this.isUsed = isUsed;
    }

    public Node parseNewickTree(Path path){
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
            // TODO: better file reading
            for(String line:lines){
                root = new Node(this);
                // remove ; from end
                parseSubtree(line.substring(0,line.length() - 1), this.root, 1);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return root;
    }

    public void mapSpeciesToColumn(ArrayList<String> snpTableSpecies){

        this.speciesToColumn = new HashMap<>();
        int colIdx = 0;
        for (String currSnpTableSpecies: snpTableSpecies){
            this.speciesToColumn.put(currSnpTableSpecies, colIdx);
            colIdx += 1;
        }    
    }

    public void setPosition(int position){
        this.position = position;
    }

    public int getPosition(){
        return this.position;
    }

    /**
     * 
     * 
     * @param subtree
     * @param node
     */
    public int parseSubtree(String subtree, Node node, int id){
       
        int idxStart = subtree.indexOf('(');
        int idxEnd = subtree.lastIndexOf(')');
        String currNode = subtree;

        if (idxStart != -1 && idxEnd != -1){

            ArrayList<String> children = splitSubtree(subtree.substring(idxStart + 1, idxEnd));
            for (String child: children){
                Node childNode = new Node(node);
                node.addChild(childNode);
                id = parseSubtree(child, childNode, id);
            }
            currNode = subtree.substring(idxEnd + 1);
        }

        if (currNode.contains(":")){
            String[] nodeAttributes = currNode.split(":");
            node.setName(nodeAttributes[0]);
            node.setDistance(Float.parseFloat(nodeAttributes[1]));
            node.setIndex(id);
        }
        else{
            node.setName(null);
            node.setDistance(Float.NaN);
            node.setIndex(id);
        }
        return id + 1;

    }

    /**
     * Splits a Newick subtree (T1, T2) into the two branches T1 and T2.
     *
     * @param subtree String of subtree in Newick format
     * @return List of branches
     */

    private ArrayList<String> splitSubtree(String subtree){
        
        ArrayList<String> splits = new ArrayList<String>();
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


    /**
     * Performs Fitch algorithm on tree and extracts mono-/poly-/paraphyletic groups.
     * @param snpList list of SNPs that should be applied to the tree
     */
    public void propragateSNPs(List<SNPType> snpList){

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
        
        if (node.hasChildren()){
            for (Node child: node.getChildren()){
                preOrderTraversal(method, child, args);                
            }
        }
        
    }

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

    public List<Phyly> getSpecifiedClades(){
        return this.specifiedClades;
    }

    /** stores the tree structure and leaf names with the corresponding node ID
     * @param outputDirectory directory the tree structure should be stored in
     */
    public void saveIDDistribution(String outputDirectory){
        try {
            FileWriter writerIDDistribution = new FileWriter((outputDirectory + "/IDdistribution.txt"));
            Class[] parameterTypes = new Class[1];
            parameterTypes[0] = FileWriter.class;
            Method writeNodeToIDDistributionFile;
            try {
                writeNodeToIDDistributionFile = Node.class.getMethod("writeNodeToIDDistributionFile", parameterTypes);
                // execute addSNPs for each node in tree in post order
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
