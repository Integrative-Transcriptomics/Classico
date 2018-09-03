package project;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import datastructures.NewickTree;
import datastructures.Node;
import datastructures.SNPTable;
import datastructures.NewFile;

public class Project {
	public SNPTable snp;
	private NewickTree tree;
	private NewFile filepath;
	// Position(Base(Liste der Knoten))
	private Map<Integer, HashMap<String, List<Node>>> claden = new HashMap<Integer, HashMap<String, List<Node>>>();
	// Position(Base(Liste der Knoten)) nur die Positionen die für den Baum
	// sprechen
	private Map<Integer, HashMap<String, List<Node>>> supportTree = new HashMap<Integer, HashMap<String, List<Node>>>();
	// Position(Base(Liste der Knoten)) nur die Positionen die gegen den Baum
	// sprechen
	private Map<Integer, HashMap<String, List<Node>>> notSupportTree = new HashMap<Integer, HashMap<String, List<Node>>>();
	// Knotenid(Positionen) nur die die für den Baum sprechen
	private Map<Integer, List<Integer>> supportSplitKeys = new HashMap<Integer, List<Integer>>();
	// Knotenid(Positionen) nur die die gegen den Baum sprechen
	private Map<Integer, List<Integer>> notSupportSplitKeys = new HashMap<Integer, List<Integer>>();

	public Project(String snpFile, String newickTreeFile, String path) {
		snp = new SNPTable(snpFile);
		System.out.println(snp.getSampleNames());
		tree = new NewickTree(newickTreeFile);
		filepath = new NewFile(path);
		filepath.createDir("Ergebnis");
	}

	public Project(SNPTable snp, NewickTree tree, String path) {
		this.snp = snp;
		this.tree = tree;
		filepath = new NewFile(path);
		filepath.createDir("Ergebnis");
	}

	public static void main(String[] args0) throws IOException {
		if (args0.length == 3) {
			Project comcla = new Project(args0[0], args0[1], args0[2]);
			comcla.compute();
			List<Integer> showPositions = new ArrayList<Integer>();
			showPositions.add(44945);
			comcla.showPositions(showPositions);
			comcla.getResults();
		} else {
			System.err.println("Falsche Eingabeparameter: [SNP-Tabelle][Newick-Datei][Ergebnispfad]");
		}
	}

	public void compute() {
		for (int pos : snp.getSNPs()) {
			label(tree, pos);
			computeCladen(tree.getRoot(), pos, true);
			evaluateCladen(pos);
		}
		splitKeys(filepath.createFile("supportSplitKeys.txt"), true);
		splitKeys(filepath.createFile("notSupportSplitKeys.txt"), false);
		nameAndID(filepath.createFile("IDzuordnung.txt"));
	}

	public void computeCladen(Node node, int key, boolean withoutN) {

		if (node.getLabel().containsKey(key)) {
			Set<String> base = node.getLabel().get(key);
			switch (base.size()) {
			case 0:
				System.err.println("Project:86 - Key enthalten aber keine Strings");
				break;
			case 1:
				setClade(key, node, base.toString(), claden);
				break;
			case 2:
				if (withoutN && node.getLabel().get(key).contains("N")) {
					if (node.getId() == tree.getRoot().getId()) {
						break;
					}
					base.remove("N");
					setClade(key, node, base.toString(), claden);
					break;
				}
			default:
				if (!node.getChildren().isEmpty()) {
					for (Node i : node.getChildren()) {
						// Durchsuche Kinder nach Claden
						computeCladen(i, key, withoutN);
					}
				} else {
					// Knoten hat keine Kinder aber zwei Basen (Darf nicht
					// passieren)
					System.err.println("Project:110 - Blatt hat zwei Basen");
				}
				break;
			}
		} else {
			// TODO: Bedeutung??
			System.err.println(
					"Project:130 - Key im Knoten nicht gefunden " + key + "_" + node.getId() + "_" + node.getName());
		}
	}

	public void evaluateCladen(int pos) {
		HashMap<String, List<Node>> l = claden.get(pos);
		List<Integer> size = new ArrayList<Integer>();
		if (l != null) {
			for (String s : l.keySet()) {
				size.add(l.get(s).size());
			}
			Collections.sort(size);
			int max = size.get(size.size() - 1);
			if (max == size.get(0)) {
				max++;
			}
			for (String s : l.keySet()) {
				List<Node> ln = l.get(s);
				if (ln.size() < max) {
					if (ln.size() == 1) {
						for (Node n : ln) {
							setClade(pos, n, s, supportTree);
						}
					} else {
						for (Node n : ln) {
							setClade(pos, n, s, notSupportTree);
						}
					}
				}
			}
		}
	}

	public void splitKeys(String filename, boolean support) {
		Map<Integer, List<Integer>> splitKeys;
		if (support) {
			for (int key : supportTree.keySet()) {
				for (String s : supportTree.get(key).keySet()) {
					for (Node n : supportTree.get(key).get(s)) {
						setInt(n.getId(), key, supportSplitKeys);
					}
				}
			}
			splitKeys = supportSplitKeys;
		} else {
			for (int key : notSupportTree.keySet()) {
				for (String s : notSupportTree.get(key).keySet()) {
					for (Node n : notSupportTree.get(key).get(s)) {
						setInt(n.getId(), key, notSupportSplitKeys);
					}
				}
			}
			splitKeys = notSupportSplitKeys;
		}

		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			List<Integer> keyList = new ArrayList<Integer>();
			for (int j : splitKeys.keySet()) {
				keyList.add(j);
			}
			Collections.sort(keyList);
			for (int i : keyList) {
				Node ni = tree.getNode(i);
				if (ni.getId() != tree.getRoot().getId()) {
					bw.write(ni.getParent().getId() + "->" + i + "\t");
				} else {
					bw.write("Root " + i + "\t");
				}
				bw.write(splitKeys.get(i).size() + "\t");
				Collections.sort(splitKeys.get(i));
				for (int j : splitKeys.get(i)) {
					bw.write(j + ":" + tree.getNode(i).getLabel().get(j).toString());
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void label(NewickTree tree, Integer key) {
		for (String sample : snp.getSampleNames()) {
			String nuc = snp.getSnp(key, sample);
			String ref = snp.getReferenceSnp(key);
			if (nuc.equals(".")) {
				nuc = ref;
			}
			for (Node current : tree.getNodeList()) {
				if (current.getName().equals(sample)) {
					current.setLabel(key, nuc);
					break;
				}
			}
		}
		List<Node> missingNodes = new ArrayList<Node>();
		for (Node n : tree.getNodeList()) {
			if (!n.getLabel().containsKey(key) && n.getChildren().isEmpty()) {
				missingNodes.add(n);
			}
		}
		if (!missingNodes.isEmpty()) {
			// TODO: Grammatik 1 oder mehr Knoten
			// System.err.println("Baum stimmt nicht mit SNPTable überein,
			// Sample " + missingNodes.toString()
			// + " sind nicht in SNP enthalten");
		}
	}

	public void setClade(int key, Node node, String label, Map<Integer, HashMap<String, List<Node>>> claden) {
		if (claden.containsKey(key)) {
			if (claden.get(key).containsKey(label)) {
				claden.get(key).get(label).add(node);
			} else {
				List<Node> split = new ArrayList<Node>();
				split.add(node);
				claden.get(key).put(label, split);
			}

		} else {
			HashMap<String, List<Node>> labeledNode = new HashMap<String, List<Node>>();
			List<Node> split = new ArrayList<Node>();
			split.add(node);
			labeledNode.put(label, split);
			claden.put(key, labeledNode);
		}
	}

	public void getResults() {
		toFile(filepath.createFile("claden.txt"), claden);
		toFile(filepath.createFile("supportTree.txt"), supportTree);
		toFile(filepath.createFile("notSupportTree.txt"), notSupportTree);

	}

	public void toFile(String filename, Map<Integer, HashMap<String, List<Node>>> claden) {
		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i : claden.keySet()) {
				bw.write(i + ":\n");
				for (String l : claden.get(i).keySet()) {
					bw.write(l + ":\n");
					for (Node n : claden.get(i).get(l)) {
						bw.write(n.getId() + "-" + n.getName() + "-" + n.getLabel().get(i) + getChildren(n, i) + "\n");
					}
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getChildren(Node n, int pos) {
		String childrenString = ":";
		for (Node j : n.getChildren()) {
			childrenString += j.getId() + "-" + j.getName() + "-" + j.getLabel().get(pos) + ",";
		}
		if (childrenString.equals(":")) {
			return "";
		}
		return childrenString.substring(0, childrenString.length() - 1);
	}

	public void setInt(int id, int pos, Map<Integer, List<Integer>> splitKeys) {
		if (splitKeys.containsKey(id)) {
			splitKeys.get(id).add(pos);
		} else {
			List<Integer> set = new ArrayList<Integer>();
			set.add(pos);
			splitKeys.put(id, set);
		}
	}

	public void showPositions(List<Integer> positions) {
		for (int i : positions) {
			if (snp.getSNPs().contains(i) || i == -1) {
				showCladeAtPosition(i);
			} else {
				System.out.println("Position " + i + " nicht in Tabelle enthalten");
			}
		}
	}

	public void showCladeAtPosition(int pos) {
		String filename;
		if(pos == -1){
			filename = filepath.createFile("IDTree.nwk");
		}else{
			filename = filepath.createFile("labeledTree" + pos + ".nwk");
		}
		
		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(tree.toPositionString(pos, false, null));
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void nameAndID(String filename) {
		FileWriter fw;
		try {
			fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (Node i : tree.getNodeList()) {
				if (i.getChildren().isEmpty()) {
					bw.write(i.getId() + "\t" + i.getName() + "\n");
				} else {
					String childrenString = "";
					for (Node j : i.getChildren()) {
						childrenString += j.getId() + ",";
					}
					bw.write(i.getId() + "\t" + childrenString + "\n");
				}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getUniqueSubtreeSNPs(String filename, int idSubtree) {
		List<Node> subtreeNodes = new ArrayList<Node>();
		Node searchSubtree = tree.getNode(idSubtree);
		subtreeNodes = giveSubtreeNodes(searchSubtree);
		getCommonSNP(filename, subtreeNodes);
	}

	private List<Node> giveSubtreeNodes(Node subtree) {
		List<Node> subtreeNodelist = new ArrayList<Node>();
		subtreeNodelist.add(subtree);
		for (Node child : subtree.getChildren()) {
			subtreeNodelist.addAll(giveSubtreeNodes(child));
		}
		return subtreeNodelist;
	}

	private String getStrains(int edgeend) {
		Node subtree = tree.getNode(edgeend);
		String allStrains = "";
		if (subtree.getChildren().isEmpty()) {
			return subtree.getName() + ", ";
		} else {
			for (Node n : subtree.getChildren()) {
				allStrains = allStrains + getStrains(n.getId());
			}
		}
		return allStrains;
	}

	/**
	 * Sind alle Kinder eines Knotens teil der Anfrageliste, ist der Knoten ein
	 * Vorfahre
	 * 
	 * @param Liste
	 *            der Knoten der Gemeinsamkeiten
	 * @param Knoten
	 *            der Vorfahre sein könnte
	 * 
	 */
	public void commonSNPs(List<Integer> sampleIDs) {
		String filename = filepath.createFile("CommonSamples.txt");
		List<Node> sampleNodes = new ArrayList<Node>();
		List<Node> commonNodes = new ArrayList<Node>();
		for (int i : sampleIDs) {
			sampleNodes.add(tree.getNode(i));
		}

		for (Node n : sampleNodes) {
			Node parent = n;
			if (n.getChildren().isEmpty()) {
				if (n.getParent() != null) {
					while (ancestor(sampleNodes, parent.getParent())) {
						parent = parent.getParent();
						if (parent == null) {
							break;
						}
					}
				}
				if (ancestor(sampleNodes, parent)) {
					List<Node> allNodes = getAllNodesOfSubtree(parent);
					for (Node m : allNodes) {
						if (!commonNodes.contains(m)) {
							commonNodes.add(m);
						}
					}
				} else {
					commonNodes.add(n);
				}
			} else {
				System.out.println("Knoten " + n.getId() + " ist eine Klade kein Individuum");
			}
		}
		getCommonSNP(filename, commonNodes);

	}

	public void getCommonSNP(String filename, List<Node> nodeList) {
		try {
			FileWriter fw = new FileWriter(filename);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(">SupportTreeSNPs\n");
			List<Integer> posList = new ArrayList<Integer>(supportTree.keySet());
			Collections.sort(posList);
			String allStrains = "";
			for (int k : posList) {
				for (String s : supportTree.get(k).keySet()) {
					for (Node m : supportTree.get(k).get(s)) {
						if (nodeList.contains(m)) {
							if (m.getChildren().isEmpty()) {
								bw.write(k + "\t" + s + "\t" + m.getName() + "\n");
							} else {
								allStrains = getStrains(m.getId());
								bw.write(k + "\t" + s + "\t" + allStrains.substring(0, allStrains.length() - 2) + "\n");
							}

						}
					}
				}
			}
			bw.write(">NotSupportTreeSNPs\n");
			posList = new ArrayList<Integer>(notSupportTree.keySet());
			Collections.sort(posList);
			allStrains = "";
			for (int k : posList) {
				for (String s : notSupportTree.get(k).keySet()) {
					if (nodeList.containsAll(notSupportTree.get(k).get(s))) {
						for (Node m : notSupportTree.get(k).get(s)) {
							allStrains = allStrains + getStrains(m.getId());
						}
						bw.write(k + "\t" + s + "\t" + allStrains.substring(0, allStrains.length() - 2) + "\n");
					}
				}
			}
			bw.close();
		} catch (IOException e) {

		}
	}

	/**
	 * Sind alle Kinder eines Knotens teil der Anfrageliste, ist der Knoten ein
	 * Vorfahre
	 * 
	 * @param Liste
	 *            der Knoten der Gemeinsamkeiten
	 * @param Knoten
	 *            der Vorfahre sein könnte
	 * 
	 */
	public boolean ancestor(List<Node> nodeList, Node ancestor) {

		boolean allChildren = true;
		for (Node n : ancestor.getChildren()) {
			// Ist ein Blatt nicht in der Liste enthalten, wird der Knoten nicht
			// in die Liste aufgenommen
			if (!allChildren) {
				break;
			}
			// Liegt ein Blatt vor wird die Liste durchsucht
			if (n.getChildren().isEmpty()) {
				boolean inList = false;
				for (Node m : nodeList) {
					inList = n.getId() == m.getId();
					// Stimmt ein Knoten der Liste überein wird die Suche
					// abgebrochen
					if (inList) {
						break;
					}
				}
				// Ist der Knoten nicht enthalten wird sind nicht alle Knoten
				// enthalten
				if (!inList) {
					allChildren = false;
				}
			} else {
				// Ist das Kind ein innerer Knoten werden die Kinder dieses
				// Knotens betrachtet
				allChildren = ancestor(nodeList, n);
			}
		}
		return allChildren;
	}

	/**
	 * Erhält einen Knoten und gibt eine Liste aller Knoten des Teilbaums zurück
	 * 
	 * @param Wurzel
	 *            des Teilbaums
	 */
	public List<Node> getAllNodesOfSubtree(Node n) {
		List<Node> listOfNodes = new ArrayList<Node>();
		// Keine Kinder füge nur den Knoten der Liste hinzu
		if (n.getChildren().isEmpty()) {
			listOfNodes.add(n);
		} else {
			// füge die Knotenlisten der Kinder hinzu
			listOfNodes.add(n);
			for (Node m : n.getChildren()) {
				listOfNodes.addAll(getAllNodesOfSubtree(m));
			}
		}
		return listOfNodes;
	}

	/**
	 * Gibt einen Newick-String aus, der nur die Kladenallele und die
	 * Blattallele enthalten
	 * 
	 * @param SNP-Position
	 * 
	 */
	public void treeSNPs(int pos) {
		List<Node> nodeList = tree.getNodeList();
		for (Node m : nodeList) {
			m.setPosSNP(null);
		}
		Node subroot = nodeList.get(0);
		while (subroot.getParent() != null) {
			subroot = subroot.getParent();
		}
		try {

			HashMap<String, List<Node>> treeSNP = new HashMap<String, List<Node>>();
			if (supportTree.containsKey(pos)) {
				treeSNP = supportTree.get(pos);
			}

			if (notSupportTree.containsKey(pos)) {
				treeSNP.putAll(notSupportTree.get(pos));
			}
			if (treeSNP.isEmpty()) {
				System.err.println("Kein Key mit dem Wert " + pos + " in notSupportTree und SupportTree enthalten.");
			} else {
				String filename = filepath.createFile("Clade-Tree" + pos + ".nwk");
				FileWriter fw;
				fw = new FileWriter(filename);
				for (String s : treeSNP.keySet()) {
					for (Node n : treeSNP.get(s)) {
						for (Node l : nodeList) {
							if (n.getId() == l.getId()) {
								l.setPosSNP(s);
								break;
							}
						}
					}
				}
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(tree.toPositionString(pos, true, subroot));
				bw.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public NewFile getFilepath() {
		return filepath;
	}

	public Map<Integer, HashMap<String, List<Node>>> getSupportTree() {
		return supportTree;
	}

}
