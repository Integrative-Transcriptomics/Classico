import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = "--nwk",  description = "phylogenetic tree of all species from the SNP table in newick format", required = true, arity = 1, order = 1)
    private String nwkTree;

    @Parameter(names = "--snptable", description = "SNP table (containing SNPs for different positions and all species from the newick tree)", required = true, arity = 1, order = 0)
    private String snpTable;

    @Parameter(names = "--out",  description = "directory where the output should be stored", required = true, arity = 1, order = 2)
    private String outDir;

    @Parameter(names = "--clades",  description = "types of clades to compute (monophyletic, polyphyletic, paraphyletic)", variableArity = true, order = 3)
    private List<Phyly> clades = Arrays.asList(Phyly.mono, Phyly.poly, Phyly.para);

    @Parameter(names = "--help", help = true, description = "Shows this help information. A more detailed documentation can be found at https://github.com/Integrative-Transcriptomics/Classico.", order = 4)
    private boolean help = false;
    
    public Boolean getHelp(){
        return this.help;
    }

    public String getOutDir(){
        return this.outDir;
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
