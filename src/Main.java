import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {

    public static HashMap<Phyly, Output> outputData; 
    public static HashMap<Phyly, Integer> snpTypeStatistics;
    public static HashMap<Phyly, Integer> phylyStatistics;

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
                String predicitonMethod = inputArgs.getPredictionMethod();
                double maxDepth = inputArgs.getPredictionMaxDepth();
                System.out.println(maxDepth);
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                System.out.println("Tree constructed.");
                propragateSNPsAndIdentifyClades(snpTree, inputArgs.getSNPTable(), maxDepth, predicitonMethod);
                // write statistics
                String filepathStat = inputArgs.getOutDir() + "/Statistics.txt";
                writeToStatisticsFiles(filepathStat);
                
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

    public static void propragateSNPsAndIdentifyClades(SNPTree snpTree, String filepath, double maxDepth, String predicitonMethod){
       
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

            String newFilepath = filepath.replace(".", "_predicted.");

            FileWriter fw = new FileWriter(newFilepath);
            //BufferedWriter writer give better performance
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(line + '\n');
            writer.close();
            

            line = reader.readLine();
            int lineCounter = 0;
            System.out.println("Compute clades:");
            String progressBar = "|                         |  0%\r";
            System.out.print(progressBar);

            // Initialize Statistics
            snpTypeStatistics = new HashMap<>();
            phylyStatistics = new HashMap<>();
            for (Phyly phyly: Phyly.values()){
                phylyStatistics.put(phyly, 0);
                snpTypeStatistics.put(phyly, 0);
            }
            
            while(line != null){
                if ((lineCounter * 100/lineCount)%4 < (((lineCounter - 1)*100)/lineCount)%4){
                    progressBar = progressBar.substring(0, (int) ((lineCounter) * 100/lineCount)/4 ) + '#' + progressBar.substring((int)(lineCounter * 100/lineCount)/4 + 1, progressBar.length() - 5) + String.format("%1$3s", (lineCounter) * 100/lineCount) + "%\r"  ;
                    System.out.print(progressBar);
                }
                
                String[] listContent = line.split("\t");
                int position = Integer.parseInt(listContent[0]);
                snpTree.setPosition(position);
                snpTree.initializeStatistic();
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
                addSNPAlleleStatistics(snpTree.snpTypeStatistics);

                // Prediction of unresolved bases
                if (snps.contains(SNPType.N) && predicitonMethod != null){                    
                    for (Node rootN: snpTree.cladesUnresolvedBases.keySet()){
                        Set<SNPType> prediction = rootN.predict(maxDepth, predicitonMethod);
                        
                            for (Node leaf: snpTree.cladesUnresolvedBases.get(rootN)){
                                
                                leaf.setSNPOptions(prediction);
                                int column = snpTree.speciesToColumn.get(leaf.getName());
                                ArrayList<String> listSNPoptions = new ArrayList<String>();
                                for (SNPType snp: leaf.getSNPOptions()){
                                    listSNPoptions.add(snp.toString());
                                }
                                listContent[column + 2] =  String.join("," , listSNPoptions);
                                
                            }
                        
                    }
                    
                                        
                }

                String newPredictionLine =  String.join("\t", listContent) + "\n";
                // write (new) SNP labels to output
                fw = new FileWriter(newFilepath, true);
                writer = new BufferedWriter(fw);
                writer.write(newPredictionLine);
                writer.close();
                
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


    private static void addSNPAlleleStatistics(HashMap<Phyly, ArrayList<SNPType>> alleleStatistics){
        
        for (Phyly phyly: alleleStatistics.keySet()){
            int previousCount = snpTypeStatistics.get(phyly);
            int newCount = previousCount + alleleStatistics.get(phyly).size();
            snpTypeStatistics.replace(phyly, newCount);
        }
    }

    private static void writeToStatisticsFiles(String filepathStat){
       
        String snpTypeStat = "Monophyletic Alleles: " + snpTypeStatistics.get(Phyly.mono) +
                            "\nParaphyletic Alleles: " + snpTypeStatistics.get(Phyly.para) + 
                            "\nPolyphyletic Alleles: " + snpTypeStatistics.get(Phyly.poly)
                            + "\n";
        String phylyStat = "Monophyletic SNPs: " + phylyStatistics.get(Phyly.mono) + 
                            "\nParaphyletic SNPs: " + phylyStatistics.get(Phyly.para) +
                            "\nPolyphyletic SNPs: " + phylyStatistics.get(Phyly.poly) + "\n";
        try {
            //Here true is to append the content to file
            FileWriter fw = new FileWriter(filepathStat);
            //BufferedWriter writer give better performance
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(phylyStat);
            writer.write(snpTypeStat);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
