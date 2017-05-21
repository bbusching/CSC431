package constprop;

/**
 * Created by Brad on 5/19/2017.
 */
public class ConstImm extends ConstValue {
    private int val;

    public ConstImm(int val) {
        this.val = val;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ConstImm) {
            return Integer.compare(this.val, ((ConstImm) o).val) == 0;
        }
        return false;
    }

    public int getVal() {
        return val;
    }

    public ConstValue join(ConstValue cv) {
        if (cv instanceof Bottom) {
            return new Bottom();
        } else if (cv instanceof ConstImm) {
            if (((ConstImm) cv).getVal() == val) {
                return new ConstImm(val);
            } else {
                return new Bottom();
            }
        } else {
            return new ConstImm(val);
        }
    }

    public boolean eq(ConstValue cv) {
        return cv instanceof ConstImm && ((ConstImm) cv).getVal() == val;
    }
}
