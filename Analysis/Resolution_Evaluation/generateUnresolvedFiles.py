import sys
import random
import copy
import re
import itertools

# ===========================================================
# This script generates a SNP table containing unresolved bases and a corresponding
# ground truth SNP table from an input SNP table for the resolution evaluation
# ===========================================================

# Function that maps a line from the SNP table to a strucuture:
# In comparison to the line from a SNP table, the structure 
# contains numbers instead of letters where each number corresponds
# to a letter. The first letter that occurs in a line gets the number
# 1, the second gets number 2, ...
# Example: 
# The line A T A G G A would get the structure 1 2 1 3 3 1.

def line_to_structure(line):
    mapping = {}
    splits = line.split('\t')
    count = 0
    structure = ""
    for split in splits[1:]:
        if split not in mapping:
            mapping[split] = count
            count += 1
        structure += str(mapping[split]) + "\t"
    return structure

# read input arguments
# 1. path of original SNP table
# 2. path of generated unresolved SNP table
# 3. path of generated ground truth SNP table
# 4. number of repetitions for each position
# 5. number of unresolved clades per row 
# 6. path of corresponding Newick tree file
# 7. size of unresolved clades

input = sys.argv
# read SNP table
file = open(input[1], 'r')

# initialize arrays that save the structures that are already seen / used in the SNP table 
seen_structures = []
# and the row index of the first occurance of each structure
not_seen_ind = []

# get sample names from SNP table
line = file.readline()
line = line[:len(line) - 1]
names = line.split('\t')

# write sample names to new unresolved file and ground truth file
new_unresolved = open(input[2], 'w')
new_unresolved.write(line + "\n")
new_truth = open(input[3], 'w')
new_truth.write(line + "\n")

# read parameters that influence the output files
num_repetitions = int(input[4])
number_ns = int(input[5])
clade_size = int(input[7])

# read newick string
newick = str(open(input[6], 'r').readlines())

# for third analysis
# compute clades with the specified clade_size
# method: find small clades of size 2 and try to extend them to a specific clade size
if clade_size > 1:
    
    # regex to get any two leaf nodes that are siblings (i.e. clades of size 2)
    my_regex =  "\([^():,]+:[.\d]+,[^():,]+:[.\d]+\)"
    reg = re.compile(my_regex)
    matches = reg.finditer(newick)
    clades = []
    
    # try to extend these clades further
    for m in matches:
        left_bracket_count = 1
        right_bracket_count = 1
        next_left = m.start()
        next_right = m.end()
        
        # as long as the clade size is not reached, the following steps are repeated
        while(left_bracket_count < clade_size - 1):
            
            # extend to left and right
            next_left -= 1
            next_right += 1
            
            # find position of opening bracket that belongs to parent of current clades root
            while(not(newick[next_left] == '(' and left_bracket_count == right_bracket_count)):                                
                if newick[next_left] == ')':
                    right_bracket_count += 1
                elif newick[next_left] == '(':
                    left_bracket_count += 1
                next_left -= 1
            left_bracket_count += 1
            
            # find position of closing bracket that belongs to parent of current clades root
            while(not(newick[next_right] == ')' and right_bracket_count + 1 == left_bracket_count)):                                
                if newick[next_right] == ')':
                    right_bracket_count += 1
                elif newick[next_right] == '(':
                    left_bracket_count += 1
                next_right += 1
            right_bracket_count += 1
            
        # check that the size of the clade is not exceeded and that the clade is not already detected
        if left_bracket_count == clade_size -1:
            if newick[next_left:next_right + 1] not in clades:
                clades.append(newick[next_left:next_right + 1])


# read all lines of original SNP table and generate unresolved SNP table and ground truth
line_counter = 1
for line in file.readlines():
    # copy old line for ground truth file
    oldline = copy.copy(line)
    
    # check if there are any N's in the line
    line = line[:len(line) - 1]
    count = 0
    splits = line.split('\t')
    for split in splits[2:]:
        if split == 'N':
            count += 1
    
    # only lines without any N's are used
    if count == 0:
        
        # only consider SNP structure which are not already used
        curr_structure = line_to_structure(line)
        if curr_structure not in seen_structures:
            seen_structures.append(curr_structure)
            not_seen_ind.append(line_counter)
            count_N = 0
            list_randint = []
            
            # 1st case: single bases should be replaced with N's 
            if clade_size == 1:
                
                # construct multiple unresolved lines per position
                while count_N < num_repetitions:
                    
                    # compute random positions of unresolved bases
                    randInts = random.sample(range(2,len(splits)),number_ns)
                    is_same = False
                    
                    # 1st criterion: check if same pattern is already constructed for this position
                    for entry in list_randint:
                        if sorted(randInts) == sorted(entry):
                            is_same = True
                        
                    # 2nd criterion: for multiple unresolved bases per row check if there are any two unresolved bases that are siblings
                    if len(randInts) > 1:
                        for combination in itertools.combinations(randInts, 2):
                            my_regex = rf"\({names[combination[0]]}:[.\d]+,{names[combination[1]]}:[.\d]+\)"
                            if re.search(my_regex, str(newick)) != None:
                                is_same = True
                    
                    # don't consider lines that fulfil any of the two criterions that are checked above
                    if is_same:
                        continue
                    # --------------------------------------------------
                    # otherwise
                    # write the old line to the ground truth file
                    new_truth.write(oldline)
                    
                    # generate a new line with unresolved bases at the above computed positions
                    previouses = {}
                    for randInt in randInts:
                        # save resolved bases in dictionary before replacing them
                        previouses[randInt] = splits[randInt]
                        splits[randInt] = 'N'
                    newline = '\t'.join(splits) + '\n'
                    
                    # write new line to unresolved file
                    new_unresolved.write(newline)
                    list_randint.append(randInts)
                    
                    # reset replaced bases with saved resolved bases before replacement
                    for randInt in randInts:
                        splits[randInt] = previouses[randInt]
                    count_N += 1
            
            # 2nd case: entire clades should be replaced with unresolved bases
            else:
                if len(clades) > 0:
                    
                    # get random clade out of clades with specified size
                    if len(clades) > 1:
                        randInt = random.randrange(0,len(clades) - 1)
                    else:
                        randInt = 0
                    clade = clades[randInt]
                    
                    # find all leaf nodes of that clade
                    my_regex2 = "[^():,]+:[.\d]+"
                    matches2 = re.findall(my_regex2, clade)
                    list_indices = []
                    for match2 in matches2:
                        match = match2.split(':')
                        count = 0
                        for name in names:
                            if match[0] == name:
                                # save index of the sample of the current node
                                list_indices.append(count)
                            count += 1
                    
                    # write the old line to the ground truth file
                    new_truth.write(oldline)
                    
                    # generate a new line with unresolved bases at the above computed positions
                    previouses = {}
                    for ind in list_indices:
                        # save resolved bases in dictionary before replacing them
                        previouses[ind] = splits[ind]
                        splits[ind] = 'N'
                    newline = '\t'.join(splits) + '\n'
                    
                    # write new line to unresolved file
                    new_unresolved.write(newline)
                    
                    # reset replaced bases with saved resolved bases before replacement
                    for ind in list_indices:
                        splits[ind] = previouses[ind]
                else:
                    print('No clade of this size in the tree')

    line_counter += 1

# print output
print('different structures:', len(not_seen_ind))
if clade_size > 1:
    print('possible clades:', len(clades))

    