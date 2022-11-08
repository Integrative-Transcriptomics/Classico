public enum SNPType {
    A,
    G,
    T,
    C,
    N,
    REF;

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