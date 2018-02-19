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
			List<Integer> l = new ArrayList<Integer>();
			l.add(-1);
			comcla.showPositions(l);
			comcla.getUniqueSubtreeSNPs(comcla.getFile().createFile("UniqueSubTree.txt"), 9);
			comcla.treeSNPs(3125);
			List<Integer> k = new ArrayList<Integer>();
			k.add(2);
			k.add(4);
			k.add(5);
			k.add(6);
			comcla.commonSNPs(k);
		}else {
			System.err.println("Geben Sie als ersten Dateipfad die SNP-Tabelle an und als zweiten eine Newick-Datei und als dritten den Pfad in dem das Ergebnis-Verzeichnis erzeugt werden soll");
		}
	}
}
