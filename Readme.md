# CLASSICO

CLASSICO is a tool for propagating SNPs along a Newick tree. The tool identifies branches that lead to root nodes of monophyletic, paraphyletic and polyphyletic clades of SNPs based on the distribution of a SNP table. The algorithm first distributes the SNPs within the reconstructed clades with Fitch's algorithm [1], however, nodes that would be randomly labeled with Fitch, because there multiple bases are possible for this node, are left ambiguous. Unresolved bases are only propagated as long as all children of a node are labeled with an unresolved base. Then, the branches that lead to root nodes of monophyletic, paraphyletic and polyphyletic clades are computed and saved. Besides that, statistics of the allele count for each clade type and the SNP count of each clade type are returned. 

Additionally, CLASSICO provides the option to resolve unresolved bases based on close nodes of the unresolved base in the phylogenetic tree, the so-called neighborhood of a node. Three different methods that define the neighborhood are implemented, i.e. the only-parent, parent-sibling and cladewise method. The methods extend the neighborhood iteratively until the depth of the neighborhood equals the specified relative maximum depth parameter. For each base a score is computed where nodes that are closer to the unresolved base are weighted more than nodes that are further apart. The base with the highest score is the resolved base for the entire clade of unresolved bases, if two or more options have the same score the base remains unresolved.
After the resolution, CLASSICO propagates the SNPs and computes the phylogenetic clades again.

## Flags
Required:
- snptable: Path to SNP Table (see example file Data/mini_snp.tsv)
- nwk: Path to Newick file (see example file Data/mini_nwk.nwk) 
- out: Output direcotry

Optional:
- clades: Set of phylogenetic clades that should be computed i.e. monophyletic, paraphyletic and polyphyletic clades (default: mono, para, poly)
- resolve: Specifies whether unresolved bases should be resolved (default: false)
- method: neighborhood extension method i.e. only-parent, parent-sibling, cladewise method (default: only-parent)
- relmaxdepth: relative maximum depth, value in range 0-1 (default: 0.2)
- help: prints the help menu

## Ouptut files
Standard:
- mono.txt: List of monophyletic roots
- para.txt: List of paraphyletic roots
- poly.txt: List of polyphyletic roots
- IDdistribution.txt: Distribution of internal IDs to taxa labels
- Statistics.txt: Allele and SNP statistics

Additional output if resolution specified:
- [FilenameSNPTable]_resolved.tsv: SNP table after resolution
- mono_resolved.txt: List of monophyletic roots after resolution
- para_resolved.txt: List of paraphyletic roots after resolution
- poly_resolved.txt: List of polyphyletic roots after resolution
- Statistics_resolved.txt: Allele and SNP statistics after resolution

## Compilation
The .jar file was built using Java version 17.0.5. One can build the tool for other Java version using the following commands:

`cd src`

`javac -cp ../lib/jcommander-1.82.jar *.java`

`jar cvfm classicoV2.jar META-INF/MANIFEST.MF *`

## Running jar
Simple Example:

`java -jar classicoV2.jar --snptable Data/mini_snp.tsv --nwk Data/mini_nwk.nwk --out Data`

Advanced example with resolution of unresolved bases:

`java -jar classicoV2.jar --snptable Data/mini_snp.tsv --nwk Data/mini_nwk.nwk --out Data --resolve --method cladewise --relmaxdepth 0.5`


## Repository structure

The source code and a compiled `.jar` file are in the `src` directory. The `lib` directory contains the jcommander framework (https://jcommander.org), that was used for parsing the input parameters. In the `Analysis` directory the scripts and resulting plots of all analyses are stored. The `Data` directory contains a mini example, the validation dataset, the *Mycobacterium leprae* and *Treponema pallidum* datasets as well as the additional *Mycobacterium leprae* dataset used for runtime and memory analysis. Further, the outputs are stored in the `Data` directory.

## References
[1] Walter M Fitch. Toward defining the course of evolution: minimum
change for a specific tree topology. Systematic Biology, 20(4):406–416,
1971.