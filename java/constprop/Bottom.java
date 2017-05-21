package constprop;

/**
 * Created by Brad on 5/19/2017.
 */
public class Bottom extends ConstValue {
    public ConstValue join(ConstValue cv) {
        return this;
    }
    public boolean eq(ConstValue cv) {
        return cv instanceof Bottom;
    }
}
