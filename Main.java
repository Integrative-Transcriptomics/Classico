package project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;

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
		}else {
			System.err.println("Geben Sie als ersten Dateipfad die SNP-Tabelle an und als zweite eine Newick-Datei und als dritten den Pfad in dem das Ergebnis-Verzeichnis erzeugt werden soll");
		}
	}
}
