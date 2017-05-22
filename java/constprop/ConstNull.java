package constprop;

/**
 * Created by Brad on 5/22/2017.
 */
public class ConstNull extends  ConstValue {
    @Override
    public ConstValue join(ConstValue cv) {
        if (cv instanceof Bottom || cv instanceof ConstImm) {
            return new Bottom();
        } else {
            return new ConstNull();
        }
    }

    @Override
    public boolean eq(ConstValue cv) {
        return cv instanceof ConstNull;
    }
}
