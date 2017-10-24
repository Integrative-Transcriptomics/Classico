/**
 * @author Alexander Seitz
 *
 */
package datastructures;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class SNPTable {

	private String inputFile;

	private Map<Integer, Integer> snpPosNum = new HashMap<Integer, Integer>();
	private Map<String, List<String>> snps = new HashMap<String, List<String>>();
	// Map<Integer, Set<String>> deletions = new HashMap<Integer, Set<String>>();
	private List<Integer> snpPos = new ArrayList<Integer>();
	private List<String> sampleNames = new LinkedList<String>();

	/**
	 * 
	 */
	public SNPTable(String inputFile) {
		this.inputFile = inputFile;
		readSNPTable();
	}

	// public SNPTable(String[] vcfFiles, String file){
	// this.inputFile = file;
	// for(String sample: vcfFiles){
	// File vcf = new File(sample);
	// String sampleName = vcf.getParent();
	//
	// }
	// }

	/**
	 * @param table
	 * @param fastAEntry
	 */
	public SNPTable(Map<String, Map<Integer, String>> table, FastAEntry fastAEntry) {
		for (String sampleName : table.keySet()) {
			this.sampleNames.add(sampleName);
			Map<Integer, String> snpMap = table.get(sampleName);
			List<Integer> snpPositions = new ArrayList<Integer>(snpMap.keySet());
			Collections.sort(snpPositions);
			if (this.snpPos.isEmpty()) {
				this.snpPos = snpPositions;
				for (int i = 1; i <= snpPositions.size(); i++) {
					this.snpPosNum.put(snpPositions.get(i - 1), i);
				}
			}
			List<String> snps = new ArrayList<String>();
			for (Integer snpP : snpPositions) {
				snps.add(snpMap.get(snpP));
			}
			this.snps.put(sampleName, snps);
		}
		List<String> snps = new ArrayList<String>();
		for (Integer snpP : this.snpPos) {
			snps.add(fastAEntry.getIthChar(snpP - 1));
		}
		this.snps.put("Ref", snps);
	}

	private void readSNPTable() {
		try {
			System.out.println(inputFile);
			@SuppressWarnings("resource")
			BufferedReader br = new BufferedReader(new FileReader(this.inputFile));
			String line = "";
			Integer lineNum = 0;
			String[] names = new String[0];
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split("\t");
				if (lineNum > 0) {
					Integer SNP = Integer.parseInt(splitted[0]);
					this.snpPosNum.put(SNP, lineNum);
					this.snpPos.add(SNP);
					for (int i = 0; i < names.length; i++) {
						String sample = names[i];
						String snp = splitted[i + 1];
						if (this.snps.containsKey(sample)) {
							List<String> lis = this.snps.get(sample);
							lis.add(snp);
							this.snps.put(sample, lis);
						} else {
							List<String> lis = new ArrayList<String>();
							lis.add(snp);
							this.snps.put(sample, lis);
						}
					}
				} else {
					names = Arrays.copyOfRange(splitted, 1, splitted.length);
					for (String n : names) {
						if (!"Ref".equals(n)) {
							this.sampleNames.add(n);
						}
					}
				}
				lineNum++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public List<Integer> getSNPs() {
		return this.snpPos;

	}

	/**
	 * @param snpPos
	 * @return
	 */
	public String getSnp(Integer snpPos, String sample) {
		if (this.snpPosNum.containsKey(snpPos)) {
			Integer snpNum = this.snpPosNum.get(snpPos);
			return this.snps.get(sample).get(snpNum - 1);
		} else {
			return null;
		}
	}

	public String getReferenceSnp(Integer snpPos) {
		return getSnp(snpPos, "Ref");
	}

	/**
	 * @return
	 */
	public List<String> getSampleNames() {
		return this.sampleNames;
	}

	public String getRefCall(Integer pos) {
		return this.getSnp(pos, "Ref");
	}

	/**
	 * @param name
	 * @param snps2
	 */
	public void add(String name, Map<Integer, String> newSnps) {
		if (!this.sampleNames.contains(name)) {
			this.sampleNames.add(name);
			List<String> sampleSNPs = new ArrayList<String>();
			for (Integer pos : this.snpPos) {
				if (newSnps.containsKey(pos)) {
					sampleSNPs.add(newSnps.get(pos));
				} else {
					sampleSNPs.add("N");
				}
			}
			this.snps.put(name, sampleSNPs);
		} else {
			System.err.println("could not add sample to snpTable");
			System.err.println("sample name already exists: " + name);
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Position");
		result.append("\t");
		result.append("Ref");
		for (String sample : this.sampleNames) {
			result.append("\t");
			result.append(sample);
		}
		result.append("\n");
		for (Integer pos : this.snpPos) {
			result.append(pos);
			result.append("\t");
			result.append(getReferenceSnp(pos));
			for (String sample : this.sampleNames) {
				result.append("\t");
				result.append(getSnp(pos, sample));
			}
			result.append("\n");
		}
		return result.toString();
	}

	public String toAlignment() {
		return toAlignment(new HashSet<Integer>());
		// StringBuffer result = new StringBuffer();
		// for(String sample:this.sampleNames){
		// result.append(">");
		// result.append(sample);
		// result.append("\n");
		// int snpNum=0;
		// for(int i=0; i<this.snps.get(sample).size(); i++){
		// String snp = this.snps.get(sample).get(i);
		// if(".".equals(snp)){
		// result.append(this.snps.get("Ref").get(i));
		// }else{
		// result.append(snp);
		// }
		// snpNum++;
		// if(snpNum%80==0){
		// result.append("\n");
		// }
		// }
		// result.append("\n");
		// }
		// return result.toString();
	}

	/**
	 * @param toExclude
	 * @return
	 */
	public String toAlignment(Set<Integer> toExclude) {
		StringBuffer result = new StringBuffer();
		for (String sample : this.sampleNames) {
			result.append(">");
			result.append(sample.replace(" ", "_"));
			result.append("\n");
			int snpNum = 0;
			for (Integer pos : this.snpPos) {
				if (toExclude.contains(pos)) {
					continue;
				}
				String snp = getSnp(pos, sample);
				if (".".equals(snp)) {
					result.append(getSnp(pos, "Ref"));
				} else {
					result.append(snp);
				}
				snpNum++;
				if (snpNum % 80 == 0) {
					result.append("\n");
				}
			}
			// for(int i=0; i<this.snps.get(sample).size(); i++){
			// getS
			// String snp = this.snps.get(sample).get(i);
			// }
			result.append("\n");
		}
		return result.toString();
	}

	public List<String> compareTo(SNPTable table) {
		List<String> result = new LinkedList<String>();
		List<String> otherSampleNames = table.getSampleNames();
		List<String> bothSampleNames = new ArrayList<String>();
		;
		for (String sample : this.sampleNames) {
			if (otherSampleNames.contains(sample)) {
				bothSampleNames.add(sample);
			}
		}
		List<Integer> othersnps = table.getSNPs();
		TreeSet<Integer> allPositions = new TreeSet<Integer>();
		allPositions.addAll(this.snpPos);
		allPositions.addAll(othersnps);
		for (Integer pos : allPositions) {
			if (this.snpPos.contains(pos) && othersnps.contains(pos)) {
				for (String s : bothSampleNames) {
					String s1 = getSnp(pos, s);
					String s2 = table.getSnp(pos, s);
					if (!s1.equals(s2)) {
						result.add("=\t" + pos + "\t" + s + "\t" + s1 + "/" + s2);
					}
				}
			} else if (this.snpPos.contains(pos)) {
				for (String s : this.sampleNames) {
					String snp = getSnp(pos, s);
					if (!"N".equals(snp) && !".".equals(snp)) {
						result.add("+\t" + pos + "\t" + s + "\t" + snp);
					}
				}
			} else {
				for (String s : table.getSampleNames()) {
					String snp = table.getSnp(pos, s);
					if (!"N".equals(snp) && !".".equals(snp)) {
						result.add("-\t" + pos + "\t" + s + "\t" + snp);
					}
				}
			}
		}
		return result;
	}

}
