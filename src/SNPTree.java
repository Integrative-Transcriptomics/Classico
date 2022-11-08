import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;


public class SNPTree{

    public Node root;
    public HashMap<String, Integer> speciesToColumn;

    public SNPTree(String filepath){
        this.root = parseNewickTree(Paths.get(filepath));
    }

    public Node parseNewickTree(Path path){
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
            // TODO: better file reading
            for(String line:lines){
                root = new Node();
                // remove ; from end
                parseSubtree(line.substring(0,line.length() - 1), this.root, 1);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return root;
    }

    public void mapSpeciesToColumn(HashMap<String, Integer> speciesToColumn){
        this.speciesToColumn = speciesToColumn;
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
     */
    public void propragateSNPs(List<SNPType> snpList){

        // define parameter types for addSNPs method
        Class[] parameterTypes = new Class[2];
        parameterTypes[0] = List.class;
        parameterTypes[1] = HashMap.class;
        Method forwardPassMethod;
        try {
            forwardPassMethod = Node.class.getMethod("forwardPass", parameterTypes);
            // execute addSNPs for each node in tree in post order
            Object [] args = new Object[]{snpList, speciesToColumn};
            postOrderTraversal(forwardPassMethod, this.root, args);
        } catch (NoSuchMethodException | SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        parameterTypes = new Class[1];
        parameterTypes[0] = HashMap.class;

        Method backwardPassMethod;
        try {
            backwardPassMethod = Node.class.getMethod("backwardPass", parameterTypes);
            // execute addSNPs for each node in tree in post order
            Object [] args = new Object[]{totalSNPCountBySNPList(snpList)};
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

    
}
