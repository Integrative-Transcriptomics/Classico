# CLASSICO

CLASSICO is a tool for propagating SNPs along a Newick tree. The tool extracts the distribution of the SNPs from the input files with respect to the least common ancestors (LCAs) that define a clade in the phylogenetic tree. The algorithm first distributes the SNPs within the reconstructed clades by the following method: The identified SNPs are propagated from the leaves towards the root. An internal node receives a SNP from its children, as long as the SNP is present among all descendants with the same allele. This process is repeated through post-ordering of the nodes up to the root of the tree. It is important to note that some SNPs might not propagate at all and will be allocated only to one of the leaves of the phylogenetic tree. If a SNP is allocated to only one internal node with the same allele, meaning it is a clade-specific SNP, then it is labelled as supporting of the tree structure, otherwise as non-supporting. CLASSICO produces two lists of SNPs, one for the supporting and one for the non-supporting SNPs

## Input files
- SNP Table (see example file X)
- Newick file (see example file Y)

## Ouptut files
- supportSplitKeys.txt: List of supporting SNPs
- notSupportSplitKeys.txt: List of non-supporting SNPs
- IDdistribution.txt: Distribution of SNPs on tree nodes

## Compilation: 
`cd src`

`jar cmfv META-INF/MANIFEST.MF classico.jar ./**/*.class`

## Running jar
`java -jar classico.jar [SNP-TABLE] [Newick-File] [Output dir]`
