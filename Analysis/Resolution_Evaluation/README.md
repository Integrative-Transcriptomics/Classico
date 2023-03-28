# Resolution Evaluation

This directory contains the evaluation of the resolution algorithm. 

The `generateUnresolvedFiles.py` subscript generates two SNP tables, a ground truth SNP table containing only resolved bases and a SNP table containing the same rows as the ground truth SNP table but with some unresolved bases instead of resolved bases.

The `evaluationResolution.py` subscript evaluates the performance of the resolution by comparing the resolved SNP table after the resolution with the ground truth and unresolved SNP table.

The `resolutionComparison.py` script is the script that needs to be executed to run the resolution performance as it combines the `generateUnresolvedFiles.py` script and the `evaluationResolution.py` script with CLASSICOv2.

The images visualize the results of the unresolved base resolution for the *Treponema pallidum* and *Mycobacterium leprae* datasets. The results with index 1 are from the first analysis in which only one base per row was replaced with an unresolved base and with different amounts of duplications of each row (7/14/21 or 10/20/30). The results with index 2 are from the second analysis in which multiple bases per row were replaced with unresolved bases and the results with index 3 are from the third analysis in which entire clades were replaced with unresolved bases.
