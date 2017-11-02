package Project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;

public class Project {
	private SNPTable snp;
	private NewickTree tree;
	private Map<Integer, List<Node>> claden = new HashMap<Integer, List<Node>>();
	private Map<Integer, List<Integer>> splitKeys = new HashMap<Integer, List<Integer>>();
	private Set<Integer> rootKeys;

	public Project(String snpFile, String newickTreeFile) {
		snp = new SNPTable(snpFile);
		tree = new NewickTree(newickTreeFile);
		this.tree.label(snp);
		rootKeys = tree.getRoot().getLabel().keySet();
	}

	public Project(SNPTable snp, NewickTree tree) {
		this.snp = snp;
		this.tree = tree;
		this.tree.label(snp);
		rootKeys = this.tree.getRoot().getLabel().keySet();
	}

	public static void main(String[] args0) throws IOException {
		if (args0.length == 3) {
			Project comcla = new Project(args0[0], args0[1]);
			Set<Integer> keys = comcla.getRootKeys();
			for (int i : keys) {
				comcla.computeCladen(comcla.tree.getRoot(), i);
			}
			System.out.println(comcla.tree);
			for (Node i : comcla.tree.getNodeList()) {
				System.out.println(i.toString());
			}
			// System.out.println(lepraSNPs);
			comcla.tree.label(comcla.snp);
			FileWriter fw = new FileWriter(args0[2]);
			BufferedWriter bw = new BufferedWriter(fw);
			for (Node i : comcla.tree.getNodeList()) {
				if (i.getLabel().containsKey(73)) {
					System.out.println(i.getId() + " - ");
					bw.write(i.getId());
					System.out.println(i.getLabel().get(73).toString());
					bw.write(i.getLabel().get(73).toString());
				}
			}
			bw.close();
			System.out.println("ready");
		} else {
			System.err.println(
					"Geben Sie als ersten Dateipfad die SNP-Tabelle, als zweite eine Newick-Datei und als dritte eine leere Datei an");
		}
	}

	public void computeCladen(Node node, int key) {

		if (node.getLabel().containsKey(key)) {
			Set<String> base = node.getLabel().get(key);
			switch (base.size()) {
			case 0:
				System.out.println("Key enthalten aber keine Strings");
				break;
			case 1:
				if (claden.containsKey(key)) {
					claden.get(key).add(node);
				} else {
					List<Node> split = new ArrayList<Node>();
					split.add(node);
					claden.put(key, split);
				}
				break;
			default:
				if (!node.getChildren().isEmpty()) {
					for (Node i : node.getChildren()) {
						// Durchsuche Kinder nach Claden
						computeCladen(i, key);
					}
				} else {
					// Knoten hat keine Kinder aber zwei Basen (Darf nicht passieren)
					System.out.println("Blatt hat zwei Basen");
				}
				break;
			}
		} else {
			System.out.println("Key im Knoten nicht gefunden");
		}
	}

	public void evaluateCladen() {
		
	}

	public void undefNuc() {
		
	}

	public void splitKeys() {
		for(int key : rootKeys) {
			List<Node> nodes = claden.get(key);
			for(Node i : nodes) {
				
			}
		}
	}

	public Set<Integer> getRootKeys() {
		return rootKeys;
	}

}
