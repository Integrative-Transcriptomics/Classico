import pandas as pd
import numpy as np
import subprocess
import re

import matplotlib.pyplot as plt

# ==================================================================
# THIS SCRIPT ONLY RUNS IF THE PATH OF THE OLD VERISON OF 
# CLASSICO IS SPECIFIED AND IF THE RUNTIME AND MEMORY USAGE IS 
# PRINTED IN THE NEW CLASSICO VERSION!
# ==================================================================

# ==================================================================
# this script runs the old and new classico version with increasing 
# sample counts and generates a plot comparing the runtime and memory 
# of the two versions
# ==================================================================

# arrays to save the results in
data_runtime = []
data_memory = []

# number of repetitions the runtime and memory analysis should be performed
repetitions = 10

# increasing sample counts from 100 - 750 samples with steps of 50 samples
sample_count = list(range(100,800))
sample_count = sample_count[0::50]

# iterate over all sample counts
for count in sample_count:
    row = []
    runtime_old_ms = []
    runtime_new_ms = []
    memory_old_MB = []
    memory_new_MB = []
    count_outofmemory = 0
    
    # repeat the analysis
    for i in range(repetitions):
        # call the old CLASSICO version with 5GB maximum heap memory
        p = subprocess.Popen('java -Xmx5g -jar classico.jar  Data/Runtime_memory_analysis_SNP_tables/syphilis_reduced_snp_table_' + str(count) + '_closest.tsv Data/Runtime_memory_analysis_newick_trees/' + str(count) + '_closest_UPGMA.nwk Data/Output_Runtime_Memory_Analysis', stdout=subprocess.PIPE)
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
        p = subprocess.Popen('java -jar src/classicoV2.jar  --snptable Data/Runtime_memory_analysis_SNP_tables/syphilis_reduced_snp_table_' + str(count) + '_closest.tsv --nwk Data/Runtime_memory_analysis_newick_trees/' + str(count) + '_closest_UPGMA.nwk --out Data/Output_Runtime_Memory_Analysis', stdout=subprocess.PIPE)
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
df_runtime = pd.DataFrame(data_runtime, index=sample_count, columns=['old version', 'old version std', 'new version', 'new version std'])

# fit polynomial functions
fit_old = np.polyfit(x=df_runtime.index, y=df_runtime.loc[:,'old version'],deg=2)
fit_new = np.polyfit(x=df_runtime.index, y=df_runtime.loc[:,'new version'],deg=1)
print('old fit:' ,fit_old)
print('new fit:' , fit_new )
func_old = np.poly1d(fit_old)
func_new = np.poly1d(fit_new)
x_fit = np.linspace(100, 750, 50)
y_old = func_old(x_fit)
y_new = func_new(x_fit)

# plot results
plt.rcParams.update({'font.size': 16})    
fig, ax = plt.subplots(figsize=(8,5))
ax.errorbar(df_runtime.index, df_runtime.loc[:,'old version'], yerr=df_runtime.loc[:,'old version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'original')
ax.errorbar(df_runtime.index, df_runtime.loc[:,'new version'], yerr=df_runtime.loc[:,'new version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'improved')
ax.plot(x_fit, y_old, label = str('y = ' + str(round(fit_old[0],2)) + "x$^2$ + "+ str(round(fit_old[1],2)) + "x + " + str(round(fit_old[2],2))))
ax.plot(x_fit, y_new, label = str('y = ' + str(round(fit_new[0],2)) + "x + " + str(round(fit_new[1],2))))
ax.legend()
ax.set_xlabel('Sample count')
ax.set_ylabel('Runtime in ms')
fig.savefig('Analysis/Runtime_Analysis/runtime_sampleCount.png', bbox_inches='tight')

# repeat same for memory results

df_memory = pd.DataFrame(data_memory, index=sample_count, columns=['old version', 'old version std', 'new version', 'new version std'])

fit_old = np.polyfit(x=df_memory.index, y=df_memory.loc[:,'old version'],deg=1)
fit_new = np.polyfit(x=df_memory.index, y=df_memory.loc[:,'new version'],deg=1)
func_old = np.poly1d(fit_old)
func_new = np.poly1d(fit_new)
x_fit = np.linspace(100, 750, 10)
y_old = func_old(x_fit)
y_new = func_new(x_fit)

print('old fit:' ,fit_old)
print('new fit:' , fit_new )

fig, ax = plt.subplots(figsize=(8,5))
ax.errorbar(df_memory.index, df_memory.loc[:,'old version'], yerr=df_memory.loc[:,'old version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'original')
ax.errorbar(df_memory.index, df_memory.loc[:,'new version'], yerr=df_memory.loc[:,'new version std'], fmt='o',ecolor='darkgray', elinewidth=1, capsize=3, label = 'improved')
ax.plot(x_fit, y_old, label = str('y = ' + str(round(fit_old[0],2)) + "x + "+ str(round(fit_old[1],2))))
ax.plot(x_fit, y_new, label = str('y = ' + str(round(fit_new[0],2)) + "x + " + str(round(fit_new[1],2))))
ax.legend()
ax.set_xlabel('Sample count')
ax.set_ylabel('Memory Usage in MB')
fig.savefig('Analysis/Runtime_Analysis/memory_sampleCount.png', bbox_inches='tight')
