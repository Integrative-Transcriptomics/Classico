# ==========================================================
# this script evaluates the resolution of unresolved bases by 
# outputting correctly resolved, incorrectly resolved and 
# unresolved bases after resolution
# ==========================================================

# input:
# 1. ground truth SNP table path
# 2. resolved SNP table path
# 3. unresolved SNP table path

import sys
input = sys.argv
truth = open(input[1], 'r')
resolved = open(input[2], 'r')
unresolved = open(input[3], 'r')

# initialize counts
correct_resolved = 0
incorrect_resolved = 0
unresolved_resolved = 0

# read lines for each file
for line_truth in truth.readlines():
    line_resolved = resolved.readline()
    line_unresolved = unresolved.readline()
    line_truth = line_truth[:len(line_truth) - 1]
    line_resolved = line_resolved[:len(line_resolved) - 1]
    line_unresolved = line_unresolved[:len(line_unresolved) - 1]
    
    # split lines to columns
    split_line_truth = line_truth.split('\t')
    split_line_resolved = line_resolved.split('\t')
    split_line_unresolved = line_unresolved.split('\t')
    
    # check that the number of columns is equal for all three files
    if (len(split_line_resolved) != len(split_line_truth) or len(split_line_truth) != len(split_line_unresolved) or len(split_line_resolved) != len(split_line_unresolved)):
        print("Not equal")
        print(len(split_line_resolved))
        print(len(split_line_truth))
        print(len(split_line_unresolved))
        break
    
    # iterate over all SNPs that belong to samples
    for count in range(2, len(split_line_unresolved)):
        # if the unresolved file contains a N this N should be resolved by the algorithm
        if split_line_unresolved[count] == 'N':
            # if the resolved file also contains a N, the base remained unresolved
            if split_line_resolved[count] == 'N':
                unresolved_resolved += 1
            else:
                # if value in the groung truth file is equal to the resolved file the resolution was correct
                if split_line_truth[count] in split_line_resolved[count]:
                    correct_resolved += 1
                # otherwise it was incorrect
                else:
                    incorrect_resolved += 1

# print number of incorrect, correct and unresolved bases
print('Incorrect Resolution: ' + str(incorrect_resolved))
print('Correct Resolution: ' + str(correct_resolved))
print('Unresolved Bases: ' + str(unresolved_resolved))