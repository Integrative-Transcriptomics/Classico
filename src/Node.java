import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Node{

    private SNPTree snpTree;
    private String name;
    private List<Node> children;
    private Node parent;
    private int idx;
    private float distance;
    private Set<SNPType> snpOptions;
    private HashMap<SNPType, Integer> counts;
    private int depth;
    private Phyly phyly;
    private ArrayList<Node> leafNodes;

    public Node(SNPTree snpTree) {
        this.snpTree = snpTree;
        this.counts = new HashMap<>();
        this.phyly = null;
        setDepth(0);
        this.leafNodes = new ArrayList<>();
    }

    public Node(Node parent, int depth) {
        this.parent = parent;
        this.snpTree = parent.getSNPTree();
        this.counts = new HashMap<>();
        this.phyly = null;
        this.setDepth(depth);
        this.leafNodes = new ArrayList<>();
    }

    public Node(Node parent, String name, float distance) {
        this.parent = parent;
        this.name = name;
        this.distance = distance;
        this.snpTree = parent.getSNPTree();
        this.counts = new HashMap<>();
        this.phyly = null;
        this.leafNodes = new ArrayList<>();
    }

    public SNPTree getSNPTree(){
        return this.snpTree;
    }

    public void setDepth(int depth){
        this.depth = depth;
    }

    public int getDepth(){
        return this.depth;
    }

    /**
     * Sets the name of the current node instance.
     * 
     * @param name for leaf node: species name, for internal node: any name
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setPhyly(Phyly phyly){
        this.phyly = phyly;
    }

    public Phyly getPhyly(){
        return this.phyly;
    }

    /**
     * Adds child to current node instance.
     * 
     * @param child
     */
    public void addChild(Node child) {
        if (this.children == null){
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }


    /**
     * @return
     */
    public List<Node> getChildren() {
        return this.children;
    }

    /**
     * @return
     */
    public boolean hasChildren() {
        if (this.children == null){
            return false;
        }
        else{
            return true;
        }
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return this.distance;
    }

    public Node getParent() {
        return this.parent;
    }

    public void setSNPOptions(Set<SNPType> snpOptions) {
        this.snpOptions = snpOptions;
    }

    public Set<SNPType> getSNPOptions() {
        return this.snpOptions;
    }

    public void setSNPTypeCount(SNPType snp, int count) {
        counts.put(snp, count);
    }

    public int getSNPTypeCount(SNPType snp) {
        return counts.get(snp);
    }

    /**
     * Adds possible SNPs to each node of the tree. Further, the count of all descendent leafs of this node for each SNP type is also added.
     * 
     * @param snpList List of SNPs from SNP table
     * @param speciesToColumn Maps each species to the corresponding column of the SNP table
     */
    public void forwardPass(List<SNPType> snpList, HashMap<String, Integer> speciesToColumn) {
        addSNPs(snpList, speciesToColumn);
        updateSNPCount();
    }

    /**
     * Chooses the SNP of the node according to parent node. Then checks for phylogenetic groups starting at the current node. 
     * 
     * @param totalSNPCount stores the total count of leafes by SNP type
     */
    public void backwardPass(HashMap<SNPType, Integer> totalSNPCount) {
        chooseSNP();
        checkPhyly(totalSNPCount);
    }

    /**
     * Chooses the SNP according to the parent node. If the parent node only contains one SNP, then this SNP is chosen for the current node.
     */
    public void chooseSNP() {
        if (this.getParent() != null) {
            Set<SNPType> snpSetParent = new HashSet<>(this.getParent().getSNPOptions());
            Set<SNPType> snpSetCurr = new HashSet<>(this.getSNPOptions());
            
            snpSetCurr.retainAll(snpSetParent);
            if (this.getSNPOptions().size() != 1 && snpSetCurr.size() > 0) {
                this.setSNPOptions(snpSetCurr);
            }
        }
        // check if this node is root of an unresolved clade
        if (this.getSNPOptions().size() == 1 && this.getSNPOptions().contains(SNPType.N) && !this.getParent().getSNPOptions().contains(SNPType.N)){
            Node root =  this;
            ArrayList<Node> leafs = new ArrayList<>();
            for (Node leaf:leafNodes){
                leafs.add(leaf);
            }
            this.snpTree.cladesUnresolvedBases.put(root, leafs);
        }
    }

    /**
     * Add possible SNPs to node. If the node is a leaf node, the SNP of the belonging species is used. Fitches forward pass is applied.
     * 
     * @param snpList List of SNPs from SNP table
     * @param speciesToColumn Maps each species to the corresponding column of the SNP table
     */
    public void addSNPs(List<SNPType> snpList, HashMap<String, Integer> speciesToColumn) {
        if (this.hasChildren()) {
            Set<SNPType> snpOptions = new HashSet<>(
                this.getChildren().get(0).getSNPOptions());
            for (int idxOtherChild = 1; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                snpOptions.retainAll(this.getChildren().get(idxOtherChild).getSNPOptions());

            }
            if (!snpOptions.isEmpty() && snpOptions != null) {
                this.setSNPOptions(snpOptions);
            } else {
                for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                    snpOptions.addAll(this.getChildren().get(idxOtherChild).getSNPOptions());
                }
                // N is only propragated as long as there is no solved base for all children of a node
                if (snpOptions.size() > 1 && snpOptions.contains(SNPType.N)){
                    snpOptions.remove(SNPType.N);
                }
                this.setSNPOptions(snpOptions);
            }
        } else {
            // put inside setSNPs method
            Set<SNPType> snps = new HashSet<>();
            snps.add(snpList.get(speciesToColumn.get(this.getName())));
            this.setSNPOptions(snps);
        }
    }

    /**
     * Update count of descending leafs for each SNP type. 
     * If the node is a leaf node, initialize the counts with one for the SNP occuring at this position and zero for the other SNPs.
     * Else add the counts of the left and right child for the current node.
     */
    public void updateSNPCount() {
        for (SNPType snp : SNPType.values()) {
            if (this.hasChildren()) {
                int snpCount = 0;
                for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                    snpCount += this.getChildren().get(idxOtherChild).getSNPTypeCount(snp);
                }
                this.setSNPTypeCount(snp, snpCount);
            } else {

                if (this.getSNPOptions().contains(snp)) {
                    this.setSNPTypeCount(snp, 1);
                } else {
                    this.setSNPTypeCount(snp, 0);
                }
            }
        }
    }

    public void updateLeafs(){
        if (this.hasChildren()){
            for (Node child:this.getChildren()){
                for (Node leaf: child.leafNodes){
                    this.leafNodes.add(leaf);
                }         
            }
        }
        else{
            this.leafNodes.add(this);
        }
    }

    /**
     * 
     * Check if the current node is the root of a new phyly group. 
     * 
     * @param totalSNPcount
     * @return
     */
    public void checkPhyly(HashMap<SNPType, Integer> totalSNPcount){
            
        for (SNPType snp : this.getSNPOptions()){
            if (this.getSNPOptions().size() == 1){
                
                boolean isTotalSNPCount = (this.getSNPTypeCount(snp) == totalSNPcount.get(snp));
                
                boolean isOtherSNP = false;
                Set<SNPType> otherSNPs = new HashSet<>();
                otherSNPs.addAll(this.counts.keySet());
                otherSNPs.remove(snp);
                for (SNPType otherSNP: otherSNPs){
                    if (this.counts.get(otherSNP) > 0){
                        isOtherSNP = true;
                    }
                }
                
                boolean isExtendable = false;
                if(this.getParent() != null && this.getParent().getSNPOptions().size() == 1 && this.getParent().getSNPOptions().contains(snp)){
                    isExtendable = true;
                }

                int childCount = 0;
                if (this.hasChildren()){
                    for (Node child: this.getChildren()){
                        if (child.getSNPOptions().contains(snp)){
                            childCount += 1;
                        }
                        
                    }
                }
                

                Phyly currPhyly = null;

                // PARAPHYLETIC
                if (isTotalSNPCount && isOtherSNP && childCount > 1){
                    if(!(snp.equals(SNPType.REF))){
                        if (!snp.equals(SNPType.N)){
                            currPhyly = Phyly.para;
                        }
                        else{
                            currPhyly = Phyly.poly;
                        }
                    }
                }

                // MONOPHYLETIC
                else if (isTotalSNPCount && !isOtherSNP){
                    if(!(snp.equals(SNPType.REF))){
                        if (!snp.equals(SNPType.N)){
                            currPhyly = Phyly.mono;
                        }
                        else{
                            currPhyly = Phyly.poly;
                        }
                    }
                }

                // POLYPHYLETIC
                else if (!isTotalSNPCount && !isOtherSNP && !isExtendable){
                    if(!(snp.equals(SNPType.REF))){
                        currPhyly = Phyly.poly;
                    }
                }
                this.setPhyly(currPhyly);
                
                

                // append to output list
                if(currPhyly != null && this.getSNPTree().getSpecifiedClades().contains(currPhyly)){
                    this.setPhylyStatistics(currPhyly, snp);
                    Main.getOutputByPhyly(currPhyly).addClade(this, this.getSNPTree().getPosition(), snp);
                }

            }
        }
        
    }

    public int getID() {
        return this.idx;
    }

    public void setPhylyStatistics(Phyly phyly, SNPType snp){
        // count number of snps in a clade
        if (this.getSNPTree().snpTypeStatistics.containsKey(phyly)){
            ArrayList<SNPType> listSNPTypes = this.getSNPTree().snpTypeStatistics.get(phyly);
            if (!listSNPTypes.contains(snp)){
                listSNPTypes.add(snp);
            }
        }
        // count number of clade types
        int previousCount = Main.phylyStatistics.get(phyly);
        int newCount = previousCount + this.counts.get(snp);
        Main.phylyStatistics.put(phyly, newCount);
    }

    public void setIndex(int index) {
        this.idx = index;
    }

    /** Adds another line to the ID distribution output file with the information on the current node. 
     * If the current node is a leaf node the name of the node is stored, else the ids of all children are stored
     * @param fileWriter instance of FileWriter that is used for storage
     */
    public void writeNodeToIDDistributionFile(FileWriter fileWriter){
        String line;
        if (this.hasChildren()){
            line = this.idx + "\t";
            for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                if (idxOtherChild != 0){
                    line += ",";
                } 
                line += this.getChildren().get(idxOtherChild).idx;
            }
            line += "\n";
        }
        else{
            line = this.idx + "\t" + this.name + "\n";
        }
        try {
            fileWriter.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<SNPType> predict(double maxDepth, String method){

        HashMap<SNPType,Double> scores = new HashMap<>();
        for (SNPType snpType: SNPType.values()){
            scores.put(snpType, getScore(maxDepth, method, snpType, this.getDepth()));
        }
        Set<SNPType> maxSNP = new HashSet<>();
        double maxScore = 0;
        for (SNPType snpType: scores.keySet()){
            if (scores.get(snpType) > maxScore){
                maxScore = scores.get(snpType);
                maxSNP = new HashSet<>();
                maxSNP.add(snpType);
            }
            else if (scores.get(snpType)== maxScore){
                maxSNP.add(snpType);
            }
        }    
        if (maxSNP.size() != 1){
            maxSNP = new HashSet<>();
            maxSNP.add(SNPType.N);
            System.err.println("Unresolved base at node " + this.idx + " and position " + this.snpTree.position + ". The computed scores are the following:" + scores);
        }
        return maxSNP;

    }
  
    public List<Node> getSiblings(){
        Set<Node> siblings = new HashSet<>();
        if (this.parent != null){
            siblings.addAll(this.parent.children);
            siblings.remove(this);
        }
        return new ArrayList<>(siblings);

    }


    public double getScore(double maxDepth, String method, SNPType snpType, int currDepth){
        double currDepthA = ((double) currDepth/this.snpTree.depth);
        double score = 0;
        int branchCount = 1;
        Node parent = this.getParent();
        List<Node> siblings = null;
        double parentDepth = ((double) parent.getDepth()/this.snpTree.depth);
        if (method.equals("cladewise") || method.equals("one-step")){
            siblings = this.getSiblings();
        }
        while(parent != null && currDepthA - maxDepth <= parentDepth){
            if (parent.snpOptions.contains(snpType)){
                score += (1.0/branchCount);
            }
            if (method.equals("cladewise") || method.equals("one-step")){
                for (Node sibling:siblings){
                    if (method.equals("cladewise")){
                        score += sibling.getCladewiseScore(branchCount + 1, snpType);
                    }
                    if (method == "one-step"){
                        if (sibling.snpOptions.contains(snpType)){
                            score += (1.0/(branchCount + 1));
                        }
                    }
                }
                siblings = parent.getSiblings();
            }
            parent = parent.getParent();
            if (parent != null){
                parentDepth = ((double) parent.getDepth()/this.snpTree.depth);
            }
            branchCount += 1;
        }
        /*if (currDepthA - maxDepth <= 0){
            System.out.println("Info: The maximal extension depth for prediction exceeds the root for an unresolved base at position " 
            + this.snpTree.position + ". The root is used as the final extension.");
        }*/
        return score;
    }

    public double getCladewiseScore(int branchCount, SNPType snpType){

        double score = 0;
        if (this.hasChildren()){
            for (Node child:this.getChildren()){
                score += child.getCladewiseScore(branchCount + 1, snpType);
            }
        }
        if (this.snpOptions.contains(snpType)){
            score += (1.0/branchCount);
        }
        return score;
    }

}
