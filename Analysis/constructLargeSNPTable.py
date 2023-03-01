import sys

#=============================================================
# script for constructing a large SNP table for the runtime 
# comparison between the old and new CLASSICO version
#=============================================================

# read inputs
# 1. original SNP table path
# 2. path for newly generated SNP table
# 3. number of rows in the new SNP table

input = sys.argv
old = open(input[1], 'r')
new = open(input[2], 'w')
new_count = int(input[3])

# write header row to new file
line = old.readline()
new.write(line)
line = old.readline()

# generate new file with specified number of rows
count = 1
while count <= new_count:
    splits = line.split('\t')
    splits[0] = str(count)
    new_line = '\t'.join(splits)
    new.write(new_line)
    line = old.readline()
    # if the new file should contain more rows than the old file, start from the beginning again
    if line == '':
        old = open(input[1], 'r')
        line = old.readline()
        line = old.readline()
    count += 1