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
    
    private HashMap<Node, ArrayList<String>> outputClades;

    public Output(){
        outputClades = new HashMap<>();
    }

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


    public String toFileString(){
        String string = "";
        for (Node node: this.outputClades.keySet()){
            if (node.getParent() == null){
                string += "root";
            }
            else{ 
                string += node.getParent().getID();
            }
            string += "->";
            string += node.getID();
            string += "\t";
            string += this.outputClades.get(node).size();
            string += "\t";
            string += String.join(",", this.outputClades.get(node));
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
