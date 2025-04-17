package cn.ac.iscas.sknn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.ac.iscas.utils.RunningTimeCounter;

import static cn.ac.iscas.secretsharing.AdditiveSecretSharing.*;

/**
 *
 */
public class SKNNV2 {

    public static class Point {
        public BigInteger id;
        public BigInteger[] data;

        // public BigInteger label; // AG label

        public Point(int m) {
            data = new BigInteger[m];
        }

        public Point(BigInteger id, BigInteger[] data) {
            this.id = id;
            this.data = data;
        }
    }

    /*
     *
     */
    public static BigInteger[] secureNEuclideanDistance(PartyID partyID, Point[] points, BigInteger[] q,
            MultiplicationTriple triple, BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {

        int num = points.length;
        int m = q.length;

        BigInteger[] diffis = new BigInteger[num * m];
        for (int i = 0; i < num; i++) {
            int offset = i * m;
            for (int j = 0; j < m; j++) {
                diffis[offset + j] = points[i].data[j].subtract(q[j]).mod(mod);
            }
        }
        BigInteger[] tis = multiplyS(partyID, diffis, diffis, triple, mod, reader, writer);

        BigInteger[] distanceis = new BigInteger[num];
        for (int i = 0; i < num; i++) {
            int offset = i * m;
            distanceis[i] = BigInteger.ZERO;

            for (int j = 0; j < m; j++) {
                distanceis[i] = distanceis[i].add(tis[offset + j]).mod(mod);
            }
        }

        return distanceis;
    }

    public static Point[] secureLinearSKNN(PartyID partyID, Point[] points, BigInteger[] q, int k,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        //
        // RunningTimeCounter.startRecord(RunningTimeCounter.COMMUNICATION_TIME);
        // long preTime = System.currentTimeMillis();
        BigInteger[] distanceis = secureNEuclideanDistance(partyID, points, q, triple, mod, reader, writer);
        // long totalTime = System.currentTimeMillis() - preTime;
        // long commtime = RunningTimeCounter.get(RunningTimeCounter.COMMUNICATION_TIME);
        // long comptime = totalTime - commtime;
        // System.out.println("communication time = " + commtime + " ms");
        // System.out.println("computing time = " + comptime + " ms");

        // RunningTimeCounter.startRecord(RunningTimeCounter.COMMUNICATION_TIME);
        // long preTime = System.currentTimeMillis();
        secureLinearSKNNCore(partyID, points, distanceis, null, k, triple, rTuple, mod, reader, writer);
        // long totalTime = System.currentTimeMillis() - preTime;
        // long commtime = RunningTimeCounter.get(RunningTimeCounter.COMMUNICATION_TIME);
        // long comptime = totalTime - commtime;
        // System.out.println("communication time = " + commtime + " ms");
        // System.out.println("computing time = " + comptime + " ms");

        Point[] resulti = Arrays.copyOfRange(points, 0, k);

        return resulti;
    }

    /*
     *
     * 
     *
    */
    private static void secureLinearSKNNCore(PartyID partyID, Point[] points, BigInteger[] distances,
            BigInteger[] labels, int k, MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        boolean labelIsNull = (labels == null);

        int num = points.length; //
        int m = points[0].data.length; //

        //
        int count = 0;
        while (count < k) {
            int len = num - count; //

            while (len > 1) {
                //
                int offset = (len % 2 == 0) ? count : count + 1;

                //
                int subLen = len / 2;
                BigInteger[] leftis = Arrays.copyOfRange(distances, offset, offset + subLen);
                BigInteger[] rightis = Arrays.copyOfRange(distances, offset + subLen, offset + 2 * subLen);

                BigInteger[] cmpis = secureComparision(partyID, leftis, rightis, triple, rTuple, mod, reader, writer); // <bool(a < b)>

                //
                int tSize;
                if (labelIsNull)
                    tSize = (2 + m) * subLen; // ids | distances | points，：subLen + subLen + m * subLen
                else
                    tSize = (2 + m) * subLen + subLen; // ids | distances | points | labels，：subLen + subLen + m * subLen + subLen

                BigInteger[] t1is = new BigInteger[tSize]; // <bool(a < b)>
                BigInteger[] t2is = new BigInteger[tSize]; // <a - b>
                for (int i = 0; i < subLen; i++) {
                    int lIndex = offset + i, rIndex = lIndex + subLen;

                    // ids
                    t1is[i] = cmpis[i];
                    t2is[i] = points[lIndex].id.subtract(points[rIndex].id).mod(mod);

                    // distances
                    int dIndex = subLen + i;
                    t1is[dIndex] = cmpis[i];
                    t2is[dIndex] = distances[lIndex].subtract(distances[rIndex]).mod(mod);

                    // points
                    int pIndex = 2 * subLen + i * m;
                    for (int j = 0; j < m; j++) {
                        t1is[pIndex + j] = cmpis[i];
                        t2is[pIndex + j] = points[lIndex].data[j].subtract(points[rIndex].data[j]).mod(mod);
                    }

                    // label
                    if (!labelIsNull) {
                        int labelIndex = (2 + m) * subLen + i;
                        t1is[labelIndex] = cmpis[i];
                        t2is[labelIndex] = labels[lIndex].subtract(labels[rIndex]).mod(mod);
                    }
                }

                BigInteger[] mulis = multiplyS(partyID, t1is, t2is, triple, mod, reader, writer); // <bool(a < b)> * <a - b>

                //  <t> = A[left] = <a>, A[left] = <b> + <bool(a < b)> * <a - b>, A[right] = <t> + <b> - A[left]
                for (int i = 0; i < subLen; i++) {
                    int lIndex = offset + i, rIndex = lIndex + subLen;

                    // ids
                    BigInteger[] ti = conditionSwap(mulis[i], points[lIndex].id, points[rIndex].id, mod);
                    points[lIndex].id = ti[0];
                    points[rIndex].id = ti[1];

                    // distances
                    int dIndex = subLen + i;
                    ti = conditionSwap(mulis[dIndex], distances[lIndex], distances[rIndex], mod);
                    distances[lIndex] = ti[0];
                    distances[rIndex] = ti[1];

                    // points
                    int pIndex = 2 * subLen + i * m;
                    for (int j = 0; j < m; j++) {
                        ti = conditionSwap(mulis[pIndex + j], points[lIndex].data[j], points[rIndex].data[j], mod);
                        points[lIndex].data[j] = ti[0];
                        points[rIndex].data[j] = ti[1];
                    }

                    // label
                    if (!labelIsNull) {
                        int labelIndex = (2 + m) * subLen + i;
                        ti = conditionSwap(mulis[labelIndex], labels[lIndex], labels[rIndex], mod);
                        labels[lIndex] = ti[0];
                        labels[rIndex] = ti[1];
                    }
                }

                len = (len % 2 == 0) ? subLen : subLen + 1;
            }

            count++;
        }
    }

    private static BigInteger[] conditionSwap(BigInteger muli, BigInteger ai, BigInteger bi, BigInteger mod) {
        BigInteger[] resulti = new BigInteger[2];
        resulti[0] = bi.add(muli).mod(mod);
        resulti[1] = ai.add(bi).subtract(resulti[0]).mod(mod);

        return resulti;
    }

    public static class AG {

        public BigInteger label; //
        public Point[] points; //
        public BigInteger[] subLabels; // label

        public AG(int num, int m) {
            points = new Point[num];
            for (int i = 0; i < num; i++) {
                points[i] = new Point(m);
            }

            subLabels = new BigInteger[num];
        }

        public AG(BigInteger label, Point[] points, BigInteger[] subLabels) {
            this.label = label;
            this.points = points;
            this.subLabels = subLabels;
        }
    }

    public static class VG {

        public Point low, high; //
        public Point[] points; //
        public BigInteger[] subLabels; // label

        public VG(int num, int m) {
            points = new Point[num];
            for (int i = 0; i < num; i++) {
                points[i] = new Point(m);
            }

            subLabels = new BigInteger[num];
        }

        public VG(Point low, Point high, Point[] points, BigInteger[] subLabels) {
            this.low = low;
            this.high = high;
            this.points = points;
            this.subLabels = subLabels;
        }
    }

    /*
     *
     * 
     *
     */
    public static Point[] secureVoronoiSKNN(PartyID partyID, AG[] ags, VG[] vgs, BigInteger[] q, int k,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        if (q.length != 2) {
            System.out.println("Currently only supports the case of m=2");
            return null;
        }

        //
        BigInteger MAX_DISTANCE = mod.divide(BigInteger.TWO).subtract(BigInteger.ONE);

        int count = 0; //
        Point[] resulti = new Point[k]; //

        int agNum = ags.length; //
        int agSize = ags[0].points.length; //
        int vgNum = vgs.length; //
        int vgSize = vgs[0].points.length; //
        int m = q.length; //

        /***  ***/
        //  计算 bool( low_i <= q_i < high_i ) = ( 1 - bool( q_i <= low_i ) * bool( q_i < high_i) 
        BigInteger[] t1i = new BigInteger[vgNum * m * 2]; //   q_1, ..., q_m   | q_1, ..., q_m
        BigInteger[] t2i = new BigInteger[vgNum * m * 2]; // low_1, ..., low_m | high_1, ..., high_m
        for (int i = 0; i < vgNum; i++) {
            int index = i * m * 2;
            for (int j = 0; j < m; j++) {
                t1i[index + j] = q[j];
                t2i[index + j] = vgs[i].low.data[j];

                t1i[index + m + j] = q[j];
                t2i[index + m + j] = vgs[i].high.data[j];
            }
        }

        BigInteger[] cmpis = secureComparision(partyID, t1i, t2i, triple, rTuple, mod, reader, writer);

        // bool( low_i <= q_i ) = 1 - bool( q_i < low_i )
        for (int i = 0; i < vgNum; i++) {
            int index = i * m * 2;

            for (int j = 0; j < m; j++) {
                cmpis[index + j] = shareConstant(partyID, BigInteger.ONE).subtract(cmpis[index + j]).mod(mod);
            }
        }

        // PROD( bool(low_i <= q_i) * bool(q_i < high_i) )
        t1i = new BigInteger[vgNum * m]; // bool(low_i <= q_i)
        t2i = new BigInteger[vgNum * m]; // bool(q_i < high_i)
        for (int i = 0; i < vgNum; i++) {
            int index1 = i * m;
            int index2 = i * m * 2;

            for (int j = 0; j < m; j++) {
                t1i[index1 + j] = cmpis[index2 + j];
                t2i[index1 + j] = cmpis[index2 + m + j];
            }
        }
        BigInteger[] mulis = multiplyS(partyID, t1i, t2i, triple, mod, reader, writer);

        t1i = new BigInteger[vgNum];
        t2i = new BigInteger[vgNum];
        for (int i = 0; i < vgNum; i++) {
            int index = i * 2;

            t1i[i] = mulis[index];
            t2i[i] = mulis[index + 1];
        }

        BigInteger[] alphais = multiplyS(partyID, t1i, t2i, triple, mod, reader, writer);

        /***  ***/
        BigInteger[][] lDatas = new BigInteger[vgNum][];
        Point[][] pDatas = new Point[vgNum][];
        for (int i = 0; i < vgNum; i++) {
            lDatas[i] = vgs[i].subLabels;
            pDatas[i] = vgs[i].points;
        }

        Point[] pointis = new Point[vgSize]; // 候选点集
        BigInteger[] labelis = new BigInteger[vgSize];
        getSelectedData(partyID, pointis, labelis, vgNum, vgSize, m, alphais, pDatas, lDatas,
                triple, mod, reader, writer);

        /***  ***/
        BigInteger[] distanceis = secureNEuclideanDistance(partyID, pointis, q, triple, mod, reader, writer);
        secureLinearSKNNCore(partyID, pointis, distanceis, labelis, 1, triple, rTuple, mod, reader, writer);
        resulti[count++] = new Point(pointis[0].id, pointis[0].data);

        pointis = Arrays.copyOf(pointis, 1);
        labelis = Arrays.copyOf(labelis, 1);
        distanceis = Arrays.copyOf(distanceis, 1);

        /***  ***/
        while (count < k) {
            int minIndex = 0; //

            t1i = new BigInteger[agNum]; //
            t2i = new BigInteger[agNum]; //
            for (int i = 0; i < agNum; i++) {
                t1i[i] = ags[i].label;
                t2i[i] = labelis[minIndex];
            }
            cmpis = secureEqual(partyID, t1i, t2i, triple, rTuple, mod, reader, writer);

            pDatas = new Point[agNum][];
            lDatas = new BigInteger[agNum][];
            for (int i = 0; i < agNum; i++) {
                lDatas[i] = ags[i].subLabels;
                pDatas[i] = ags[i].points;
            }

            Point[] agPointis = new Point[agSize];
            BigInteger[] agLabelis = new BigInteger[agSize];
            getSelectedData(partyID, agPointis, agLabelis, agNum, agSize, m, cmpis, pDatas, lDatas,
                    triple, mod, reader, writer);

            BigInteger[] agDistanceis = secureNEuclideanDistance(partyID, agPointis, q, triple, mod, reader, writer);

            /*** ***/
            distanceis[minIndex] = shareConstant(partyID, MAX_DISTANCE);
            int aLen = agSize * count;
            int tLen = pointis.length - 1;
            t1i = new BigInteger[aLen + tLen]; // agPoints' ids   ||  points' Ids
            t2i = new BigInteger[aLen + tLen]; //      minIds     ||    minIds
            for (int i = 0; i < count; i++) {
                int index = i * agSize;
                for (int j = 0; j < agSize; j++) {
                    t1i[index + j] = agPointis[j].id;
                }
                Arrays.fill(t2i, index, index + agSize, resulti[i].id);
            }
            for (int i = 0; i < tLen; i++) {
                t1i[aLen + i] = pointis[minIndex].id;
                t2i[aLen + i] = pointis[i + 1].id;
            }
            cmpis = secureEqual(partyID, t1i, t2i, triple, rTuple, mod, reader, writer); // bool( agId == minId ) || bool( pId == minId )

            BigInteger[] tsumi = new BigInteger[agSize];
            for (int i = 0; i < agSize; i++) {
                tsumi[i] = BigInteger.ZERO;
                for (int j = 0; j < count; j++) {
                    tsumi[i] = tsumi[i].add(cmpis[j * agSize + i]).mod(mod);
                }
            }

            // (1 - bool(Id==minId) ) * d + bool(Id==minId) * MAX_DISTANCE
            t1i = new BigInteger[2 * (agSize + tLen)]; //  (1 - bool(agId==minId) ) | bool(agId==minId)  ||  (1 - bool(pId==minId) ) | bool(pId==minId)
            t2i = new BigInteger[2 * (agSize + tLen)]; //       ag distance         |  MAX_DISTANCE      ||     points distance      |  MAX_DISTANCE
            for (int i = 0; i < agSize; i++) {
                int index = i * 2;
                t1i[index] = shareConstant(partyID, BigInteger.ONE).subtract(tsumi[i]).mod(mod);
                t1i[index + 1] = tsumi[i];

                t2i[index] = agDistanceis[i];
                t2i[index + 1] = shareConstant(partyID, MAX_DISTANCE);
            }
            int offset = 2 * agSize;
            for (int i = 0; i < tLen; i++) {
                int index = i * 2;
                t1i[offset + index] = shareConstant(partyID, BigInteger.ONE).subtract(cmpis[aLen + i]).mod(mod);
                t1i[offset + index + 1] = cmpis[aLen + i];

                t2i[offset + index] = distanceis[i + 1];
                t2i[offset + index + 1] = shareConstant(partyID, MAX_DISTANCE);
            }

            mulis = multiplyS(partyID, t1i, t2i, triple, mod, reader, writer);

            for (int i = 0; i < agSize; i++) {
                int index = i * 2;
                agDistanceis[i] = mulis[index].add(mulis[index + 1]).mod(mod);
            }
            for (int i = 0; i < tLen; i++) {
                int index = i * 2;
                distanceis[i + 1] = mulis[offset + index].add(mulis[offset + index + 1]).mod(mod);
            }

            int oldSize = pointis.length;
            int newSize = pointis.length + agSize;
            pointis = Arrays.copyOf(pointis, newSize);
            labelis = Arrays.copyOf(labelis, newSize);
            distanceis = Arrays.copyOf(distanceis, newSize);
            System.arraycopy(agPointis, 0, pointis, oldSize, agSize);
            System.arraycopy(agLabelis, 0, labelis, oldSize, agSize);
            System.arraycopy(agDistanceis, 0, distanceis, oldSize, agSize);

            secureLinearSKNNCore(partyID, pointis, distanceis, labelis, 1, triple, rTuple, mod, reader, writer);

            resulti[count++] = new Point(pointis[0].id, pointis[0].data);
        }

        return resulti;
    }

    private static void getSelectedData(PartyID partyID, Point[] pointis, BigInteger[] labelis, int num, int size,
            int m, BigInteger[] alphais, Point[][] pDatas, BigInteger[][] lDatas,
            MultiplicationTriple triple, BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {

        int tSize = size * (2 + m);
        BigInteger[] t1i = new BigInteger[num * tSize]; // label |  id   | pointData
        BigInteger[] t2i = new BigInteger[num * tSize]; // alpha | alpha | alpha
        for (int i = 0; i < num; i++) {
            int index1 = i * tSize;

            for (int j = 0; j < size; j++) {
                int index2 = index1 + j * (2 + m);

                t1i[index2] = lDatas[i][j];
                t1i[index2 + 1] = pDatas[i][j].id;
                System.arraycopy(pDatas[i][j].data, 0, t1i, index2 + 2, m);
            }

            Arrays.fill(t2i, index1, index1 + tSize, alphais[i]);
        }
        BigInteger[] mulis = multiplyS(partyID, t1i, t2i, triple, mod, reader, writer);

        for (int i = 0; i < size; i++) {
            labelis[i] = BigInteger.ZERO;

            pointis[i] = new Point(m);
            pointis[i].id = BigInteger.ZERO;
            pointis[i].data = new BigInteger[m];
            for (int j = 0; j < m; j++) {
                pointis[i].data[j] = BigInteger.ZERO;
            }
        }

        for (int i = 0; i < num; i++) {
            int index1 = i * tSize;

            for (int j = 0; j < size; j++) {
                int index2 = index1 + j * (2 + m);

                labelis[j] = labelis[j].add(mulis[index2]).mod(mod);
                pointis[j].id = pointis[j].id.add(mulis[index2 + 1]).mod(mod);

                for (int l = 0; l < m; l++) {
                    pointis[j].data[l] = pointis[j].data[l].add(mulis[index2 + 2 + l]).mod(mod);
                }
            }
        }
    }

    public static void main(String[] args) {

        List<BigInteger> list = new ArrayList<>();
        list.add(BigInteger.valueOf(0L));
        list.add(BigInteger.valueOf(1L));
        list.add(BigInteger.valueOf(2L));

        BigInteger[] array = list.toArray(new BigInteger[] {});
        System.out.println(array);
    }
}
