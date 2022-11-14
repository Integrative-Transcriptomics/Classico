import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {


    public static void main(String[] args) {
        Args inputArgs = new Args();
        JCommander jc = JCommander.newBuilder().addObject(inputArgs).build();
        jc.setProgramName("classico.jar");
        
        try {
            jc.parse(args);
            if (inputArgs.getHelp()){
                jc.usage();
            }
            else{
                System.out.println(inputArgs.getSpecifiedClades());
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                compute(snpTree, inputArgs.getSNPTable());
            }
            
          } 
        catch (ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
          }
        

    }

    public static void compute(SNPTree snpTree, String filepath){
       
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String line = reader.readLine();
            ArrayList<String> columnNames = new ArrayList<>();
            int j = 0;
            for (String cell: line.split("\t")){
                if (j > 1){
                    columnNames.add(cell);
                }
                j += 1;
            }
            snpTree.mapSpeciesToColumn(speciesToColumn(columnNames));
            line = reader.readLine();
            while(line != null){
                String[] listContent = line.split("\t");
                int position = Integer.parseInt(listContent[0]);
                SNPType referenceSNP = SNPType.fromString(listContent[1]);
                List<SNPType> snps = new ArrayList<>();
                for (int i = 2; i < listContent.length; i++){
                    snps.add(SNPType.fromString(listContent[i]));
                }
                System.out.println(position);
                snpTree.propragateSNPs(snps);
                line = reader.readLine();
            }
            reader.close();
            
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static HashMap<String, Integer> speciesToColumn(ArrayList<String> snpTableSpecies){
        HashMap<String, Integer> speciesToColumn = new HashMap<>();
        int colIdx = 0;
        for (String currSnpTableSpecies: snpTableSpecies){
            speciesToColumn.put(currSnpTableSpecies, colIdx);
            colIdx += 1;
        }
        return speciesToColumn;
    }
}
