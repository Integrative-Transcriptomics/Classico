# ==========================================================
# This is the main script that evaluates the resolution of unresolved bases.
# It brings the subscripts and CLASSICO together and produces the output
# figures (and tables if specified). 
# ==========================================================

import subprocess
import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import os
import math

# tested methods, relative maximum depths, number of clades and clade sizes
methods = ['cladewise', 'parent-sibling', 'only-parent']
plot_methods = ['cladewise', 'only-parent']
maxDepths = np.arange(0,1.05,0.05)
numClades = np.arange(0.02,0.20,0.02)
cladeSize = np.arange(0.02,0.20,0.01)

# different repetitions (numbers are based on Treponema pallidum for Mycobacterium leprae they are 7, 14 and 21)
repetitions_per_position = [10, 20, 30]

# ---------------------------------
# Three different analysis have been performed
# ---------------------------------
# no. 1: one unresolved base per row
single_n = True
# no. 2: multiple unresolved bases per row (there are no sibling nodes that are unresolved)
multi_single_n = True
# no. 3: one unresolved clade (multiple unresolved bases in neighborhood to each other)
single_multi_n = True


# ------------------------------------------
# Analysis no. 1: one unresolved clade of size one (exactly one base)
# maximum depth and repetitions variable (and three resolution methods)
# ------------------------------------------
if single_n:
    for repetition in repetitions_per_position:
        
        # only one unresolved base per row
        num = 1
        
        # initialize dataframes        
        df_treponema_correct = pd.DataFrame(0.0, index=maxDepths, columns=methods)
        df_treponema_incorrect = pd.DataFrame(0.0, index=maxDepths, columns=methods)
        df_treponema_unresolved = pd.DataFrame(0.0, index=maxDepths, columns=methods)

        # define file paths
        print('\nTreponema\n')
        original_file = 'Data/Treponema_snvTable_paperEvidente.tsv'
        generated_file = 'Analysis/Resolution_Evaluation/Treponema_unresolved.tsv'
        truth_file = 'Analysis/Resolution_Evaluation/Treponema.tsv'
        resolved_file = 'Data/Output_Treponema/Treponema_unresolved_resolved.tsv'
        newick_file = 'Data/Treponema_MPTree_paperEvidente.NWK'
        
        # construct file with unresolved bases and ground truth file
        subprocess.call("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " " + str(num) +  " " + newick_file + " 1")
        
        # iterate over all methods and maximum depths
        for method in methods:
            for maxDepth in maxDepths:
                # run CLASSICO version 2
                subprocess.call('java -jar src/classicoV2.jar --snptable ' + generated_file + ' --nwk Data/Treponema_MPTree_paperEvidente.NWK --out Data/Output_Treponema --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
                # compare results of CLASSICO version 2 with ground truth
                result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
                # extract counts from output
                correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
                incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
                unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
                # save results to dataframe
                df_treponema_correct.loc[maxDepth, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
                df_treponema_incorrect.loc[maxDepth, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
                df_treponema_unresolved.loc[maxDepth, method] = float(unresolved/(correct_res + incorrect_res + unresolved))

        print(df_treponema_correct)
        print(df_treponema_incorrect)
        print(df_treponema_unresolved)     
        
        # plot results
        plt.rcParams.update({'font.size': 15})
        fig, (ax1, ax2) = plt.subplots(2,1, sharex = True)
        fig.subplots_adjust(hspace=0.2)
        
        for method in plot_methods:
            if method == 'only-parent':
                curr_label = 'only-p./parent-s.'
            else:
                curr_label = method
            line, = ax1.plot(df_treponema_correct.index, df_treponema_correct.loc[:,method], linestyle = '-', label = curr_label + " correct")
            ax1.plot(df_treponema_incorrect.index, df_treponema_incorrect.loc[:,method], linestyle = '--', color = line.get_color(), label = curr_label + " incorrect")
            ax1.plot(df_treponema_unresolved.index, df_treponema_unresolved.loc[:,method], linestyle = '-.', color = line.get_color(), label = curr_label + " unresolved")
            line, = ax2.plot(df_treponema_correct.index, df_treponema_correct.loc[:,method], linestyle = '-', label = curr_label + " correct")
            ax2.plot(df_treponema_incorrect.index, df_treponema_incorrect.loc[:,method], linestyle = '--', color = line.get_color(), label = curr_label + " incorrect")
            ax2.plot(df_treponema_unresolved.index, df_treponema_unresolved.loc[:,method], linestyle = '-.', color = line.get_color(), label = curr_label + " unresolved")
            
        ax1.set_ylim(.78, 1.)  # outliers only
        ax2.set_ylim(0, .22)
        
        ax1.spines.bottom.set_visible(False)
        ax2.spines.top.set_visible(False)
        ax1.xaxis.tick_top()
        ax1.tick_params(labeltop=False)  # don't put tick labels at the top
        ax2.xaxis.tick_bottom()
        d = .5  
        kwargs = dict(marker=[(-1, -d), (1, d)], markersize=12,
                    linestyle="none", color='k', mec='k', mew=1, clip_on=False)
        ax1.plot([0, 1], [0, 0], transform=ax1.transAxes, **kwargs)
        ax2.plot([0, 1], [1, 1], transform=ax2.transAxes, **kwargs)
        
        ax1.legend(loc='upper right', bbox_to_anchor=(1,0.5))
        ax2.set_xlabel('Maximum relative depth')
        ax1.set_ylabel('Frequency')
        ax1.yaxis.set_label_coords(-0.1,-0.1)
        ax1.set_zorder(ax2.get_zorder()+1)
        fig.savefig('Analysis/Resolution_Evaluation/TreponemaResolution1_' + str(repetition) + '.png',bbox_inches='tight')
        #df_treponema_correct.to_csv('Analysis/Resolution_Evaluation/overview_treponema_correct_single_single_' + str(repetition) + '.csv')
        #df_treponema_incorrect.to_csv('Analysis/Resolution_Evaluation/overview_treponema_incorrect_single_single_' + str(repetition) + '.csv')
        #df_treponema_unresolved.to_csv('Analysis/Resolution_Evaluation/overview_treponema_unresolved_single_single_' + str(repetition) + '.csv')


        # repeat the same for lepra dataset
        
        print('\n\nLepra \n')

        df_lepra_correct = pd.DataFrame(0.0, index=maxDepths, columns=methods)
        df_lepra_incorrect = pd.DataFrame(0.0, index=maxDepths, columns=methods)
        df_lepra_unresolved = pd.DataFrame(0.0, index=maxDepths, columns=methods)

        print('\nTreponema\n')
        original_file = 'Data/Mycobacterium_leprae_SNP_schuenemann.tsv'
        generated_file = 'Analysis/Resolution_Evaluation/Lepra_unresolved.tsv'
        truth_file = 'Analysis/Resolution_Evaluation/Lepra.tsv'
        resolved_file = 'Data/Output_Mycobacterium_Leprae/Lepra_unresolved_resolved.tsv'
        newick_file = 'Data/Mycobacterium_leprae_schuenemann.nwk'
        
        if int(repetition) == 10:
            repetition = 7
        elif int(repetition) == 20:
            repetition = 14
        else:
            repetition = 21
        
        subprocess.call("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " " + str(num) +  " " + newick_file + " 1")
        
        for method in methods:
            print(method)
            for maxDepth in maxDepths:
                subprocess.call('java -jar src/classicoV2.jar  --snptable ' + generated_file + ' --nwk Data/Mycobacterium_leprae_schuenemann.nwk --out Data/Output_Mycobacterium_Leprae --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
                result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
                print(result)
                correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
                incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
                unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
                df_lepra_correct.loc[maxDepth, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
                df_lepra_incorrect.loc[maxDepth, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
                df_lepra_unresolved.loc[maxDepth, method] = float(unresolved/(correct_res + incorrect_res + unresolved))

        #print(df_lepra_correct)
        #print(df_lepra_incorrect)
        #print(df_lepra_unresolved)
        #df_lepra_correct.to_csv('Analysis/Resolution_Evaluation/overview_lepra_correct_single_single_' + str(repetition) + '.csv')
        #df_lepra_incorrect.to_csv('Analysis/Resolution_Evaluation/overview_lepra_incorrect_single_single_' + str(repetition) + '.csv')
        #df_lepra_unresolved.to_csv('Analysis/Resolution_Evaluation/overview_lepra_unresolved_single_single_' + str(repetition) + '.csv')
        plt.rcParams.update({'font.size': 15})
        fig, (ax1, ax2) = plt.subplots(2,1, sharex = True)
        fig.subplots_adjust(hspace=0.2)
        
        for method in plot_methods:
            if method == 'only-parent':
                curr_label = 'only-p./parent-s.'
            else:
                curr_label = method
            line, = ax1.plot(df_lepra_correct.index, df_lepra_correct.loc[:,method], linestyle = '-', label = curr_label + " correct")
            ax1.plot(df_lepra_incorrect.index, df_lepra_incorrect.loc[:,method], linestyle = '--', color = line.get_color(), label = curr_label + " incorrect")
            ax1.plot(df_lepra_unresolved.index, df_lepra_unresolved.loc[:,method], linestyle = '-.', color = line.get_color(), label = curr_label + " unresolved")
            line, = ax2.plot(df_lepra_correct.index, df_lepra_correct.loc[:,method], linestyle = '-', label = curr_label + " correct")
            ax2.plot(df_lepra_incorrect.index, df_lepra_incorrect.loc[:,method], linestyle = '--', color = line.get_color(), label = curr_label + " incorrect")
            ax2.plot(df_lepra_unresolved.index, df_lepra_unresolved.loc[:,method], linestyle = '-.', color = line.get_color(), label = curr_label + " unresolved")

        ax1.set_ylim(.78, 1.)  # outliers only
        ax2.set_ylim(0, .22)
        
        ax1.spines.bottom.set_visible(False)
        ax2.spines.top.set_visible(False)
        ax1.xaxis.tick_top()
        ax1.tick_params(labeltop=False)  # don't put tick labels at the top
        ax2.xaxis.tick_bottom()
        d = .5  
        kwargs = dict(marker=[(-1, -d), (1, d)], markersize=12,
                    linestyle="none", color='k', mec='k', mew=1, clip_on=False)
        ax1.plot([0, 1], [0, 0], transform=ax1.transAxes, **kwargs)
        ax2.plot([0, 1], [1, 1], transform=ax2.transAxes, **kwargs)
        ax1.set_zorder(ax2.get_zorder()+1)
        ax1.legend(loc='upper right', bbox_to_anchor=(1,0.5))
        ax2.set_xlabel('Maximum relative depth')
        ax1.set_ylabel('Frequency')
        ax1.yaxis.set_label_coords(-0.1,-0.1)
        fig.savefig('Analysis/Resolution_Evaluation/LepraResolution1_' + str(repetition) + '.png',bbox_inches='tight')
        

# ------------------------------------------
# Multi single case: multiple unresolved clades of size one (num times one base)
# num clades variable
# ------------------------------------------

if multi_single_n:

    # based on first analysis repetition of 10 for Treponema pallidum is chosen and relative maximum depth of 0.2
    repetition = 10
    maxDepth = 0.2
                
    # initialize dataframes
    df_treponema_correct = pd.DataFrame(0.0, index=numClades, columns=methods)
    df_treponema_incorrect = pd.DataFrame(0.0, index=numClades, columns=methods)
    df_treponema_unresolved = pd.DataFrame(0.0, index=numClades, columns=methods)
    
    # array in which sizes of clades are stored
    treponema_list_clade_size_count = []
    
    # define file paths
    print('\nTreponema\n')
    original_file = 'Data/Treponema_snvTable_paperEvidente.tsv'
    generated_file = 'Analysis/Resolution_Evaluation/Treponema_unresolved.tsv'
    truth_file = 'Analysis/Resolution_Evaluation/Treponema.tsv'
    resolved_file = 'Data/Output_Treponema/Treponema_unresolved_resolved.tsv'
    newick_file = 'Data/Treponema_MPTree_paperEvidente.NWK'
    
    # iterate over different absolute clade sizes (in procent)
    for percentage in numClades:
        
        # calculate the absolute clade size
        num = math.floor(percentage * 75)
        
        # generate unresolved file and ground truth file
        subprocess.call("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " " + str(num) + " " + newick_file + " 1")
        
        # append absolute clade size
        treponema_list_clade_size_count.append(str(round(percentage,2)) + "\n("+ str(num) + ")")
        
        # iterate over all methods
        for method in methods:
            
            # run CLASSICO version 2
            subprocess.call('java -jar src/classicoV2.jar  --snptable ' + generated_file + ' --nwk ' + newick_file + ' --out Data/Output_Treponema --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
            
            # run evaluation script
            result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
            
            # extract counts from output
            correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
            incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
            unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
            
            # save results to dataframe
            df_treponema_correct.loc[percentage, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
            df_treponema_incorrect.loc[percentage, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
            df_treponema_unresolved.loc[percentage, method] = float(unresolved/(correct_res + incorrect_res + unresolved))


    #print(df_treponema_correct)
    #print(df_treponema_incorrect)
    #print(df_treponema_unresolved)
        
    # save result as csv
    #df_treponema_correct.to_csv('Analysis/overview_treponema_correct_multi_single.csv')
    #df_treponema_incorrect.to_csv('Analysis/overview_treponema_incorrect_multi_single.csv')
    #df_treponema_unresolved.to_csv('Analysis/overview_treponema_unresolved_multi_single.csv')

    # repeat for lepra
    print('\n\nLepra \n')

    # with repetition = 7
    repetition = 7

    df_lepra_correct = pd.DataFrame(0.0, index=numClades, columns=methods)
    df_lepra_incorrect = pd.DataFrame(0.0, index=numClades, columns=methods)
    df_lepra_unresolved = pd.DataFrame(0.0, index=numClades, columns=methods)

    lepra_list_clade_size_count = []

    original_file = 'Data/Mycobacterium_leprae_SNP_schuenemann.tsv'
    generated_file = 'Analysis/Resolution_Evaluation/Lepra_unresolved.tsv'
    truth_file = 'Analysis/Resolution_Evaluation/Lepra.tsv'
    resolved_file = 'Data/Output_Mycobacterium_Leprae/Lepra_unresolved_resolved.tsv'
    newick_file = 'Data/Mycobacterium_leprae_schuenemann.nwk'
    for percentage in numClades:
        num = math.floor(percentage * 169)
        subprocess.call("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " " + str(num) + " " + newick_file + " 1")
        lepra_list_clade_size_count.append(str(round(percentage,2)) + "\n("+ str(num) + ")")
        for method in methods:
            print(method, percentage)
            subprocess.call('java -jar src/classicoV2.jar  --snptable ' + generated_file + ' --nwk '  + newick_file + ' --out Data/Output_Mycobacterium_Leprae --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
            result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
            print(result)
            correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
            incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
            unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
            df_lepra_correct.loc[percentage, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
            df_lepra_incorrect.loc[percentage, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
            df_lepra_unresolved.loc[percentage, method] = float(unresolved/(correct_res + incorrect_res + unresolved))

    #print(df_lepra_correct)
    #print(df_lepra_incorrect)
    #print(df_lepra_unresolved)
    #df_lepra_correct.to_csv('Analysis/overview_lepra_correct_multi_single.csv')
    #df_lepra_incorrect.to_csv('Analysis/overview_lepra_incorrect_multi_single.csv')
    #df_lepra_unresolved.to_csv('Analysis/overview_lepra_unresolved_multi_single.csv')
    
    # plot results
        
    min_y = min(min(df_lepra_correct.min()), min(df_treponema_correct.min())) - 0.005
    plt.rcParams.update({'font.size': 15})
    plt.figure(figsize=(12,3.5))
    
    for method in plot_methods:
        if method == 'only-parent':
            curr_label = 'only-p./parent-s.'
        else:
            curr_label = method
        line, = plt.plot(df_treponema_correct.index, df_treponema_correct.loc[:,method], linestyle = '-', label = curr_label)
               
    plt.legend()
    plt.xlabel('relative no. of N\'s \n(absolute no. of N\'s)')
    plt.ylabel('Accuracy')
    plt.ylim(min_y, 1)
    plt.xticks(df_treponema_correct.index, labels=treponema_list_clade_size_count)
    plt.savefig('Analysis/Resolution_Evaluation/TreponemaResolution2.png',bbox_inches='tight')
    
    plt.rcParams.update({'font.size': 15})
    plt.figure(figsize=(12,3.5))
    
    for method in plot_methods:
        if method == 'only-parent':
            curr_label = 'only-p./parent-s.'
        else:
            curr_label = method
        line, = plt.plot(df_lepra_correct.index, df_lepra_correct.loc[:,method], linestyle = '-', label = curr_label)
                
    plt.legend()
    plt.xlabel('relative no. of N\'s \n(absolute no. of N\'s)')
    plt.ylabel('Accuracy')
    plt.ylim(min_y, 1)
    plt.xticks(df_lepra_correct.index, labels=lepra_list_clade_size_count)
    plt.savefig('Analysis/Resolution_Evaluation/LepraResolution2.png',bbox_inches='tight')
    plt.figure()
    
# ------------------------------------------
# Single multi case: one unresolved clades of size k (single unresolved clade)
# clade size k variable
# ------------------------------------------

if single_multi_n:

    # based on first analysis repetition of 10 for Treponema pallidum is chosen and relative maximum depth of 0.2
    repetition = 10
    maxDepth = 0.2
    
    # initialize dataframes            
    df_treponema_correct = pd.DataFrame(np.nan, index=cladeSize, columns=methods)
    df_treponema_incorrect = pd.DataFrame(np.nan, index=cladeSize, columns=methods)
    df_treponema_unresolved = pd.DataFrame(np.nan, index=cladeSize, columns=methods)

    # arrays with absolute clade size and number of possible clads of that size in the tree
    treponema_list_clade_size_count = []
    treponema_clade_count = []
    
    # define file paths
    print('\nTreponema\n')
    original_file = 'Data/Treponema_snvTable_paperEvidente.tsv'
    generated_file = 'Analysis/Resolution_Evaluation/Treponema_unresolved.tsv'
    truth_file = 'Analysis/Resolution_Evaluation/Treponema.tsv'
    resolved_file = 'Data/Output_Treponema/Treponema_unresolved_resolved.tsv'
    newick_file = 'Data/Treponema_MPTree_paperEvidente.NWK'
    
    # iterate over all clade sizes
    for percentage in cladeSize:
        
        # compute absolute clade size
        size = math.floor(percentage * 75)
        
        # only analyze if clade size is greater 1
        if size <= 1:
            continue
        
        # generate unresolved file and ground truth file
        p = subprocess.Popen("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " 1 " + newick_file + " "  + str(size), stdout=subprocess.PIPE)
        output, err = p.communicate()
        rc = p.returncode
        
        # if there are no clades of this size in the tree, then continue with next clade size
        if re.search('No clade of this size in the tree', output.decode()) != None:
            continue
        # if there are clades of this size in the tree, save the absolute clade size and number of possible clades
        else:
            treponema_list_clade_size_count.append(str(round(percentage,2)) + "\n("+ str(size) + ")")
            treponema_clade_count.append(int(re.search('\d+', re.search('possible clades: \d+', output.decode()).group()).group()))
        
        # iterate over all methods
        for method in methods:
            
            # run CLASSICO version 2
            subprocess.call('java -jar src/classicoV2.jar  --snptable ' + generated_file + ' --nwk ' + newick_file + ' --out Data/Output_Treponema --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
            
            # run evaluation script
            result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
            
            # extract counts
            correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
            incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
            unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
            
            # save results to dataframe
            df_treponema_correct.loc[percentage, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
            df_treponema_incorrect.loc[percentage, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
            df_treponema_unresolved.loc[percentage, method] = float(unresolved/(correct_res + incorrect_res + unresolved))

    # drop empty rows
    df_treponema_correct = df_treponema_correct.dropna()
    df_treponema_incorrect = df_treponema_incorrect.dropna()
    df_treponema_unresolved = df_treponema_unresolved.dropna()

    #print(df_treponema_correct)
    #print(df_treponema_incorrect)
    #print(df_treponema_unresolved)
        
    #df_treponema_correct.to_csv('Analysis/Resolution_Evaluation/overview_treponema_correct_single_multi.csv')
    #df_treponema_incorrect.to_csv('Analysis/Resolution_Evaluation/overview_treponema_incorrect_single_multi.csv')
    #df_treponema_unresolved.to_csv('Analysis/Resolution_Evaluation/overview_treponema_unresolved_single_multi.csv')
    
    # plot results
    plt.rcParams.update({'font.size': 15})
    fig, ax = plt.subplots(figsize=(12,3.5))
    ax2 = ax.twinx()
    ax.set_zorder(ax2.get_zorder()+1)
    ax.patch.set_visible(False)
    ax2.bar(df_treponema_correct.index, treponema_clade_count, width = 0.003, align='center', color = 'gray', alpha = 0.7)  
    ax2.set_ylabel('total no. of possible clades')
    for method in plot_methods:
        if method == 'only-parent':
            curr_label = 'only-p./parent-s.'
        else:
            curr_label = method
        line, = ax.plot(df_treponema_correct.index, df_treponema_correct.loc[:,method], linestyle = '-', label = curr_label)
        
    
    # repeat for lepra

    print('\n\nLepra \n')


    if int(repetition) == 10:
        repetition = 7
    elif int(repetition) == 20:
        repetition = 14
    else:
        repetition = 21

    df_lepra_correct = pd.DataFrame(np.nan, index=cladeSize, columns=methods)
    df_lepra_incorrect = pd.DataFrame(np.nan, index=cladeSize, columns=methods)
    df_lepra_unresolved = pd.DataFrame(np.nan, index=cladeSize, columns=methods)

    lepra_list_clade_size_count = []
    lepra_clade_count = []

    original_file = 'Data/Mycobacterium_leprae_SNP_schuenemann.tsv'
    generated_file = 'Analysis/Resolution_Evaluation/Lepra_unresolved.tsv'
    truth_file = 'Analysis/Resolution_Evaluation/Lepra.tsv'
    resolved_file = 'Data/Output_Mycobacterium_Leprae/Lepra_unresolved_resolved.tsv'
    newick_file = 'Data/Mycobacterium_leprae_schuenemann.nwk'
    for percentage in cladeSize:
        size = math.floor(percentage * 169)
        if size <= 1:
            continue
        p = subprocess.Popen("python Analysis/Resolution_Evaluation/generateUnresolvedFiles.py " + original_file + " " + generated_file  + " "  + truth_file + " " + str(repetition) + " 1 " + newick_file + " "  + str(size), stdout=subprocess.PIPE)
        output, err = p.communicate()
        rc = p.returncode
        if re.search('No clade of this size in the tree', output.decode()) != None:
            continue
        else:
            lepra_list_clade_size_count.append(str(round(percentage,2)) + "\n(" + str(size) + ")")
            lepra_clade_count.append(int(re.search('\d+', re.search('possible clades: \d+', output.decode()).group()).group()))
        for method in methods:
            print(method)
            subprocess.call('java -jar src/classicoV2.jar  --snptable ' + generated_file + ' --nwk '  + newick_file + ' --out Data/Output_Mycobacterium_Leprae --resolve --method ' + method + ' --relmaxdepth ' + str(maxDepth))
            result = subprocess.run('python Analysis/Resolution_Evaluation/evaluateResolution.py ' + truth_file + " " + resolved_file + " " + generated_file, capture_output=True)
            print(result)
            correct_res = int(re.search('\d+', re.search('Correct Resolution: \d+', result.stdout.decode()).group()).group())
            incorrect_res = int(re.search('\d+',re.search('Incorrect Resolution: \d+', result.stdout.decode()).group()).group())
            unresolved = int(re.search('\d+',re.search('Unresolved Bases: \d+', result.stdout.decode()).group()).group())
            df_lepra_correct.loc[percentage, method] = float(correct_res/ (correct_res + incorrect_res + unresolved))
            df_lepra_incorrect.loc[percentage, method] = float(incorrect_res/(correct_res + incorrect_res + unresolved))
            df_lepra_unresolved.loc[percentage, method] = float(unresolved/(correct_res + incorrect_res + unresolved))

    
    df_lepra_correct = df_lepra_correct.dropna()
    df_lepra_incorrect = df_lepra_incorrect.dropna()
    df_lepra_unresolved = df_lepra_unresolved.dropna()

    #print(df_lepra_correct)
    #print(df_lepra_incorrect)
    #print(df_lepra_unresolved)
    
    
    #df_lepra_correct.to_csv('Analysis/Resolution_Evaluation/overview_lepra_correct_single_multi.csv')
    #df_lepra_incorrect.to_csv('Analysis/Resolution_Evaluation/overview_lepra_incorrect_single_multi.csv')
    #df_lepra_unresolved.to_csv('Analysis/Resolution_Evaluation/overview_lepra_unresolved_single_multi.csv')
    
    
    min_y = min(min(df_lepra_correct.min()), min(df_treponema_correct.min())) - 0.05
    max_y = max(max(lepra_clade_count), max(treponema_clade_count)) + 1
    min_x = min(min(df_treponema_correct.index), min(df_lepra_correct.index)) - 0.01
    max_x = max(max(df_treponema_correct.index), max(df_lepra_correct.index)) + 0.01
    ax.legend()
    ax.set_xlabel('relative clade size of N\'s \n(absolute clade size of N\'s)')
    ax.set_xlim(min_x, max_x)
    ax.set_ylabel('Accuracy')
    ax.set_ylim(min_y, 1)
    ax2.set_ylim(0,max_y)
    plt.xticks(df_treponema_correct.index, labels=treponema_list_clade_size_count)
    plt.savefig('Analysis/Resolution_Evaluation/TreponemaResolution3.png',bbox_inches='tight')
    
    plt.rcParams.update({'font.size': 15})
    fig, ax = plt.subplots(figsize=(12,3.5))
    #plt.figure(3,2)
    ax2 = ax.twinx()
    ax.set_zorder(ax2.get_zorder()+1)
    ax.patch.set_visible(False)
    ax2.bar(df_lepra_correct.index, lepra_clade_count, width = 0.003, align = 'center', color='gray', alpha = 0.7)    
    ax2.set_ylabel('total no. of possible clades')
    ax2.set_ylim(0,max_y)
    for method in plot_methods:
        if method == 'only-parent':
            curr_label = 'only-p./parent-s.'
        else:
            curr_label = method
        line, = ax.plot(df_lepra_correct.index, df_lepra_correct.loc[:,method], linestyle = '-', label = curr_label)
        
    ax.legend()
    ax.set_xlabel('relative clade size of N\'s \n(absolute clade size of N\'s)')
    ax.set_xlim(min_x, max_x)
    ax.set_ylabel('Accuracy')
    ax.set_ylim(min_y, 1)
    plt.xticks(df_lepra_correct.index, labels=lepra_list_clade_size_count)
    plt.savefig('Analysis/Resolution_Evaluation/LepraResolution3.png',bbox_inches='tight')
    plt.figure()


