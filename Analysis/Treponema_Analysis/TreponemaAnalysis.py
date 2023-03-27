import re
import matplotlib.pyplot as plt
import pandas as pd

# =====================================================================
# this script generates computes the top 20 nodes with the  most polyphyletic 
# roots and plots them
# =====================================================================

poly = open("Data/Output_Treponema/poly.txt", "r")

poly_resolved = open("Data/Output_Treponema/poly_resolved.txt", "r")

snptable = open("Data/Treponema_snvTable_paperEvidente.tsv", "r")

poly_lines = poly.readlines()

poly_resolved_lines = poly_resolved.readlines()

snptable.readline()
snptable_lines = snptable.readlines()

distribution = open("Data/Output_Treponema/IDDistribution.txt", "r")
lines_distribution = distribution.readlines()

poly_node_count = {}

# -----------------------------------------------
# plot top 20 most common polyphyletic clades and 
# distinguish between resolved clades and unresolved 
# clades
# -----------------------------------------------

tep_count = 0
ten_count = 0
nichols_count = 0
ss14_count = 0

# iterate over polyphyletic output

for line in poly_lines:
    splits = line.split('\t')
    # get node ID
    node_id = re.search('\d+', re.search('>\d+', splits[0]).group()).group()
    # get number of polyphyletic clades with this node ID
    count = int(splits[1])
    # get number of unresolved polyphyletic clades
    unresolved = re.findall('[N]', splits[2])
    poly_node_count[node_id] = [int(count) - len(unresolved), len(unresolved)]
    node_id_int = int(node_id)
    
    # increase corresponding subspecies count
    if node_id_int == 133:
        ten_count += count
    elif (node_id_int > 133) and (node_id_int < 147):
        tep_count += count
    elif (node_id_int < 133) and (node_id_int > 113):
        nichols_count += count
    elif (node_id_int < 113):
        ss14_count += count
 
print('Poly TEP:', tep_count)
print('Poly TEN', ten_count)
print('Poly Nic', nichols_count)
print('Poly SS14', ss14_count) 

# sort node counts in descendant order
poly_node_count = dict(sorted(poly_node_count.items(), key=lambda item: (int(item[1][0]) + item[1][1]), reverse=True))

# construct new plots
fig, ax = plt.subplots(figsize=(10,3.5))
x = list(poly_node_count.keys())
y = list(poly_node_count.values())

# plot top 20 nodes with the corrsponsing resolved / unresolved category
df = pd.DataFrame(y)
barlist = ax.bar(x[:20], df.iloc[:20,0], label="Resolved")
barlist = ax.bar(x[:20], df.iloc[:20,1], bottom=df.iloc[:20,0], label="Unresolved")

# print legend, labels
ax.legend()
ax.set_ylabel('no. of polyphyletic clades')
ax.set_xlabel('Node IDs of top 20 nodes with most polyphyletic clades')
ticks_labels = x[:20]

# add corresponding subspecies to label
count = 0
for ticks_label in ticks_labels: 
    if int(ticks_label) == 133:
        ticks_label = ticks_label + "\nTEN"
    elif (int(ticks_label) > 133) and (int(ticks_label) < 147):
        ticks_label = ticks_label + "\nTEP"
    elif (int(ticks_label) < 133) and (int(ticks_label) > 113):
        ticks_label = ticks_label + "\nNic"
    elif (int(ticks_label) < 113):
        ticks_label = ticks_label + "\nSS14"
    ticks_labels[count] = ticks_label
    count += 1
ax.set_xticks(x[:20], ticks_labels)
fig.savefig('Analysis/Treponema_Analysis/TreponemaPolyphylyCount.png', bbox_inches='tight')

