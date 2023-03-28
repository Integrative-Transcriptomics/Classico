# Runtime and Memory Analysis

This directory contains the runtime and memory analysis and comparison between the first and the second version of CLASSICO. 

**! The scripts in this directory can only be executed if the CLASSICOv1 is in the root directory as `classico.jar` and the first and second version print the runtime and memory consumption. !**

The `constructLargeSNPTable.py` is a script that constructs a SNP table with many SNP positions.

The `comparisonSampleCount.py` script compares the runtime and memory consumption of the two CLASSICO versions with an increasing number of samples and generates a plot as an output (`memorySampleCount.png` and `runtimeSampleCount.png`).

The `comparisonSNPCount.py` script compares the runtime and memory consumption of the two CLASSICO versions with an increasing number of SNPs and generates a plot as an output (`memorySNPPOsitionCount.png` and `runtimeSNPPositionCount.png`).

The `runtimeAnalysisNewVersion.py` script plots the runtime for the different steps of CLASSICO and compares them between the *Treponema pallidum* and *Mycobacterium leprae* dataset.