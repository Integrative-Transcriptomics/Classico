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
    private int id;
    private float distance;
    private Set<SNPType> snpOptions;
    private HashMap<SNPType, Integer> counts;
    private int depth;
    private Phyly phyly;
    private ArrayList<Node> leafNodes;

    // =================================================
    // CONSTRUCTORS
    // =================================================
    

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

    // ===================================================
    // GETTERS AND SETTERS
    // ===================================================

    public SNPTree getSNPTree(){
        return this.snpTree;
    }

    public void setDepth(int depth){
        this.depth = depth;
    }

    public int getDepth(){
        return this.depth;
    }

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

    public List<Node> getChildren() {
        return this.children;
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

    public void setSNPOptions(SNPType snp) {
        Set<SNPType> snpOptions = new HashSet<>();
        snpOptions.add(snp);
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

    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public List<Node> getSiblings(){
        Set<Node> siblings = new HashSet<>();
        if (this.parent != null){
            siblings.addAll(this.parent.children);
            siblings.remove(this);
        }
        return new ArrayList<>(siblings);

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
    public boolean hasChildren() {
        if (this.children == null){
            return false;
        }
        else{
            return true;
        }
    }

    // ====================================================================
    // FITCH FORWARD AND BACKWARD PASS
    // ====================================================================

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
        checksRootUnresolvedClade();
        checkPhyly(totalSNPCount);
    }

    /**
     * Label nodes with SNPs. If the node is a leaf node, the SNP 
     * of the corresponding species is used. Otherwise the label of Fitchs forward pass is used.
     * 
     * @param snpList List of SNPs from SNP table
     * @param speciesToColumn Maps each species to the corresponding column of the SNP table
     */
    public void addSNPs(List<SNPType> snpList, HashMap<String, Integer> speciesToColumn) {
        // node has children -> Fitch's forward pass is used
        if (this.hasChildren()) {
            // compute intersection of all children's SNP labels
            Set<SNPType> snpOptions = new HashSet<>(
                this.getChildren().get(0).getSNPOptions());
            for (int idxOtherChild = 1; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                snpOptions.retainAll(this.getChildren().get(idxOtherChild).getSNPOptions());

            }
            // 1st case: intersection is not empty -> label node with intersection
            if (!snpOptions.isEmpty()) {
                this.setSNPOptions(snpOptions);
            } 
            // 2nd case: intersection is empty -> label node with union
            else {
                // compute union of all children's SNP labels
                for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                    snpOptions.addAll(this.getChildren().get(idxOtherChild).getSNPOptions());
                }
                // N is only propragated as long as there is no solved base for all children of a node
                if (snpOptions.size() > 1 && snpOptions.contains(SNPType.N)){
                    snpOptions.remove(SNPType.N);
                }
                this.setSNPOptions(snpOptions);
            }
        } 
        // node has no children -> use SNP from SNP table
        else {
            this.setSNPOptions(snpList.get(speciesToColumn.get(this.getName())));
        }
    }

    /**
     * Update count of descending leafs for each SNP type. 
     * If the node is a leaf node, initialize the counts with one for the SNP occuring at this position and zero for the other SNPs.
     * Otherwise, add the counts of the left and right child for the current node.
     */
    public void updateSNPCount() {
        for (SNPType snp : SNPType.values()) {
            // node is no leaf node
            if (this.hasChildren()) {
                // sum up all counts of all children
                int snpCount = 0;
                for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                    snpCount += this.getChildren().get(idxOtherChild).getSNPTypeCount(snp);
                }
                this.setSNPTypeCount(snp, snpCount);
            } 
            // node is leaf node
            else {
                // initialize count with 1 if node contains this SNP label
                if (this.getSNPOptions().contains(snp)) {
                    this.setSNPTypeCount(snp, 1);
                } 
                // otherwise initialize count with 0
                else {
                    this.setSNPTypeCount(snp, 0);
                }
            }
        }
    }

    /**
     * Chooses the SNP according to the parent node. The intersection of the current node's label and parent's label are the new labeling 
     * of the current node if it is not empty. Furthermore, 
     */
    public void chooseSNP() {
        // if node has a parent
        if (this.getParent() != null) {
            // try to resolve node's label based on parent
            Set<SNPType> snpSetParent = new HashSet<>(this.getParent().getSNPOptions());
            Set<SNPType> snpSetCurr = new HashSet<>(this.getSNPOptions());
            snpSetCurr.retainAll(snpSetParent);
            if (this.getSNPOptions().size() != 1 && snpSetCurr.size() > 0) {
                this.setSNPOptions(snpSetCurr);
            }
        }
        
    }
 
    /**
     * Check if this node is root of an unresolved clade and adds the clade to unresolved clades if this is the case.
     */
    public void checksRootUnresolvedClade(){
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
     * 
     * Check if the current node is the root of a new phyly group. 
     * If this is the case then save it in the corresponding {@link Output} datastructure and 
     * update the allele statistics for this position. 
     * 
     * @param totalSNPcount map linking each SNP type to the total count of this SNP at the current position
     * 
     */
    public void checkPhyly(HashMap<SNPType, Integer> totalSNPcount){
        
        // only check nodes with unambiguous SNP labels
        if (this.getSNPOptions().size() == 1){
            for (SNPType snp : this.getSNPOptions()){
            
                // check if label is specific for this clade, so no other subtree contains this SNP
                boolean isSpecific = (this.getSNPTypeCount(snp) == totalSNPcount.get(snp));
                
                // check if label is consistent in this clade, so there are no other SNPs except this SNP in the subtree
                boolean isConsistent = true;
                Set<SNPType> otherSNPs = new HashSet<>();
                otherSNPs.addAll(this.counts.keySet());
                otherSNPs.remove(snp);
                for (SNPType otherSNP: otherSNPs){
                    if (this.counts.get(otherSNP) > 0){
                        isConsistent = false;
                    }
                }
                
                // check if label is extendable to parent -> extension criterion
                boolean isExtendable = false;
                if(this.getParent() != null && this.getParent().getSNPOptions().size() == 1 && this.getParent().getSNPOptions().contains(snp)){
                    isExtendable = true;
                }

                // check if there are more than one children with the current label -> MRCA criterion
                int childCount = 0;
                if (this.hasChildren()){
                    for (Node child: this.getChildren()){
                        if (child.getSNPOptions().contains(snp)){
                            childCount += 1;
                        }
                        
                    }
                }
                

                Phyly currPhyly = null;

                // PARAPHYLETIC:
                // label needs to be specific for clade, but not consistent in clade but should be MRCA
                // (can not be N because of special N propagation)
                if (isSpecific && !isConsistent && childCount > 1){
                    if(!(snp.equals(SNPType.REF))){
                        currPhyly = Phyly.para;
                    }
                }

                // MONOPHYLETIC
                // label needs to be specific for clade and consistent in clade
                // N's are still unresolved because they could be any of the four bases
                else if (isSpecific && isConsistent){
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
                // label needs to be consistent in clade, but not specific for clade and not extendable to its parent
                else if (!isSpecific && isConsistent && !isExtendable){
                    if(!(snp.equals(SNPType.REF))){
                        currPhyly = Phyly.poly;
                    }
                }

                // update phyly
                this.setPhyly(currPhyly);
                
                

                // save phyly to output data structure and statistiscs if this phyly is specified
                if(currPhyly != null && this.getSNPTree().getSpecifiedClades().contains(currPhyly)){
                    this.updateStatistics(currPhyly, snp);
                    Main.getOutputByPhyly(currPhyly).addClade(this, this.getSNPTree().getPosition(), snp);
                }

            }
        }
        
    }

    

    /**
     * update statistics
     * 
     * @param phyly current phyly
     * @param snp SNP of current phyly
     */
    public void updateStatistics(Phyly phyly, SNPType snp){
        // update allele statistics
        if (this.getSNPTree().alleleStatistics.containsKey(phyly)){
            ArrayList<SNPType> listSNPTypes = this.getSNPTree().alleleStatistics.get(phyly);
            if (!listSNPTypes.contains(snp)){
                listSNPTypes.add(snp);
            }
        }
        // update snp statistics
        int previousCount = Main.snpStatistics.get(phyly);
        int newCount = previousCount + this.counts.get(snp);
        Main.snpStatistics.put(phyly, newCount);
    }


    // =========================================================
    // OTHER METHODS
    // =========================================================

    /**
     * add all descendant leaf nodes to the leafNode list of the current node
     */
    public void updateLeafs(){
        // if this node is no leaf get all leafs from its children
        if (this.hasChildren()){
            for (Node child:this.getChildren()){
                for (Node leaf: child.leafNodes){
                    this.leafNodes.add(leaf);
                }         
            }
        }
        // if this node is a leaf node only add self
        else{
            this.leafNodes.add(this);
        }
    }



    /** Adds another line to the ID distribution output file with the information on the current node. 
     * If the current node is a leaf node the name of the node is stored, else the ids of all children are stored
     * @param fileWriter instance of FileWriter that is used for storage
     */
    public void writeNodeToIDDistributionFile(FileWriter fileWriter){
        String line;
        // node no leaf node
        if (this.hasChildren()){
            line = this.id + "\t";
            for (int idxOtherChild = 0; idxOtherChild < this.getChildren().size(); idxOtherChild ++){
                if (idxOtherChild != 0){
                    line += ",";
                } 
                line += this.getChildren().get(idxOtherChild).id;
            }
            line += "\n";
        }
        // node leaf node
        else{
            line = this.id + "\t" + this.name + "\n";
        }
        // store in file
        try {
            fileWriter.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ==================================================================================
    // RESOLTION METHODS
    // ==================================================================================

    /**
     * resolve current node
     * 
     * @param relMaxDepth relative maximum depth parameter
     * @param method neighborhood extension method
     * @return resolved SNP
     */
    public SNPType resolve(double relMaxDepth, String method){
        
        // compute scores for each SNP 
        HashMap<SNPType,Double> scores = new HashMap<>();
        for (SNPType snpType: SNPType.values()){
            scores.put(snpType, getScore(relMaxDepth, method, snpType));
        }

        // determine SNP(s) with maximum score 
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
        SNPType result = null;
        // if there are more than one SNP with the maximum score leave base unresolved
        if (maxSNP.size() != 1){
            System.out.println("Unresolved base at node " + this.id + " and position " + this.snpTree.position + ". The computed scores are the following:" + scores);
            result = SNPType.N;
        }
        // otherwise return SNP with maximum score 
        else{
            for (SNPType snpType:maxSNP){
                result =  snpType;
            }
        }
        return result;
    }
  

    /**
     * compute score for a SNP
     * 
     * @param relMaxDepth relative maximum depth parameter
     * @param method neighborhood extension method
     * @param snpType SNP for which score should be computed
     * @return score
     */
    public double getScore(double relMaxDepth, String method, SNPType snpType){
        
        // compute current relative depth
        int currDepth = this.getDepth();
        double currRelDepth = ((double) currDepth/this.snpTree.depth);
        
        // initialize parameters
        double score = 0;
        int branchCount = 1;
        Node parent = this.getParent();
        List<Node> siblings = null;
        double parentDepth = ((double) parent.getDepth()/this.snpTree.depth);
        if (method.equals("cladewise") || method.equals("parent-sibling")){
            siblings = this.getSiblings();
        }

        // extend neighborhood until the relative maximum depth parameter or root is reached
        while(parent != null && currRelDepth - relMaxDepth <= parentDepth){
            // always check if parent contains SNP as label to add parent score to total score
            if (parent.snpOptions.contains(snpType)){
                score += (1.0/branchCount);
            }
            // if the cladewise neighborhood extension method or parent-sibling method is chosen also consider siblings of current node
            if (method.equals("cladewise") || method.equals("parent-sibling")){
                for (Node sibling:siblings){
                    // if cladewise method is chosen for the whole subtree of each sibling the score is added
                    if (method.equals("cladewise")){
                        score += sibling.getCladewiseScore(branchCount + 1, snpType);
                    }
                    // if the parent-sibling method is chosen only the sibling score is added
                    if (method == "parent-sibling"){
                        if (sibling.snpOptions.contains(snpType)){
                            score += (1.0/(branchCount + 1));
                        }
                    }
                }
                siblings = parent.getSiblings();
            }

            // in the next iteration consider next level
            parent = parent.getParent();
            if (parent != null){
                parentDepth = ((double) parent.getDepth()/this.snpTree.depth);
            }
            branchCount += 1;
        }
        // print warning if maximum relative depth exceeds extension depth
        /*if (currRelDepth - relMaxDepth <= 0){
            System.out.println("Info: The maximal extension depth for resolution exceeds the root for an unresolved base at position " 
            + this.snpTree.position + ". The root is used as the final extension.");
        }*/
        return score;
    }

    /**
     * compute score for whole subtree
     * 
     * @param branchCount branch count from unresolved base to current node
     * @param snpType SNP for which score should be computed
     * @return score
     */
    public double getCladewiseScore(int branchCount, SNPType snpType){

        double score = 0;
        // recursively add scores of further subtrees while increasing the branch count
        if (this.hasChildren()){
            for (Node child:this.getChildren()){
                score += child.getCladewiseScore(branchCount + 1, snpType);
            }
        }
        // for current node check if SNP is contained in SNP label and if yes add score
        if (this.snpOptions.contains(snpType)){
            score += (1.0/branchCount);
        }
        return score;
    }

}
