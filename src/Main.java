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
    public static long predictionTime =  0;

    public static void main(String[] args) {

        long startTime = System.nanoTime();
        double startSeconds = (double)startTime / 1_000_000_000.0;
        Runtime rt = Runtime.getRuntime();
        rt.gc();
        System.out.println("Start time: " + startSeconds);
        // input arguments
        Args inputArgs = new Args();
        JCommander jc = JCommander.newBuilder().addObject(inputArgs).build();
        jc.setProgramName("classico.jar");
        
        try {
            // validate input arguments
            jc.parse(args);
            if (inputArgs.getHelp()){
                jc.usage();
            }
            else{
                // read input arguments
                System.out.println("Read input parameters.");
                constructOutputFiles(inputArgs.getSpecifiedClades());
                String predicitonMethod = inputArgs.getPredictionMethod();
                double maxDepth = inputArgs.getPredictionMaxDepth();
                Boolean predictBoolean = inputArgs.getPredict();
                String outputDirectory = inputArgs.getOutDir();
                String SNPtableFilepath = inputArgs.getSNPTable();

                System.out.println(predictBoolean);
                // -----------------------------------------------------------------------
                // MAIN ALGORITHM
                // -----------------------------------------------------------------------
                
                // parse SNP Tree from input and define specified clades
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                System.out.println("Tree constructed.");
                // propragate SNP along tree, identify clades and predict unresolved bases if specified
                long initializationTime = System.nanoTime();
                System.out.println("Initialization time " + (initializationTime - startTime));
                propragateSNPsAndIdentifyClades(snpTree, SNPtableFilepath, maxDepth, predicitonMethod, predictBoolean, outputDirectory);
                // save summary statistics
                
                long cladeIdentificationTime = System.nanoTime();
                System.out.println("Clade identification time " + ((cladeIdentificationTime - initializationTime) - predictionTime));
                
                System.out.println("Write Statistics");
                String filepathStat = outputDirectory + "/Statistics.txt";
                writeToStatisticsFiles(filepathStat);
                // save ID distribution across tree
                System.out.println("Save ID Distribution");
                snpTree.saveIDDistribution(outputDirectory + "/IDDistribution.txt");
                // save computed clades
                System.out.println("Save output");
                saveOutput(outputDirectory, false);

                // ---------------------------------------------------------------------
                // SECOND ITERATION OF THE ALGORITHM ON PREDICTED SNP TABLE IF SPECIFIED
                // ---------------------------------------------------------------------
                long saveTime = System.nanoTime();
                System.out.println("Write Output time " + (saveTime - cladeIdentificationTime));

                long endTime = System.nanoTime();
                rt = Runtime.getRuntime();
                long endMemory = (rt.totalMemory()-rt.freeMemory());
                System.out.println("Memory Clade Identification: " + (endMemory) + " bytes");
                System.out.println("Run time: " + (endTime - startTime) + "ns");

                System.out.println("Prediction Time: " + predictionTime);

                if (predictBoolean){
                    // construct new Output Files
                    constructOutputFiles(inputArgs.getSpecifiedClades());
                    
                    // propragate SNP along tree, identify clades and predict unresolved bases if specified
                    int startIdxFilepath = SNPtableFilepath.lastIndexOf('/') + 1;
                    String predictedSNPTableFilepath = outputDirectory + "/" + SNPtableFilepath.substring(startIdxFilepath).replace(".", "_resolved.");
                    long cladeIdentificationTime2start = System.nanoTime();
                    propragateSNPsAndIdentifyClades(snpTree, predictedSNPTableFilepath, maxDepth, predicitonMethod, false, outputDirectory);
                    // save second summary statistics
                    long cladeIdentificationTime2end = System.nanoTime();
                    System.out.println("Second: Clade identification time " + (cladeIdentificationTime2end - cladeIdentificationTime2start));

                    long outputTime2start = System.nanoTime();
                    filepathStat = outputDirectory + "/Statistics_resolved.txt";
                    writeToStatisticsFiles(filepathStat);
                    // save second computed clades
                    saveOutput(outputDirectory, true);
                    long outputTime2end = System.nanoTime();
                    System.out.println("Second: Write Output Time " + (outputTime2end - outputTime2start));
                }

                System.out.println("Output saved in " + outputDirectory);
            }
        } 
        catch(ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
        }
    }

    public static void propragateSNPsAndIdentifyClades(SNPTree snpTree, String filepath, double maxDepth, String predicitonMethod, Boolean predictBoolean, String outputDir){
       
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

            int startIdxFilepath = filepath.lastIndexOf('/') + 1;
            String newFilepath = outputDir + "/" + filepath.substring(startIdxFilepath).replace(".", "_resolved.");
            
            if (predictBoolean){
                FileWriter fw = new FileWriter(newFilepath);
                BufferedWriter writer = new BufferedWriter(fw);
                writer.write(line + '\n');
                writer.close();
            }
            

            line = reader.readLine();
            int lineCounter = 0;
            if(predictBoolean){
                System.out.println("Compute clades:");
            }
            else{
                System.out.println("Compute clades on predicted SNPs:");
            }
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
                
                if (predictBoolean){
                    long predictionTimeStart = System.nanoTime();
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
                    FileWriter fw = new FileWriter(newFilepath, true);
                    BufferedWriter writer = new BufferedWriter(fw);
                    writer.write(newPredictionLine);
                    writer.close();

                    long predictionTimeEnd = System.nanoTime();
                    predictionTime += (predictionTimeEnd - predictionTimeStart);

                }

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

    private static void saveOutput(String directory, boolean predicted){
        for (Phyly phyly:outputData.keySet()){
            if (!predicted){
                outputData.get(phyly).saveAs(directory + "/" + phyly.toString() + ".txt");
            }
            else{
                outputData.get(phyly).saveAs(directory + "/" + phyly.toString() + "_resolved.txt");
            }
        }
    }
}
