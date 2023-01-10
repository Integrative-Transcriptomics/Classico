import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Args {

    // ---------------------
    // define input flags for parsing command line parameters (with JCommander, documentation at https://jcommander.org/)
    // ---------------------

    @Parameter(names = "--nwk",  description = "path to phylogenetic tree of all species from the SNP table in newick format", 
    required = true, arity = 1, order = 1, validateValueWith  = MyFile.class)
    private String nwkTree;

    @Parameter(names = "--snptable", description = "path to SNP table (containing SNPs for different positions and all species from the newick tree)", 
    required = true, arity = 1, order = 0, validateValueWith  = MyFile.class)
    private String snpTable;

    @Parameter(names = "--out",  description = "directory where the output should be stored", 
    required = true, arity = 1, order = 2, validateValueWith  = MyDirectory.class)
    private String outDir;

    @Parameter(names = "--truth",  description = "directory where the true values are stored", 
    required = false, arity = 1, order = 4, validateValueWith  = MyFile.class)
    private String truth;

    @Parameter(names = "--clades",  description = "types of clades to compute (monophyletic, polyphyletic, paraphyletic)", 
    variableArity = true, order = 3)
    private List<Phyly> clades = Arrays.asList(Phyly.mono, Phyly.poly, Phyly.para);

    @Parameter(names = "--method",  description = "prediction method ()", order = 5, arity = 1)
    private String predMethod;

    @Parameter(names = "--predmaxdepth",  description = "max. depth used for prediciton", order = 6, arity = 1)
    private double maxDepth;


    @Parameter(names = "--help", help = true, description = "Shows this help information. A more detailed documentation can be found at https://github.com/Integrative-Transcriptomics/Classico.", order = 4)
    private boolean help = false;


    // ---------------------
    // classes for validation of input files and output directory
    // ---------------------

    public static class MyDirectory implements IValueValidator<String> {
        /* Check if input value is a directory
         */
        public void validate(String paramName, String value) throws ParameterException {
            File file = new File(value);
            if (!file.isDirectory()){
                throw new ParameterException("Parameter " + paramName + " should be a directory");
            }
        }
    }

    public static class MyFile implements IValueValidator<String> {
        /* Check if input value is a file
         */
        public void validate(String paramName, String value) throws ParameterException {
            File file = new File(value);
            if (!file.isFile()){
                throw new ParameterException("Parameter " + paramName + " should be a file");
            }
        }
    }

    // ---------------------
    // getter functions
    // ---------------------

    public Boolean getHelp(){
        return this.help;
    }

    public String getTruthDir(){
        return this.truth;
    }

    public String getOutDir(){
        return this.outDir;
    }

    public String getPredictionMethod(){
        return this.predMethod;
    }

    public double getPredictionMaxDepth(){
        return this.maxDepth;
    }

    public String getSNPTable(){
        return this.snpTable;
    }

    public String getNwk(){
        return this.nwkTree;
    }

    public List<Phyly> getSpecifiedClades(){
        return this.clades;
    }
    
}
