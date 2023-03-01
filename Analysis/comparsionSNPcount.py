import pandas as pd
import numpy as np
import subprocess
import re

import matplotlib.pyplot as plt
import matplotlib.ticker as mticker

# ==================================================================
# THIS SCRIPT ONLY RUNS IF THE PATH OF THE OLD VERISON OF 
# CLASSICO IS CORRECTLY SPECIFIED AND IF THE RUNTIME AND MEMORY USAGE 
# IS PRINTED IN THE NEW AND OLD CLASSICO VERSION!
# ==================================================================

# ==================================================================
# this script runs the old and new classico version with increasing 
# SNP position counts and generates a plot comparing the runtime and memory 
# of the two versions
# ==================================================================

# arrays to save the results in
data_runtime = []
data_memory = []

# number of repetitions the runtime and memory analysis should be performed
repetitions = 10

# increasing SNP position counts from 0 to 20.000 SNP positions with steps of 500 SNP positions
snp_count = list(range(20001))
snp_count = snp_count[0::500]

# iterate over all SNP position counts
for count in snp_count:
    row = []
    runtime_old_ms = []
    runtime_new_ms = []
    memory_old_MB = []
    memory_new_MB = []
    count_outofmemory = 0
    
    # for each SNP position count construct SNP table with this count
    p = subprocess.Popen('python Analysis/constructLargeSNPtable.py MiniExample/SNP_tables/syphilis_reduced_snp_table_150_closest.tsv MiniExample/SNP_tables/syphilis_reduced_snp_table_150_closest' + str(count) + '.tsv ' + str(count))
    output, err = p.communicate()
    
    # repeat analysis
    for i in range(repetitions):
        # call the old CLASSICO version with 5GB maximum heap memory
        p = subprocess.Popen('java -Xmx5g -jar classico.jar MiniExample/SNP_tables/syphilis_reduced_snp_table_150_closest' + str(count) + '.tsv MiniExample/computed_trees/150_closest_UPGMA.nwk MiniExample/Results_Tree_Size', stdout=subprocess.PIPE)
        output, err = p.communicate()
        if re.search('Run time: ', output.decode()):
            # parse and save runtime
            runtime_ns = int(re.search('\d+', re.search('Run time: \d+ns', output.decode()).group()).group())
            runtime_ms = float(runtime_ns / 1000000)
            runtime_old_ms.append(runtime_ms)
            # parse and save memory
            memory_bytes = int(re.search('\d+', re.search('Memory Clade Identification: \d+ bytes', output.decode()).group()).group())
            memory_MB = memory_bytes / (1024*1024)
            memory_old_MB.append(memory_MB)
            print('old', count, 'runtime:', runtime_ms, 'memory:', memory_MB)
        else:
            print(str(count) + "out of memory")
            continue
    # repeat the analysis
    for i in range(repetitions):
        # call the new CLASSICO version without assigning additional heap memory (because it already has sufficient)
        p = subprocess.Popen('java -jar build/classicoV2.jar --snptable MiniExample/SNP_tables/syphilis_reduced_snp_table_150_closest' + str(count) + '.tsv --nwk MiniExample/computed_trees/150_closest_UPGMA.nwk --out MiniExample/Results_Tree_Size', stdout=subprocess.PIPE)
        output, err = p.communicate()
        if re.search('Run time: ', output.decode()):
            # parse and save runtime
            runtime_ns = int(re.search('\d+', re.search('Run time: \d+ns', output.decode()).group()).group())
            runtime_ms = float(runtime_ns / 1000000)
            runtime_new_ms.append(runtime_ms)
            # parse and save memory
            memory_bytes = int(re.search('\d+', re.search('Memory Clade Identification: \d+ bytes', output.decode()).group()).group())
            memory_MB = memory_bytes / (1024*1024)
            memory_new_MB.append(memory_MB)
            print('new', count, 'runtime:', runtime_ms, 'memory:', memory_MB)
        else:
            print(str(count) + "out of memory")
            continue
    
    # save results (mean and standard deviation) for this count
    data_runtime.append([np.mean(runtime_old_ms), np.std(runtime_old_ms), np.mean(runtime_new_ms), np.std(runtime_new_ms)])
    data_memory.append([np.mean(memory_old_MB), np.std(memory_old_MB), np.mean(memory_new_MB), np.std(memory_new_MB)])

# runtime results   
df_runtime = pd.DataFrame(data_runtime, index=snp_count, columns=['old version', 'old version std', 'new version', 'new version std'])

plt.rcParams.update({'font.size': 15})
# fit polynomial functions
fit_old = np.polyfit(x=df_runtime.index, y=df_runtime.loc[:,'old version'],deg=1)
fit_new = np.polyfit(x=df_runtime.index, y=df_runtime.loc[:,'new version'],deg=1)
print('old fit:' ,fit_old)
print('new fit:' , fit_new )
func_old = np.poly1d(fit_old)
func_new = np.poly1d(fit_new)
x_fit = np.linspace(0, 20000, 50)
y_old = func_old(x_fit)
y_new = func_new(x_fit)

# plot
fig, ax = plt.subplots(figsize=(8,5))
ax.errorbar(df_runtime.index, df_runtime.loc[:,'old version'], yerr=df_runtime.loc[:,'old version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'original')
ax.errorbar(df_runtime.index, df_runtime.loc[:,'new version'], yerr=df_runtime.loc[:,'new version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'improved')
ax.plot(x_fit, y_old, label = str('y = ' + str(round(fit_old[0],2)) + "x + "+ str(round(fit_old[1],2))))
ax.plot(x_fit, y_new, label = str('y = ' + str(round(fit_new[0],2)) + "x + " + str(round(fit_new[1],2))))
ax.legend()
ax.set_xlabel('SNP position count')
ax.set_ylabel('Runtime in ms')
fig.savefig('Analysis/runtime_snpPositionCount.png', bbox_inches='tight')

# repeat same for memory

df_memory = pd.DataFrame(data_memory, index=snp_count, columns=['old version', 'old version std', 'new version', 'new version std'])

fit_old = np.polyfit(x=df_memory.index, y=df_memory.loc[:,'old version'],deg=1)
fit_new = np.polyfit(x=df_memory.index, y=df_memory.loc[:,'new version'],deg=1)
func_old = np.poly1d(fit_old)
func_new = np.poly1d(fit_new)
x_fit = np.linspace(0, 20000, 50)
y_old = func_old(x_fit)
y_new = func_new(x_fit)

fig, ax = plt.subplots(figsize=(8,5))
ax.errorbar(df_memory.index, df_memory.loc[:,'old version'], yerr=df_memory.loc[:,'old version std'], fmt='-o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'original')
ax.errorbar(df_memory.index, df_memory.loc[:,'new version'], yerr=df_memory.loc[:,'new version std'], fmt='-o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'improved')
ax.plot(x_fit, y_old, label = str('y = ' + str(round(fit_old[0],2)) + "x + "+ str(round(fit_old[1],2))))
ax.plot(x_fit, y_new, label = str('y = ' + str(round(fit_new[0],2)) + "x + " + str(round(fit_new[1],2))))
ax.legend()
ax.set_xlabel('SNP position count')
ax.set_ylabel('Memory Usage in MB')
fig.savefig('Analysis/memory_snpPositionCount.png', bbox_inches='tight')