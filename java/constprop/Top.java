package constprop;

/**
 * Created by Brad on 5/19/2017.
 */
public class Top extends ConstValue {
    public ConstValue join(ConstValue cv) {
        return cv;
    }

    public boolean eq(ConstValue cv) {
        return cv instanceof Top;
    }
}
