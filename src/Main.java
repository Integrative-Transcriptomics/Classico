import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.stream.Stream;

// JCommander for CLI
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {

    public static HashMap<Phyly, Output> outputData; 
    public static HashMap<Phyly, Integer> alleleStatistics;
    public static HashMap<Phyly, Integer> snpStatistics;
    public static long resolutionTime =  0;
    public static String resolutionMethod;
    public static double relativeMaxDepth;
    public static String outputDirectory;

    public static void main(String[] args) {
        
        // measure Time for Runtime analysis
        long startTime = System.nanoTime();
        Runtime rt = Runtime.getRuntime();
        rt.gc();
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
                resolutionMethod = inputArgs.getResolutionMethod();
                relativeMaxDepth = inputArgs.getRelativeMaxDepth();
                Boolean resolveBoolean = inputArgs.getResolve();
                outputDirectory = inputArgs.getOutDir();
                String SNPtableFilepath = inputArgs.getSNPTable();
                
                // -----------------------------------------------------------------------
                // MAIN ALGORITHM
                // -----------------------------------------------------------------------
                
                // parse SNP Tree from input and define specified clades
                SNPTree snpTree = new SNPTree(inputArgs.getNwk(), inputArgs.getSpecifiedClades());
                System.out.println("Tree constructed.");
                
                // ***********************************************
                // PRINT STATEMENTS FOR RUNTIME ANALYSIS ARE COMMENTED
                // ***********************************************
                long initializationTime = System.nanoTime();
                //System.out.println("Initialization time " + (initializationTime - startTime));
                long cladeIdentificationTime1start = System.nanoTime();
                
                // propragate SNP along tree, identify clades and predict unresolved bases if specified
                propagateSNPsAndIdentifyClades(snpTree, SNPtableFilepath, resolveBoolean);
                
                
                long cladeIdentificationTime1end = System.nanoTime();
                //System.out.println("Clade identification time " + ((cladeIdentificationTime1end - cladeIdentificationTime1start) - resolutionTime));
                long outputTime1start = System.nanoTime();
                System.out.println("Write Statistics");
                // save summary statistics
                String filepathStat = outputDirectory + "/Statistics.txt";
                writeToStatisticsFiles(filepathStat, inputArgs.getSpecifiedClades());
                
                // save ID distribution across tree
                System.out.println("Save ID Distribution");
                snpTree.saveIDDistribution(outputDirectory + "/IDDistribution.txt");
                
                // save computed clades
                System.out.println("Save output");
                saveOutput(outputDirectory, false);
                long outputTime1end = System.nanoTime();
                //System.out.println("Write Output time " + (outputTime1end - outputTime1start));
                //System.out.println("Resolution Time: " + resolutionTime);

                // get memory consumption for comparison with old CLASSICO version
                rt = Runtime.getRuntime();
                long endMemory = (rt.totalMemory()-rt.freeMemory());
                //System.out.println("Memory Clade Identification: " + (endMemory) + " bytes");


                // ---------------------------------------------------------------------
                // SECOND ITERATION OF THE ALGORITHM ON RESOLVED SNP TABLE IF SPECIFIED
                // ---------------------------------------------------------------------
                
                if (resolveBoolean){
                    // construct new Output Files
                    constructOutputFiles(inputArgs.getSpecifiedClades());
                    
                    // propragate SNP along tree, identify clades and predict unresolved bases if specified
                    int startIdxFilepath = SNPtableFilepath.lastIndexOf('/') + 1;
                    String resolvedSNPTableFilepath = outputDirectory + "/" + SNPtableFilepath.substring(startIdxFilepath).replace(".", "_resolved.");
                    long cladeIdentificationTime2start = System.nanoTime();
                    propagateSNPsAndIdentifyClades(snpTree, resolvedSNPTableFilepath, false);
                    
                    // save second summary statistics
                    long cladeIdentificationTime2end = System.nanoTime();
                    //System.out.println("Second: Clade identification time " + (cladeIdentificationTime2end - cladeIdentificationTime2start));
                    long outputTime2start = System.nanoTime();
                    filepathStat = outputDirectory + "/Statistics_resolved.txt";
                    writeToStatisticsFiles(filepathStat, inputArgs.getSpecifiedClades());

                    // save second computed clades
                    saveOutput(outputDirectory, true);
                    long outputTime2end = System.nanoTime();
                    //System.out.println("Second: Write Output Time " + (outputTime2end - outputTime2start));
                }

                System.out.println("Output saved in " + outputDirectory);
                // print runtime for comparison with old CLASSICO version
                long endTime = System.nanoTime();
                //System.out.println("Run time: " + (endTime - startTime) + "ns");
            }
        } 
        // if parameter exception print help menu
        catch(ParameterException e) {
            System.err.println(e.getLocalizedMessage());
            jc.usage();
        }
    }

    /**
     * Labels nodes of phylogenetic tree, identifies specified clades and resolves unresolved bases (if specified) for all positions in the SNP table.
     * 
     * @param snpTree {@link SNPTree} structure parsed from Newick input
     * @param snpTableFilepath path to SNP table
     * @param resolveBoolean boolean describing whether unresolved bases should be resolved
     */
    public static void propagateSNPsAndIdentifyClades(SNPTree snpTree, String snpTableFilepath, Boolean resolveBoolean){
       
        try {
            // compute the total line count of the SNP table to know the fraction of processed bases for the progress animation
            long lineCount;
            Path file = Paths.get(snpTableFilepath);
            try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
                lineCount = stream.count();
            }

            // read SNP table file
            BufferedReader reader = new BufferedReader(new FileReader(snpTableFilepath));
            
            // process first line to map samples to column of SNP table
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

            // construct new SNP table filepath for resolved bases
            int startIdxFilepath = snpTableFilepath.lastIndexOf('/') + 1;
            String newFilepath = outputDirectory + "/" + snpTableFilepath.substring(startIdxFilepath).replace(".", "_resolved.");
            
            // if resolution is specified initialize this file with the header containing the sample names
            if (resolveBoolean){
                FileWriter fw = new FileWriter(newFilepath);
                BufferedWriter writer = new BufferedWriter(fw);
                writer.write(line + '\n');
                writer.close();
            }
            
            // start of clade identification and resolution
            line = reader.readLine();
            int lineCounter = 0;
            if(resolveBoolean){
                System.out.println("Compute clades:");
            }
            else{
                System.out.println("Compute clades on resolved SNPs:");
            }
            String progressBar = "|                         |  0%\r";
            System.out.print(progressBar);

            // initialize Statistics
            alleleStatistics = new HashMap<>();
            snpStatistics = new HashMap<>();
            for (Phyly phyly: Phyly.values()){
                snpStatistics.put(phyly, 0);
                alleleStatistics.put(phyly, 0);
            }
            
            // iterate over all positions
            while(line != null){
                
                // update animation progress bar
                if ((lineCounter * 100/lineCount)%4 < (((lineCounter - 1)*100)/lineCount)%4){
                    progressBar = progressBar.substring(0, (int) ((lineCounter) * 100/lineCount)/4 ) + '#' + progressBar.substring((int)(lineCounter * 100/lineCount)/4 + 1, progressBar.length() - 5) + String.format("%1$3s", (lineCounter) * 100/lineCount) + "%\r"  ;
                    System.out.print(progressBar);
                }
                
                // update position of SNP Tree and initalize allele statistic and unresolved clades
                String[] listContent = line.split("\t");
                int position = Integer.parseInt(listContent[0]);
                snpTree.setPosition(position);
                snpTree.initializeAlleleStatistic();
                snpTree.initializeUnresolvedClades();
                
                // extract SNPs from current position and check that they are not equal to the reference SNP
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

                // --------------------------------------------------------
                // MAIN STEP: propagate SNPs and identify clades for current position 
                // --------------------------------------------------------
                snpTree.propagateSNPs(snps);

                // update allele statistic
                addSNPAlleleStatistics(snpTree.alleleStatistics);
                
                // --------------------------------------------------------
                // MAIN STEP: resolution if specifies
                // --------------------------------------------------------
                if (resolveBoolean){
                    // measure resolution time separately
                    long resolutionTimeStart = System.nanoTime();

                    // only resolve if there are unresolved bases
                    if (snps.contains(SNPType.N)){                    
                        
                        // iterate over roots of unresolved base clades that have been identified during SNP propagation
                        for (Node rootN: snpTree.cladesUnresolvedBases.keySet()){
                            
                            // resolve root of unresolved clade
                            SNPType resolution = rootN.resolve(relativeMaxDepth, resolutionMethod);
                        
                            // Assign the resolved base to all leafs of the clade
                            for (Node leaf: snpTree.cladesUnresolvedBases.get(rootN)){
                                leaf.setSNPOptions(resolution);
                                
                                // replace resolved base with original base in new SNP table 
                                int column = snpTree.speciesToColumn.get(leaf.getName());
                                ArrayList<String> listSNPoptions = new ArrayList<String>();
                                for (SNPType snp: leaf.getSNPOptions()){
                                    listSNPoptions.add(snp.toString());
                                }
                                listContent[column + 2] =  String.join("," , listSNPoptions);
                            }
                        }
                        
                                            
                    }   
                    
                    
                    String newResolutionLine =  String.join("\t", listContent) + "\n";
                    // write new line of SNP table to output
                    FileWriter fw = new FileWriter(newFilepath, true);
                    BufferedWriter writer = new BufferedWriter(fw);
                    writer.write(newResolutionLine);
                    writer.close();

                    //  measure resolution time
                    long resolutionTimeEnd = System.nanoTime();
                    resolutionTime += (resolutionTimeEnd - resolutionTimeStart);

                }
                // continue with next line of SNP table
                line = reader.readLine();
                lineCounter += 1;
            }
            // end of clade identification
            progressBar = "|#########################|100%\r";
            System.out.println(progressBar);
            reader.close();
            
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }


    /**
     * add result of allele counts for this position to overall allele statistics
     * 
     * @param alleleStatisticsList map that links all phylies to all alleles of this phyly
     */
    private static void addSNPAlleleStatistics(HashMap<Phyly, ArrayList<SNPType>> alleleStatisticsList){
        
        for (Phyly phyly: alleleStatisticsList.keySet()){
            int previousCount = alleleStatistics.get(phyly);
            int newCount = previousCount + alleleStatisticsList.get(phyly).size();
            alleleStatistics.replace(phyly, newCount);
        }
    }

    /**
     * generates an output file based on computed allele and snp statistics
     * 
     * @param filepathStat filepath of statistics file
     */
    private static void writeToStatisticsFiles(String filepathStat, List<Phyly> specifiedPhylies){
       
        String snpTypeStat = "";
        String phylyStat = "";
        if (specifiedPhylies.contains(Phyly.mono)){
            snpTypeStat += "Monophyletic Alleles: " + alleleStatistics.get(Phyly.mono);
            phylyStat += "Monophyletic SNPs: " + snpStatistics.get(Phyly.mono);
        }
        if (specifiedPhylies.contains(Phyly.para)){
            if (specifiedPhylies.contains(Phyly.mono)){
                snpTypeStat += "\n";
                phylyStat += "\n";
            }
            snpTypeStat += "Paraphyletic Alleles: " + alleleStatistics.get(Phyly.para);
            phylyStat += "Paraphyletic SNPs: " + snpStatistics.get(Phyly.para);
        }
        if (specifiedPhylies.contains(Phyly.poly)){
            if (specifiedPhylies.contains(Phyly.mono) || specifiedPhylies.contains(Phyly.para)){
                snpTypeStat += "\n";
                phylyStat += "\n";
            }
            snpTypeStat += "Polyphyletic Alleles: " + alleleStatistics.get(Phyly.poly);
            phylyStat += "Polyphyletic SNPs: " + snpStatistics.get(Phyly.poly);
        }
        snpTypeStat += "\n";
        phylyStat += "\n";
        try {
            FileWriter fw = new FileWriter(filepathStat);
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(phylyStat);
            writer.write(snpTypeStat);
            writer.close();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }

    /**
     * constructs an {@link Output} for all specified phylies
     * 
     * @param specifiedPhylies phylies for which output files should be constructed
     */
    private static void constructOutputFiles(List<Phyly> specifiedPhylies){
        outputData = new HashMap<>();
        for (Phyly specifiedPhyly: specifiedPhylies){
            outputData.put(specifiedPhyly, new Output());
        }
    }

    /**
     * returns {@link Output} for a phyly
     * 
     * @param phyly 
     * @return {@link Output} if it exists for this phyly
     */
    public static Output getOutputByPhyly(Phyly phyly){
        if (outputData.containsKey(phyly)){
            return outputData.get(phyly);
        }
        else{
            return null;
        }
    }

    /**
     * constructs clade outputs
     * 
     * @param directory directory where the output files should be saved
     * @param resolved whether the clades are based on resolved bases
     */
    private static void saveOutput(String directory, boolean resolved){
        for (Phyly phyly:outputData.keySet()){
            if (!resolved){
                outputData.get(phyly).saveAs(directory + "/" + phyly.toString() + ".txt");
            }
            else{
                outputData.get(phyly).saveAs(directory + "/" + phyly.toString() + "_resolved.txt");
            }
        }
    }
}
