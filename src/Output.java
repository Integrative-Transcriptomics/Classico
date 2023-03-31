import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

public class Output {
    
    private HashMap<Node, ArrayList<String>> outputClades;

    public Output(){
        outputClades = new HashMap<>();
    }



    /**
     * add clade to output
     * 
     * @param node root of clade
     * @param position SNP position in genome
     * @param snp SNP
     */
    public void addClade(Node node, int position, SNPType snp){
        if (!this.outputClades.containsKey(node)){
            ArrayList<String> listPosSNP = new ArrayList<>();
            listPosSNP.add(position + ":" + "[" + snp + "]");
            this.outputClades.put(node, listPosSNP);
        }
        else{
            ArrayList<String> listPosSNP = this.outputClades.get(node);
            listPosSNP.add(position + ":" + "[" + snp + "]");
            this.outputClades.put(node, listPosSNP);
        }
    }


    /**
     * convert output to string for output file
     * 
     * @return String
     */
    public String toFileString(){
        String string = "";
        // iterate over all nodes and save each node in a separate row
        for (Node node: this.outputClades.keySet()){
            // save branch where mutation occurs
            // if mutation to root of phylogenetic tree
            if (node.getParent() == null){
                string += "root";
            }
            else{ 
                string += node.getParent().getID();
            }
            string += "->";
            string += node.getID();
            string += "\t";
            // save number of clades starting at this node
            string += this.outputClades.get(node).size();
            string += "\t";
            // save all positions and corresponding SNPs of the clade
            string += String.join(",", this.outputClades.get(node));
            string += "\n";
        }
        
        return string;
    }

    /**
     * write output to file
     * 
     * @param filename path of output file
     */
    public void saveAs(String filename){
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(filename), "utf-8"));
              writer.write(this.toFileString());
              writer.close();
        } catch (UnsupportedEncodingException e) {
            
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            
            e.printStackTrace();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
       
    }
}
