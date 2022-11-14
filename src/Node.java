import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Node {

    private SNPTree snpTree;
    private String name;
    private Node leftChild;
    private Node rightChild;
    private Node parent;
    private int idx;
    private float distance;
    private Set<SNPType> snpOptions;
    private HashMap<SNPType, Integer> counts;

    public Node(SNPTree snpTree) {
        this.snpTree = snpTree;
        this.counts = new HashMap<>();
    }

    public Node(Node parent) {
        this.parent = parent;
        this.snpTree = parent.getSNPTree();
        this.counts = new HashMap<>();
    }

    public Node(Node parent, String name, float distance) {
        this.parent = parent;
        this.name = name;
        this.distance = distance;
        this.snpTree = parent.getSNPTree();
        this.counts = new HashMap<>();
    }

    public SNPTree getSNPTree(){
        return this.snpTree;
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

    /**
     * Adds child to current node instance.
     * 
     * @param child
     */
    public void addChild(Node child) {
        if (this.leftChild == null) {
            this.leftChild = child;
        } else {
            this.rightChild = child;
        }
    }

    public Node getLeftChild() {
        return this.leftChild;
    }

    public Node getRightChild() {
        return this.rightChild;
    }

    /**
     * @return
     */
    public LinkedList<Node> getChildren() {
        LinkedList<Node> children = new LinkedList<>();
        children.add(leftChild);
        children.add(rightChild);
        return children;
    }

    /**
     * @return
     */
    public boolean hasChildren() {
        if (this.rightChild == null && this.leftChild == null) {
            return false;
        } else {
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
            if (this.getSNPOptions().size() != 1 && this.getParent().getSNPOptions().size() == 1) {
                this.setSNPOptions(this.getParent().getSNPOptions());
            }
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
            Set<SNPType> intersection = new HashSet<>(this.leftChild.getSNPOptions());
            intersection.retainAll(this.rightChild.getSNPOptions());
            if (!intersection.isEmpty() && intersection != null) {
                this.setSNPOptions(intersection);
            } else {
                Set<SNPType> union = new HashSet<>(this.leftChild.getSNPOptions());
                union.addAll(this.rightChild.getSNPOptions());
                this.setSNPOptions(union);
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
                int snpCount = this.leftChild.getSNPTypeCount(snp) + this.rightChild.getSNPTypeCount(snp);
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

    /**
     * 
     * Check if the current node is the root of a new phyly group. 
     * 
     * @param totalSNPcount
     * @return
     */
    public void checkPhyly(HashMap<SNPType, Integer> totalSNPcount){
        if (this.getParent() != null){
            
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

                    Phyly currPhyly = null;

                    // PARAPHYLETIC
                    if (isTotalSNPCount && isOtherSNP){
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
                    else if (!isTotalSNPCount && !isExtendable){
                        if(!(snp.equals(SNPType.REF))){
                            currPhyly = Phyly.poly;
                        }
                    }

                    // append to output list
                    if(currPhyly != null && this.getSNPTree().getSpecifiedClades().contains(currPhyly)){
                        Main.getOutputByPhyly(currPhyly).addClade(this, this.getSNPTree().getPosition(), snp);
                    }

                }
                // OPTIONAL 
                /*else{
                    if (this.getLeftChild().getSNPOptions().contains(snp) && this.getRightChild().getSNPOptions().contains(snp)){
                        
                        // PARAPHYLETIC
                        if(!(snp.equals(SNPType.REF))){
                            System.out.println(parentID + "->" + this.getID() + " " +  snp + " " +  "PARAPHYLETIC 1/" + this.getSNPOptions().size());
                        }

                    }
                }*/
            }
        }
    }

    public int getID() {
        return this.idx;
    }

    

    public void setIndex(int index) {
        this.idx = index;
    }

    

}
