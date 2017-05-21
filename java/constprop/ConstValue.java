package constprop;

public abstract class ConstValue {
    public abstract ConstValue join(ConstValue cv);
    public abstract boolean eq(ConstValue cv);
}
