package cfg;

public class Pair<M, N> {
    private M fst;
    private N snd;

    public Pair(M fst, N snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public M getFirst() {
        return this.fst;
    }

    public N getSecond() {
        return this.snd;
    }
}
