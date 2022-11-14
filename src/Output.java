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
    
    private HashMap<Node, HashMap<Integer, List<SNPType>>> outputClades;

    public Output(){
        outputClades = new HashMap<>();
    }

    public void addClade(Node node, int position, SNPType snp){
        
        if (!this.outputClades.containsKey(node)){
            this.outputClades.put(node, new HashMap<>());
        }
        if (! this.outputClades.get(node).containsKey(position)){
            List<SNPType> snps = new ArrayList<>();
            snps.add(snp);
            this.outputClades.get(node).put(position,snps);
        }
        if (! this.outputClades.get(node).get(position).contains(snp)){
            this.outputClades.get(node).get(position).add(snp);
        }
    }


    public String toFileString(){
        String string = "";
        for (Node node: this.outputClades.keySet()){
            string += node.getParent().getID();
            string += "->";
            string += node.getID();
            string += "\t";
            string += this.outputClades.get(node).size();
            string += "\t";
            for (int position: this.outputClades.get(node).keySet()){
                string += position;
                string += ":";
                string += this.outputClades.get(node).get(position);
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
