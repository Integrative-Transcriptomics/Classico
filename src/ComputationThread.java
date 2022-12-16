import java.util.ArrayList;
import java.util.List;

public class ComputationThread extends Thread {

    ArrayList<String> columnNames;
    SNPTree snpTree;
    String line;

    
    public ComputationThread(SNPTree snpTree){
        this.snpTree = snpTree;
    }

    public void setParams(ArrayList<String> columnNames, String line){
        this.columnNames = columnNames;
        this.line = line;
    }

    public void run(){
        String[] listContent = line.split("\t");
        int position = Integer.parseInt(listContent[0]);
        snpTree.setPosition(position);
        SNPType referenceSNP = SNPType.fromString(listContent[1]);
        List<SNPType> snps = new ArrayList<>();
        for (int i = 2; i < listContent.length; i++){
            SNPType currSNP = SNPType.fromString(listContent[i]);
            if (!currSNP.equals(referenceSNP)){
                snps.add(SNPType.fromString(listContent[i]));
            }
            else{
                System.err.println("Info: The SNP of species " + columnNames.get(i-2) + " equals reference SNP at position " + position + ". The SNP is treated as the reference.");
                snps.add(SNPType.REF);
            }
        }
        snpTree.propragateSNPs(snps);
        snpTree.setIsUsed(false);
        Main.addUnusedTree(snpTree);
    }
}