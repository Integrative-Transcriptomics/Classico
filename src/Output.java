import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Output {
    
    public boolean isUsed;
    private HashMap<String, HashMap<Integer, List<SNPType>>> outputClades;

    public Output(){
        outputClades = new HashMap<>();
    }

    public boolean isUsed(){
        return this.isUsed;
    }

    public void setIsUsed(boolean isUsed){
        this.isUsed = isUsed;
    }

    public void addClade(Node node, int position, SNPType snp){
        String branchString = node.getParent().getID() + "->"  + node.getID();
        if (!this.outputClades.containsKey(branchString)){
            this.outputClades.put(branchString, new HashMap<>());
        }
        if (! this.outputClades.get(branchString).containsKey(position)){
            List<SNPType> snps = new ArrayList<>();
            snps.add(snp);
            this.outputClades.get(branchString).put(position,snps);
        }
        if (! this.outputClades.get(branchString).get(position).contains(snp)){
            this.outputClades.get(branchString).get(position).add(snp);
        }
    }


    public String toFileString(){
        String string = "";
        for (String branchString: this.outputClades.keySet()){
            string += branchString;
            string += "\t";
            string += this.outputClades.get(branchString).size();
            string += "\t";
            for (int position: this.outputClades.get(branchString).keySet()){
                string += position;
                string += ":";
                string += this.outputClades.get(branchString).get(position);
            }
            string += "\n";
        }
        
        return string;
    }

    public void saveAs(String filename){
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
              new FileOutputStream(filename), "utf-8"));
              writer.write(this.toFileString());
              writer.close();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
    }
}
