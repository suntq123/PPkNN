package cn.ac.iscas.paillier;

public class EncryptedKDTreeNode {

    int size;
    int m;

    EncryptedKDTreePoint[] points;
    EncryptedKDTreePoint lb;
    EncryptedKDTreePoint ub;

    public EncryptedKDTreeNode(EncryptedKDTreePoint[] points, EncryptedKDTreePoint lb, EncryptedKDTreePoint ub){
        this.size = points.length;
        this.m = points[0].data.length;
        
        this.points = points;
        this.lb = lb;
        this.ub = ub;
    }
}
