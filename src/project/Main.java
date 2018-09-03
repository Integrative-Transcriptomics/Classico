package project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Katrin Fischer
 *
 */
public class Main {
	public static void main(String[] args0) throws IOException{
		if(args0.length == 3) {
			Project comcla = new Project(args0[0], args0[1], args0[2]);
			comcla.compute();
			comcla.getResults();
			//SNP-Positionen deren Baum ausgegeben werden soll mit allen Nukleotiden der einen Position
			List<Integer> l = new ArrayList<Integer>();
			l.add(-1);
			l.add(3125);
			l.add(15282);
			comcla.showPositions(l);
			// Einzigartige SNPs f�r einen Teilbaum
			comcla.getUniqueSubtreeSNPs(comcla.getFilepath().createFile("UniqueSubTree.txt"), 13);
			// Zeige Baum mit Kladenallelen
			List<Integer> j = new ArrayList<Integer>();
            l.add(-1);
            comcla.showPositions(j);
//			for(int i : comcla.snp.getSNPs()){
//				comcla.treeSNPs(i);
//			}
			// Einzigartige SNPs f�r mehrere verschiedene Individuen
			List<Integer> k = new ArrayList<Integer>();
			k.add(1);
			k.add(6);
			comcla.commonSNPs(k);
		}else {
			System.err.println("Falsche Eingabeparameter: [SNP-Tabelle] [Newick-Datei] [Ergebnispfad]");
		}
	}
}
