import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Period;
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
                String pathTruth = inputArgs.getTruthDir();
                System.out.println(pathTruth);
                String predicitonMethod = inputArgs.getPredictionMethod();
                double maxDepth = inputArgs.getPredictionMaxDepth();
                System.out.println(maxDepth);
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                System.out.println("Tree constructed.");
                compute(snpTree, inputArgs.getSNPTable(), maxDepth, predicitonMethod, pathTruth);
                // save ID distribution across tree
                snpTree.saveIDDistribution(inputArgs.getOutDir());
                // save clades
                saveOutput(inputArgs.getOutDir());
                System.out.println("Output saved in " + inputArgs.getOutDir());
            }
            final long runtime = System.nanoTime() - startTime;
            System.out.println("Run time tree construction: " + runtime / 1000000 + "ms");
            rt = Runtime.getRuntime();
            System.out.println("Used memory: " + (rt.totalMemory()-rt.freeMemory() - startMemory) + " bytes");
        } 
        catch(ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
        }
    }

    public static void compute(SNPTree snpTree, String filepath, double maxDepth, String predicitonMethod, String pathTruth){
       
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
            System.out.println("Leafs: " + snpTree.leafs.size());
            System.out.println("Depth: " + snpTree.depth);
            line = reader.readLine();
            int lineCounter = 0;
            System.out.println("Compute clades:");
            String progressBar = "|                         |  0%\r";
            System.out.print(progressBar);
            boolean containsSingleN = false;
            boolean containsMultiN = false;
            int correctPrediction = 0;
            int incorrectPrediction = 0;
            HashMap<Integer, SNPType> predictions = new HashMap<>();
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
                if (snps.contains(SNPType.N)){
                    int countN = 0;
                    for (int i = 0; i < snps.size(); i ++){
                        if(snps.get(i) == SNPType.N){
                            countN += 1;
                        }
                    }
                    if(countN > 1){
                        containsMultiN = true;
                    }
                    else if (countN == 1){
                        
                        List<Node> listNodesN = snpTree.getLeafNodesBySNPType(SNPType.N);
                        for (Node currNodeN: listNodesN){
                            List<SNPType> prediction = currNodeN.predict(maxDepth, predicitonMethod);
                            for (SNPType snpType: prediction){
                                predictions.put(position, snpType);
                            }
                            if (!(pathTruth == null)){
                                
                            SNPType truth = getTruth(currNodeN.getName(), position, pathTruth, snpTree);
                            if (prediction.contains(truth)){
                                correctPrediction += 1;
                            }
                            else{
                                incorrectPrediction += 1;
                            }
                            }
                            
                        }
                        containsSingleN = true;
                    }
                }
                    
                
                line = reader.readLine();
                lineCounter += 1;
            }
            progressBar = "|#########################|100%\r";
            System.out.println(progressBar);

            System.out.println("Correct Predictions: " + correctPrediction);
            System.out.println("Incorrect Predictions: " + incorrectPrediction);
            reader.close();
            
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static SNPType getTruth(String name, int position, String truth, SNPTree snpTree){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(truth));
            String line = reader.readLine();
            line = reader.readLine();
            while (line != null){
                String[] splits = line.split("\t");
                int currPosition = Integer.parseInt(splits[0]);
                if (currPosition == position){
                    int column = snpTree.speciesToColumn.get(name) + 2;
                    SNPType snpType = SNPType.fromString(splits[column]);
                    reader.close();
                    return snpType;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
