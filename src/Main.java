import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {


    public static HashMap<Phyly, Output> outputData; 

    public static void main(String[] args) {

        long startTime = System.nanoTime();
        Runtime rt = Runtime.getRuntime();
        long startMemory = (rt.totalMemory()-rt.freeMemory());
        System.out.println("Used memory: " + startMemory + " bytes");

        Args inputArgs = new Args();
        JCommander jc = JCommander.newBuilder().addObject(inputArgs).build();
        jc.setProgramName("classico.jar");
        
        try {
            jc.parse(args);
            if (inputArgs.getHelp()){
                jc.usage();
            }
            else{
                System.out.println("Read input parameters.");
                constructOutputFiles(inputArgs.getSpecifiedClades());
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                System.out.println("Tree constructed.");
                compute(snpTree, inputArgs.getSNPTable());
                saveOutput(inputArgs.getOutDir());
                System.out.println("Output saved in " + inputArgs.getOutDir());
            }
            
            final long runtime = System.nanoTime() - startTime;
            System.out.println("Run time tree construction: " + runtime / 1000000 + "ms");
            rt = Runtime.getRuntime();
            System.out.println("Used memory: " + (rt.totalMemory()-rt.freeMemory() - startMemory) + " bytes");

        } 
        catch (ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
        }
        

    }

    public static void compute(SNPTree snpTree, String filepath){
       
        try {
            long lineCount;
            Path file = Paths.get(filepath);
            try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
                lineCount = stream.count();
            }
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
            snpTree.mapSpeciesToColumn(columnNames);
            line = reader.readLine();
            int lineCounter = 0;
            System.out.println("Compute clades:");
            String progressBar = "|                         |  0%\r";
            System.out.print(progressBar);
            while(line != null){
                if ((lineCounter * 100/lineCount)%4 < (((lineCounter - 1)*100)/lineCount)%4){
                    progressBar = progressBar.substring(0, (int) ((lineCounter) * 100/lineCount)/4 ) + '#' + progressBar.substring((int)(lineCounter * 100/lineCount)/4 + 1, progressBar.length() - 5) + String.format("%1$3s", (lineCounter) * 100/lineCount) + "%\r"  ;
                    System.out.print(progressBar);
                }
                
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
                line = reader.readLine();
                lineCounter += 1;
            }
            progressBar = "|#########################|100%\r";
            System.out.println(progressBar);
            reader.close();
            
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void constructOutputFiles(List<Phyly> specifiedPhylies){
        outputData = new HashMap<>();
        for (Phyly specifiedPhyly: specifiedPhylies){
            outputData.put(specifiedPhyly, new Output());
        }
    }

    public static Output getOutputByPhyly(Phyly phyly){
        if (outputData.containsKey(phyly)){
            return outputData.get(phyly);
        }
        else{
            return null;
        }

    }

    private static void saveOutput(String directory){
        for (Phyly phyly:outputData.keySet()){
            outputData.get(phyly).saveAs(directory + "/" + phyly.toString() + ".txt");
        }
    }
}
