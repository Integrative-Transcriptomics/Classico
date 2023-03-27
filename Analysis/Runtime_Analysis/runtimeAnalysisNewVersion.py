import subprocess
import re
import pandas as pd
import matplotlib.pyplot as plt
from textwrap import wrap

# ==================================================================
# THIS SCRIPT ONLY RUNS IF THE RUNTIME IS 
# PRINTED IN THE NEW CLASSICO VERSION!
# ==================================================================

# =============================================================
# this script plots the runtime of the steps of the new CLASSICO
# version for both test datasets
# =============================================================

# number of repetitions the runtime and memory analysis should be performed
repetitions = 10

# array to save the results of treponema pallidum in
runtime_treponema = []

# repeat analysis
for i in range(repetitions):
    # call new CLASSICO version with resolution flag specified
    p = subprocess.Popen('java -jar src/classicoV2.jar  --snptable Data/Treponema_snvTable_paperEvidente.tsv --nwk Data/Treponema_MPTree_paperEvidente.NWK --out Data/Output_Runtime_Memory_Analysis --method only-parent --relmaxdepth 0.2 --resolve', stdout=subprocess.PIPE)
    # parse output
    output, err = p.communicate()
    runtime_init = int(re.search('\d+', re.search('Initialization time \d+', output.decode()).group()).group())
    runtime_first_clade_ident = int(re.search('\d+', re.search('Clade identification time \d+', output.decode()).group()).group())
    runtime_first_output = int(re.search('\d+', re.search('Write Output time \d+', output.decode()).group()).group())
    runtime_resolution = int(re.search('\d+', re.search('Resolution Time: \d+', output.decode()).group()).group())
    runtime_second_clade_ident = int(re.search('\d+', re.search('Second: Clade identification time \d+', output.decode()).group()).group())
    runtime_second_output = int(re.search('\d+', re.search('Second: Write Output Time \d+', output.decode()).group()).group())
    runtime_total = int(re.search('\d+', re.search('Run time: \d+', output.decode()).group()).group())
    # save output
    row = [runtime_init, runtime_first_clade_ident, runtime_first_output, runtime_resolution, runtime_second_clade_ident, runtime_second_output, runtime_total]
    runtime_treponema.append(row)

# repeat for mycobacterium leprae

runtime_lepra = []
for i in range(repetitions):
    p = subprocess.Popen('java -jar src/classicoV2.jar  --snptable Data/Mycobacterium_leprae_SNP_schuenemann.tsv --nwk Data/Mycobacterium_leprae_schuenemann.nwk --out Data/Output_Runtime_Memory_Analysis --method only-parent --relmaxdepth 0.2 --resolve', stdout=subprocess.PIPE)
    output, err = p.communicate()
    runtime_init = int(re.search('\d+', re.search('Initialization time \d+', output.decode()).group()).group())
    runtime_first_clade_ident = int(re.search('\d+', re.search('Clade identification time \d+', output.decode()).group()).group())
    runtime_first_output = int(re.search('\d+', re.search('Write Output time \d+', output.decode()).group()).group())
    runtime_resolution = int(re.search('\d+', re.search('Resolution Time: \d+', output.decode()).group()).group())
    runtime_second_clade_ident = int(re.search('\d+', re.search('Second: Clade identification time \d+', output.decode()).group()).group())
    runtime_second_output = int(re.search('\d+', re.search('Second: Write Output Time \d+', output.decode()).group()).group())
    runtime_total = int(re.search('\d+', re.search('Run time: \d+', output.decode()).group()).group())
    row = [runtime_init, runtime_first_clade_ident, runtime_first_output, runtime_resolution, runtime_second_clade_ident, runtime_second_output, runtime_total]
    runtime_lepra.append(row)

# convert to ms and compute mean and standard deviation
df_treponema = pd.DataFrame(runtime_treponema, columns=['Initialization', 'Group Identification\nBefore Resol.', 'Output\nBefore Resol.',  'Resolution\nUnresolved Bases', 'Group Identification\nAfter Resol.', 'Output\nAfter Resol.', 'Total'])
df_treponema = df_treponema / 1000000
df_treponema_std = df_treponema.std(axis = 0)
df_treponema = df_treponema.mean(axis=0)
print(df_treponema)
print(df_treponema_std)

df_lepra = pd.DataFrame(runtime_lepra, columns=['Initialization', 'Group Identification\nBefore Resol.', 'Output\nBefore Resol.',  'Resolution\nUnresolved Bases', 'Group Identification\nAfter Resol.', 'Output\nAfter Resol.', 'Total'])
df_lepra = df_lepra / 1000000
df_lepra_std = df_lepra.std(axis = 0)
df_lepra = df_lepra.mean(axis=0)

print(df_lepra)
print(df_lepra_std)

df_merge = pd.concat([df_treponema, df_lepra], axis=1)
df_merge.columns = ['Treponema', 'Lepra']
print(df_merge)

# plot results

fig, ax = plt.subplots(figsize=(11,4))
ax1 = ax.bar(df_merge.index, df_merge.loc[:,'Treponema'], yerr = df_treponema_std,  label='$\it{Treponema}~\it{pallidum}$', align='edge', capsize=3, width= -0.4)
ax2 = ax.bar(df_merge.index, df_merge.loc[:,'Lepra'],yerr = df_lepra_std, label='$\it{Mycobacterium}~\it{leprae}$', align='edge',capsize=3, width = 0.4)

ax.legend()
ax.set_ylabel("Time in ms")
labels = df_merge.index
labels = ['\n'.join(wrap(x, 15)) for x in  labels]
ax.set_xticklabels(labels)

fig.savefig("Analysis/RuntimeAnalysisSteps.png", bbox_inches='tight')