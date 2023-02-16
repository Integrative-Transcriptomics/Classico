public enum SNPType {
    A,
    G,
    T,
    C,
    N,
    REF;

    @Override
    public String toString() {
        switch(this) {
        case A: return "A";
        case G: return "G";
        case T: return "T";
        case C: return "C";
        case N: return "N";
        case REF: return ".";
        default: throw new IllegalArgumentException();
        }
    }

    /**
     * converts String to SNP type
     * 
     * @param s String containing the SNP type
     * @return SNP Type
     */
    public static SNPType fromString(String s){
        if ("Aa".contains(s)){
            return A;
        }
        else if ("Gg".contains(s)){
            return G;
        }
        else if ("Tt".contains(s)){
            return T;
        }
        else if ("Cc".contains(s)){
            return C;
        }
        else if (".,".contains(s)){
            return REF;
        }
        else if ("Nn".contains(s)){
            return N;
        }
        else{
            return null;
        }
    }
}