package cn.ac.iscas.secretsharing;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import com.alibaba.fastjson.JSON;

import cn.ac.iscas.utils.Util;

/**
 *
 */
public class AdditiveSecretSharing {

    // static {
    //     RunningTimeCounter.startRecord("C1_C2_NETWORK_TOTAL_COMMUNICATION_TIME");
    // }

    public enum PartyID {
        C1, C2
    }

    /**
     *
     */
    public static class MultiplicationTriple {
        // [a]_i, [b]_i, [c]_i
        public BigInteger ai, bi, ci;
        // private int ai, bi, ci;

        public MultiplicationTriple(BigInteger ai, BigInteger bi, BigInteger ci) {
            this.ai = ai;
            this.bi = bi;
            this.ci = ci;
        }
    }

    public static MultiplicationTriple[] generateMultiplicationTriples(BigInteger mod) {
        BigInteger a = Util.getRandomBigInteger(mod);
        BigInteger b = Util.getRandomBigInteger(mod);
        BigInteger c = a.multiply(b).mod(mod);

        BigInteger[] aSecrets = new BigInteger[2], bSecrets = new BigInteger[2], cSecrets = new BigInteger[2];
        aSecrets = AdditiveSecretSharing.randomSplit(a, mod);
        bSecrets = AdditiveSecretSharing.randomSplit(b, mod);
        cSecrets = AdditiveSecretSharing.randomSplit(c, mod);

        MultiplicationTriple[] triples = new MultiplicationTriple[2];
        triples[0] = new MultiplicationTriple(aSecrets[0], bSecrets[0], cSecrets[0]);
        triples[1] = new MultiplicationTriple(aSecrets[1], bSecrets[1], cSecrets[1]);

        System.out.println("a = " + a + ", b = " + b + ", c = " + c);

        return triples;
    }

    public static String parseMultiplicationTripleToJson(MultiplicationTriple triple) {
        return JSON.toJSONString(triple);
    }

    public static MultiplicationTriple parseJsonToMultiplicationTriple(String json) {
        return JSON.parseObject(json, MultiplicationTriple.class);
    }

    /**
     * [] represents in Z_{2^l}, <> represents in Z_2
     * s is data length.
     */
    public static class ComparisonTuple {

        public int s;
        public long twoPowS;

        public long ri; // <r>
        public long fi; // [f]

        public long[] alphas; // [alpha] = ([alpha_1], ..., [alpha_s])
        // public long[] betas; // [beta] = ([beta_1], ..., [beta_s])

        public long ui; // [u]
        public long vi; // [v]

        public ComparisonTuple(int s, long ri, long fi, long[] alphas, long ui, long vi) {
            this.s = s;
            this.twoPowS = BigInteger.TWO.pow(s).longValue();

            this.ri = ri;
            this.fi = fi;
            this.alphas = alphas;
            // this.betas = betas;
            this.ui = ui;
            this.vi = vi;
        }
    }

    /**
     * mod = 2^l
     * 
     * @param s
     * @param mod
     * @return
     */
    public static ComparisonTuple[] generateComparsionTuple(int s, long mod) {

        long twoPowS = BigInteger.TWO.pow(s).longValue();

        if (twoPowS >= mod * 4) {
            return null;
        }

        long r = Util.getRandomBigInteger(BigInteger.valueOf(mod)).longValue();
        long f;

        long modDivideTwo = mod / 2; // 2^{l-1}, mod = 2^l
        if (0 <= r && r < modDivideTwo) {
            f = 0;
        } else {
            f = 1;
        }

        long alpha = Util.modOperation(r, twoPowS); // r % twoPowS;
        long[] alphaBinary = Util.decimalToBinary(alpha, s); // LGB在数组低位，LSB在数组高位

        long u = Util.modOperation(Math.floorDiv(r, twoPowS), 2L); // Math.floorDiv(r, twoPowS) % 2;
        long v = Util.modOperation(Math.floorDiv(r - mod, twoPowS), 2L); // Math.floorDiv(r - mod, twoPowS) % 2;

        ComparisonTuple[] tuples = new ComparisonTuple[2];

        System.out.println("mod=" + mod + ", s=" + s + ", r=" + r + ", f=" + f + ", alpha=" + alpha
                + ", u=" + u + ", v=" + v);

        BigInteger[] rSplit = randomSplit(BigInteger.valueOf(r), BigInteger.valueOf(mod));
        BigInteger[] fSplit = randomSplit(BigInteger.valueOf(f), BigInteger.TWO);
        BigInteger[] uSplit = randomSplit(BigInteger.valueOf(u), BigInteger.TWO);
        BigInteger[] vSplit = randomSplit(BigInteger.valueOf(v), BigInteger.TWO);

        long[][] alphaBinarySplit = new long[2][s];
        for (int i = 0; i < s; i++) {
            BigInteger[] t = randomSplit(BigInteger.valueOf(alphaBinary[i]), BigInteger.TWO);
            alphaBinarySplit[0][i] = t[0].longValue();
            alphaBinarySplit[1][i] = t[1].longValue();
        }

        tuples[0] = new ComparisonTuple(s, rSplit[0].longValue(), fSplit[0].longValue(), alphaBinarySplit[0],
                uSplit[0].longValue(), vSplit[0].longValue());
        tuples[1] = new ComparisonTuple(s, rSplit[1].longValue(), fSplit[1].longValue(), alphaBinarySplit[1],
                uSplit[1].longValue(), vSplit[1].longValue());

        return tuples;
    }

    public static String parseComparisionTupleToJson(ComparisonTuple cTuple) {
        return JSON.toJSONString(cTuple);
    }

    public static ComparisonTuple parseJsonToComparisionTuple(String json) {
        return JSON.parseObject(json, ComparisonTuple.class);
    }

    /**
     *
     *
     * @param x
     * @param mod
     * @return [x_1, x_2]
     */
    public static BigInteger[] randomSplit(BigInteger x, BigInteger mod) {
        if (x == null)
            return new BigInteger[] { null, null };

        BigInteger[] xSecrets = new BigInteger[2];

        xSecrets[0] = Util.getRandomBigInteger(mod);
        xSecrets[1] = subtract(x, xSecrets[0], mod);

        return xSecrets;
    }

    /**
     *
     * @param partyID C_1 or C_2
     * @param a
     * @return
     */
    public static BigInteger shareConstant(PartyID partyID, BigInteger a) {
        return (partyID == PartyID.C1) ? a : BigInteger.ZERO;
    }

    public static long shareConstant(PartyID partyID, long a) {
        return (partyID == PartyID.C1) ? a : 0L;
    }

    public static BigInteger recover(PartyID partyID, BigInteger xi, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        Util.writeBigInteger(xi, writer);
        BigInteger xTemp = Util.readBigInteger(reader);

        return add(xi, xTemp, mod);
    }

    public static BigInteger[] recover(PartyID partyID, BigInteger[] xiArray, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int len = xiArray.length;

        // Util.writeBigIntegers(xiArray, writer);
        // BigInteger[] tiArray = Util.readBigIntegers(len, reader);
        BigInteger[] tiArray = Util.exchangeBigIntegers(xiArray, reader, writer);

        BigInteger[] result = new BigInteger[len];
        for (int i = 0; i < len; i++) {
            result[i] = add(xiArray[i], tiArray[i], mod);
        }

        return result;
    }

    public static long recover(PartyID partyID, long xi, long mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        Util.writeLong(xi, writer);
        long xTemp = Util.readLong(reader);

        return Util.modOperation(xi + xTemp, mod); // (xi + xTemp) % mod;
    }

    /**
     *
     *
     * @param x
     * @param y
     * @param mod
     * @return
     */
    public static BigInteger add(BigInteger x, BigInteger y, BigInteger mod) {
        return x.add(y).mod(mod);
    }

    public static BigInteger subtract(BigInteger x, BigInteger y, BigInteger mod) {
        return x.subtract(y).mod(mod);
    }

    /**
    *
    * <p>
    * z_i = alpha * x_i
    */
    public static BigInteger multiply(BigInteger alpha, BigInteger xi, BigInteger mod) {
        return alpha.multiply(xi).mod(mod);
    }

    public static long xor(PartyID partyID, long xi, long c) {
        return Util.modOperation(xi + shareConstant(partyID, c), 2L);
    }

    public static long xor(long xi, long yi) {
        return Util.modOperation(xi + yi, 2L);
    }

    /**
     *
     */
    public static BigInteger multiply(PartyID partyID, BigInteger xi, BigInteger yi, MultiplicationTriple triple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger ei = subtract(xi, triple.ai, mod);
        BigInteger fi = subtract(yi, triple.bi, mod);

        Util.writeBigIntegers(new BigInteger[] { ei, fi }, writer);
        BigInteger[] t = Util.readBigIntegers(2, reader);
        BigInteger e = add(ei, t[0], mod);
        BigInteger f = add(fi, t[1], mod);

        BigInteger x;
        if (partyID == PartyID.C1) // C_1计算： [z]_1 = f * [a]_1 + e * [b]_1 + [c]_1
            // x = f * triple.ai + e * triple.bi + triple.ci;
            x = f.multiply(triple.ai).add(e.multiply(triple.bi)).add(triple.ci);
        else
            // x = e * f + f * triple.ai + e * triple.bi + triple.ci;
            x = e.multiply(f).add(f.multiply(triple.ai)).add(e.multiply(triple.bi)).add(triple.ci);

        return x.mod(mod);

        // long ei = Util.modOperation(xi.longValue() - triple.ai.longValue(), mod.longValue()); // subtract(xi, triple.ai, mod);
        // long fi = Util.modOperation(yi.longValue() - triple.bi.longValue(), mod.longValue()); //  subtract(yi, triple.bi, mod);

        // writer.println(ei);
        // writer.println(fi);
        // writer.flush();
        // long e = Util.modOperation(ei + Util.readLong(reader), mod.longValue()); // add(ei, t[0], mod);
        // long f = Util.modOperation(fi + Util.readLong(reader), mod.longValue()); // add(fi, t[1], mod);

        // long x;
        // if (partyID == PartyID.C1) // C_1计算： [z]_1 = f * [a]_1 + e * [b]_1 + [c]_1
        //     // x = f * triple.ai + e * triple.bi + triple.ci;
        //     x = f * triple.ai.longValue() + e * triple.bi.longValue() + triple.ci.longValue();
        // else
        //     // x = e * f + f * triple.ai + e * triple.bi + triple.ci;
        //     x = e * f + f * triple.ai.longValue() + e * triple.bi.longValue() + triple.ci.longValue();

        // return BigInteger.valueOf(x).mod(mod);
    }

    public static BigInteger[] multiplyS(PartyID partyID, BigInteger[] xis, BigInteger[] yis,
            MultiplicationTriple triple, BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        int num = xis.length;
        BigInteger[] result = new BigInteger[num];

        BigInteger[] efis = new BigInteger[num * 2];
        for (int i = 0; i < num; i++) {
            efis[i] = subtract(xis[i], triple.ai, mod);
            efis[i + num] = subtract(yis[i], triple.bi, mod);
        }

        // Util.writeBigIntegers(efis, writer);        // 当数量太大，会卡住，估计是因为传输太多，爆栈？
        // BigInteger[] ts = Util.readBigIntegers(num * 2, reader);
        BigInteger[] ts = Util.exchangeBigIntegers(efis, reader, writer);

        for (int i = 0; i < num; i++) {

            BigInteger e = add(efis[i], ts[i], mod);
            BigInteger f = add(efis[i + num], ts[i + num], mod);

            BigInteger x;
            if (partyID == PartyID.C1) // C_1计算： [z]_1 = f * [a]_1 + e * [b]_1 + [c]_1
                // x = f * triple.ai + e * triple.bi + triple.ci;
                x = f.multiply(triple.ai).add(e.multiply(triple.bi)).add(triple.ci);
            else
                // x = e * f + f * triple.ai + e * triple.bi + triple.ci;
                x = e.multiply(f).add(f.multiply(triple.ai)).add(e.multiply(triple.bi)).add(triple.ci);

            result[i] = x.mod(mod);
        }

        return result;
    }

    /*
     *
     */
    public static BigInteger secureProduct(PartyID partyID, BigInteger[] xiArray, MultiplicationTriple triple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {

        if (xiArray == null || xiArray.length == 0)
            return null;

        while (xiArray.length > 1) {
            int subLen = xiArray.length / 2;
            BigInteger[] preiArray = Arrays.copyOfRange(xiArray, 0, subLen);
            BigInteger[] postiArray = Arrays.copyOfRange(xiArray, subLen, subLen * 2);

            BigInteger[] tiArray = multiplyS(partyID, preiArray, postiArray, triple, mod, reader, writer);

            if (xiArray.length % 2 != 0) {
                BigInteger taili = xiArray[xiArray.length - 1];

                xiArray = new BigInteger[subLen + 1];
                xiArray[subLen] = taili;
            } else {
                xiArray = new BigInteger[subLen];
            }

            System.arraycopy(tiArray, 0, xiArray, 0, subLen);
        }

        return xiArray[0];
    }

    public static BigInteger[] secureProduct(PartyID partyID, BigInteger[][] xiArrays, MultiplicationTriple triple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {

        int arrNum = xiArrays.length;
        int arrLen = xiArrays[0].length;

        while (xiArrays[0].length > 1) {
            int subLen = arrLen / 2;

            BigInteger[] preisArray = new BigInteger[arrNum * subLen];
            BigInteger[] postisArray = new BigInteger[arrNum * subLen];
            for (int i = 0; i < arrNum; i++) {
                System.arraycopy(xiArrays[i], 0, preisArray, i * subLen, subLen);
                System.arraycopy(xiArrays[i], subLen, postisArray, i * subLen, subLen);
            }
            // BigInteger[] preiArray = Arrays.copyOfRange(xiArray, 0, subLen);
            // BigInteger[] postiArray = Arrays.copyOfRange(xiArray, subLen, subLen * 2);

            BigInteger[] tisArray = multiplyS(partyID, preisArray, postisArray, triple, mod, reader, writer);

            if (arrLen % 2 != 0) {
                for (int i = 0; i < arrNum; i++) {
                    BigInteger tailii = xiArrays[i][arrLen - 1];

                    xiArrays[i] = new BigInteger[subLen + 1];
                    xiArrays[i][subLen] = tailii;
                }
                // BigInteger taili = xiArray[xiArray.length - 1];

                // xiArray = new BigInteger[subLen + 1];
                // xiArray[subLen] = taili;
            } else {
                for (int i = 0; i < arrNum; i++) {
                    xiArrays[i] = new BigInteger[subLen];
                }
                // xiArray = new BigInteger[subLen];
            }
            // if (xiArray.length % 2 != 0) {
            //     BigInteger taili = xiArray[xiArray.length - 1];

            //     xiArray = new BigInteger[subLen + 1];
            //     xiArray[subLen] = taili;
            // } else {
            //     xiArray = new BigInteger[subLen];
            // }

            for (int i = 0; i < arrNum; i++) {
                System.arraycopy(tisArray, i * subLen, xiArrays[i], 0, subLen);
            }
            // System.arraycopy(tiArray, 0, xiArray, 0, subLen);

            arrLen = xiArrays[0].length;
        }

        BigInteger[] resultis = new BigInteger[arrNum];
        for (int i = 0; i < arrNum; i++) {
            resultis[i] = xiArrays[i][0];
        }

        return resultis;
    }

    public static BigInteger secureExponentiation(PartyID partyID, BigInteger xi, int pow, MultiplicationTriple triple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        if (pow == 0) {
            if (partyID == PartyID.C1)
                return BigInteger.ONE;
            else
                return BigInteger.ZERO;
        }

        BigInteger result = xi;
        for (int i = 1; i < pow; i++) {
            result = multiply(partyID, result, xi, triple, mod, reader, writer);
        }

        return result;
    }

    /**
    *
    */
    public static BigInteger smallerThenPlus(PartyID partyID, BigInteger x, BigInteger a, MultiplicationTriple triple,
            BigInteger mod, BufferedReader reader, PrintWriter writer) throws IOException {
        // b=Bool(x < a)
        BigInteger bi = secureComparisionCore(partyID, x, shareConstant(partyID, a), triple, mod, reader, writer);
        BigInteger b = recover(partyID, bi, mod, reader, writer);

        if (b.equals(BigInteger.ONE)) { // x < a
            return x.add(shareConstant(partyID, a));
        } else {
            return x;
        }
    }

    /**
    * Secure decision node evaluation
    *
    */
    public static BigInteger secureComparision(PartyID partyID, BigInteger xi, BigInteger yi,
            MultiplicationTriple triple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        xi = smallerThenPlus(partyID, xi, mod, triple, mod, reader, writer);
        yi = smallerThenPlus(partyID, yi, mod, triple, mod, reader, writer);

        // System.out.println("xi = " + xi + ", yi = " + yi);

        return secureComparisionCore(partyID, xi, yi, triple, mod, reader, writer);
    }

    /**
     *
     * 
     * @param partyID
     * @param xi
     * @param yi
     * @param triple
     * @param mod
     * @param reader
     * @param writer
     * @return
     * @throws IOException
     */
    private static BigInteger secureComparisionCore(PartyID partyID, BigInteger xi, BigInteger yi,
            MultiplicationTriple triple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        BigInteger ai = xi.subtract(yi);

        StringBuilder binary = new StringBuilder(Util.decimalToBinary(ai, mod.bitLength()));

        if (partyID == PartyID.C1)
            return secureComparisionInC1(binary.toString(), triple, mod, reader, writer);
        else
            return secureComparisionInC2(binary.toString(), triple, mod, reader, writer);
    }

    /**
     * @param p
     * @param triple
     * @param mod
     * @param inputStream
     * @param outputStream
     * @return
     */
    private static BigInteger secureComparisionInC1(String p, MultiplicationTriple triple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        int index = p.length() - 1;

        BigInteger c1_1 = multiply(PartyID.C1, Util.charToBigInteger(p.charAt(index)), BigInteger.ZERO, triple,
                BigInteger.TWO, reader, writer);
        index--;

        while (index >= 1) {
            BigInteger dk1 = multiply(PartyID.C1, Util.charToBigInteger(p.charAt(index)), BigInteger.ZERO, triple,
                    BigInteger.TWO, reader, writer);
            dk1 = add(dk1, BigInteger.ONE, BigInteger.TWO);

            BigInteger ek1 = multiply(PartyID.C1, Util.charToBigInteger(p.charAt(index)), c1_1, triple, BigInteger.TWO,
                    reader, writer);
            ek1 = add(ek1, BigInteger.ONE, BigInteger.TWO);

            c1_1 = multiply(PartyID.C1, ek1, dk1, triple, BigInteger.TWO, reader, writer);
            c1_1 = add(c1_1, BigInteger.ONE, BigInteger.TWO);

            index--;
        }

        BigInteger al1 = add(Util.charToBigInteger(p.charAt(0)), c1_1, BigInteger.TWO);

        // [[a_l]] = [[t_1]] + [[t_2]] - 2 * [[t_1]] * [[t_2]]
        // BigInteger t3_1 = add(al1, BigInteger.ZERO, mod); // [[t_3]]_1 = [[t_1]]_1 + [[t_2]]_1
        // BigInteger t4_1 = multiply(PartyID.C1, al1, BigInteger.ZERO, triple, mod, reader, writer); // [[t_4]]_1 = [[t_1]]_1 * [[t_2]]_1
        // t4_1 = multiply(BigInteger.TWO, t4_1, mod); // [[t_4]]_1 = 2 * [[t_1]]_1 * [[t_2]]_1
        // al1 = subtract(t3_1, t4_1, mod); // [[a_l]]_1 = [[t_1]]_1 + [[t_2]]_1 - 2 * [[t_1]]_1 * [[t_2]]_1

        al1 = conversionZ2ToZP(PartyID.C1, al1, triple, mod, reader, writer);

        return al1;
    }

    private static BigInteger secureComparisionInC2(String q, MultiplicationTriple triple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        int index = q.length() - 1;

        BigInteger c1_2 = multiply(PartyID.C2, BigInteger.ZERO, Util.charToBigInteger(q.charAt(index)), triple,
                BigInteger.TWO, reader, writer);
        index--;

        while (index >= 1) {
            BigInteger dk2 = multiply(PartyID.C2, BigInteger.ZERO, Util.charToBigInteger(q.charAt(index)), triple,
                    BigInteger.TWO, reader, writer);

            BigInteger ek2 = multiply(PartyID.C2, Util.charToBigInteger(q.charAt(index)), c1_2, triple, BigInteger.TWO,
                    reader, writer);

            c1_2 = multiply(PartyID.C2, ek2, dk2, triple, BigInteger.TWO, reader, writer);

            index--;
        }

        //  <a_l> = <w_l> + <c_{l-1}>
        BigInteger al2 = add(Util.charToBigInteger(q.charAt(0)), c1_2, BigInteger.TWO);

        // // [[a_l]] = [[t_1]] + [[t_2]] - 2 * [[t_1]] * [[t_2]]
        // BigInteger t3_1 = add(BigInteger.ZERO, al2, mod); // [[t_3]]_1 = [[t_1]]_1 + [[t_2]]_1
        // BigInteger t4_1 = multiply(PartyID.C2, BigInteger.ZERO, al2, triple, mod, reader, writer); // [[t_4]]_1 = [[t_1]]_1 * [[t_2]]_1
        // t4_1 = multiply(BigInteger.TWO, t4_1, mod); // [[t_4]]_1 = 2 * [[t_1]]_1 * [[t_2]]_1
        // al2 = subtract(t3_1, t4_1, mod); // [[a_l]]_1 = [[t_1]]_1 + [[t_2]]_1 - 2 * [[t_1]]_1 * [[t_2]]_1

        al2 = conversionZ2ToZP(PartyID.C2, al2, triple, mod, reader, writer);

        return al2;
    }

    /**
     * s < l - 2 
     * mod = 2^l
     * 
     */
    public static BigInteger secureComparision(PartyID partyID, long xi, long yi,
            MultiplicationTriple triple, ComparisonTuple cTuple, long mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int s = cTuple.s;

        // C1 and C2 compute <z> = <x> - <y> + 2^s + <r>
        long zi = Util.modOperation(xi - yi + shareConstant(partyID, cTuple.twoPowS) + cTuple.ri, mod);

        // C1 and C2 recover z
        long z = recover(partyID, zi, mod, reader, writer);

        long zDivideTwoPowS = Util.modOperation(Math.floorDiv(z, cTuple.twoPowS), 2L); // Math.floorDiv(z, cTuple.twoPowS) % 2; // ( FLOOR(z / 2^s) % 2)

        long bi; // [Bool(x < y)]
        long modDivideTwo = mod / 2; // mod = 2^l, modDivedeTwo = 2^{l-1}
        long b1i = secureComparisionCore(partyID, z, cTuple.alphas, triple, s, cTuple.twoPowS, reader, writer); // [Bool(lambda < alpha)]
        if (modDivideTwo <= z && z < mod) { // 2^{l-1} <= z < 2^l

            // [Bool(x < y)] = 1 - (FLOOR(z / 2^s) % 2) + [u] + [Bool(lambda < alpha)]
            bi = Util.modOperation(shareConstant(partyID, 1L - zDivideTwoPowS) + cTuple.ui + b1i, 2L);
        } else {

            // C1 and C2 compute [phi] = 1 - ( FLOOR(z / 2^s) % 2) + [u] + [Bool(lambda < alpha)]
            long phii = Util.modOperation(shareConstant(partyID, 1L - zDivideTwoPowS) + cTuple.ui + b1i, 2L);

            // C1 and C2 compute [w] = 1 - ( FLOOR(z / 2^s) % 2) + [u] + [Bool(lambda < alpha)]
            long wi = Util.modOperation(shareConstant(partyID, 1L - zDivideTwoPowS) + cTuple.vi + b1i, 2L);

            // C1 and C2 compute [Bool(x < y)] = [phi] * [1 - f] + [w] * [f]
            // long t1i = multiply(partyID, BigInteger.valueOf(phii),
            //         BigInteger.valueOf(shareConstant(partyID, 1L) - cTuple.fi), triple,
            //         BigInteger.TWO, reader, writer).longValue(); // [phi] * [1 - f]
            // long t2i = multiply(partyID, BigInteger.valueOf(wi), BigInteger.valueOf(cTuple.fi), triple,
            //         BigInteger.TWO, reader, writer).longValue(); // [w] * [f]

            BigInteger[] tis = multiplyS(
                    partyID, new BigInteger[] { BigInteger.valueOf(phii), BigInteger.valueOf(wi) }, new BigInteger[] {
                            BigInteger.valueOf(shareConstant(partyID, 1L) - cTuple.fi), BigInteger.valueOf(cTuple.fi) },
                    triple, BigInteger.TWO, reader, writer);

            // bi = Util.modOperation(t1i + t2i, 2L); // (t1i + t2i) % 2;
            bi = Util.modOperation(tis[0].longValue() + tis[1].longValue(), 2L); // (t1i + t2i) % 2;
        }

        // C1 and C2 convert [Bool(x < y)] to <Bool(x < y)> using the conversion technique
        BigInteger ti = conversionZ2ToZP(partyID, BigInteger.valueOf(bi), triple, BigInteger.valueOf(mod), reader,
                writer);

        return ti;
    }

    /**
     * array: alpha or beta
     */
    public static long secureComparisionCore(PartyID partyID, long z, long[] array, MultiplicationTriple triple,
            int s, long twoPowS, BufferedReader reader, PrintWriter writer) throws IOException {

        // C1 and C2 compute lambda = z mod 2^s
        long lambda = Util.modOperation(z, twoPowS);

        long[] lambdaBinary = Util.decimalToBinary(lambda, s);

        // C1 and C2 compute: 
        long w1i = xor(partyID, array[0], lambdaBinary[0]); // [w1] = lambda_1 XOR [alpha_1]

        long[] wtis = new long[s]; // [w']
        wtis[0] = shareConstant(partyID, 1L) - w1i; // [w1'] = 1 - [w1]

        long[] vis = new long[s]; // [v]
        vis[0] = wtis[0]; // [v1] = [w1']

        for (int i = 1; i < s; i++) {
            // C1 and C2 compute:
            long wi = xor(partyID, array[i], lambdaBinary[i]); // [wi] = lambda_i XOR [alpha_i]
            wtis[i] = shareConstant(partyID, 1L) - wi; // [wi'] = 1 - [wi]

            // C1 and C2 compute [vi] = [v_{i-1}] * [w'i]
            vis[i] = multiply(partyID, BigInteger.valueOf(vis[i - 1]), BigInteger.valueOf(wtis[i]), triple,
                    BigInteger.TWO, reader, writer).longValue();
        }

        long[] deltais = new long[s]; // [delta]
        deltais[0] = xor(partyID, vis[0], 1L); // C1 and C2 compute [delta_1] = [v_1] XOR 1
        for (int i = 1; i < s; i++) {
            deltais[i] = xor(vis[i - 1], vis[i]); // C1 and C2 compute [delta_1] = [v_{i-1}] XOR [v_i]
        }

        long dlSumi = 0L; // SUM([delta_i] * \lambda_i)
        long dSumi = 0L; // SUM([delta_i])
        for (int i = 0; i < s; i++) {
            dlSumi += deltais[i] * lambdaBinary[i];
            dSumi += deltais[i];
        }

        // C1 and C2 compute: 
        long b1i = Util.modOperation(shareConstant(partyID, 1L) - dlSumi, 2L); // [Bool(lambda <= alpha)] = 1 - SUM([delta_i] * \lambda_i)
        long b2i = multiply(partyID, BigInteger.valueOf(b1i), BigInteger.valueOf(dSumi), triple,
                BigInteger.TWO, reader, writer).longValue(); // [Bool(lambda < alpha)] = [Bool(lambda <= alpha)] * SUM([delta_i])

        return b2i;
    }

    public static BigInteger conversionZ2ToZP(PartyID partyID, BigInteger xi, MultiplicationTriple triple,
            BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger t3_1; // [[t_3]]_1 = [[t_1]]_1 + [[t_2]]_1
        BigInteger t4_1; // [[t_4]]_1 = [[t_1]]_1 * [[t_2]]_1
        if (partyID == PartyID.C1) {
            t3_1 = add(xi, BigInteger.ZERO, mod);
            t4_1 = multiply(PartyID.C1, xi, BigInteger.ZERO, triple, mod, reader, writer);
        } else {
            t3_1 = add(BigInteger.ZERO, xi, mod);
            t4_1 = multiply(PartyID.C2, BigInteger.ZERO, xi, triple, mod, reader, writer);
        }

        t4_1 = multiply(BigInteger.TWO, t4_1, mod); // [[t_4]]_1 = 2 * [[t_1]]_1 * [[t_2]]_1

        return subtract(t3_1, t4_1, mod); // [[a_l]]_1 = [[t_1]]_1 + [[t_2]]_1 - 2 * [[t_1]]_1 * [[t_2]]_1
    }

    public static class RandomNumberTuple {
        public BigInteger r;
        public int l;
        public BigInteger[] rBinary;

        public RandomNumberTuple(BigInteger r, int l, BigInteger[] rBinary) {
            this.r = r;
            this.l = l;
            this.rBinary = rBinary;
        }
    }

    public static RandomNumberTuple[] generateRandomNumberTuples(int l, BigInteger mod) {
        RandomNumberTuple[] tuples = new RandomNumberTuple[2];

        BigInteger r = Util.getRandomBigInteger(mod);
        BigInteger[] rSecrets = randomSplit(r, mod);

        BigInteger[] rBinary = Util.decimalToBinaryV2(r, l);

        BigInteger[][] rBinarySecrets = new BigInteger[2][l];
        for (int i = 0; i < l; i++) {
            BigInteger[] secrets = randomSplit(rBinary[i], mod);
            rBinarySecrets[0][i] = secrets[0];
            rBinarySecrets[1][i] = secrets[1];
        }

        tuples[0] = new RandomNumberTuple(rSecrets[0], l, rBinarySecrets[0]);
        tuples[1] = new RandomNumberTuple(rSecrets[1], l, rBinarySecrets[1]);

        return tuples;
    }

    public static String parseRandomNumberTupleToJson(RandomNumberTuple rTuple) {
        return JSON.toJSONString(rTuple);
    }

    public static RandomNumberTuple parseJsonToRandomNumberTuple(String json) {
        return JSON.parseObject(json, RandomNumberTuple.class);
    }

    /**
    * SC - v2
    *  bool(a < b)
    *
    * mod: p，
    * 0 <= ai, bi < p/2
    *
    *
    */
    public static BigInteger secureComparision(PartyID partyID, BigInteger ai, BigInteger bi,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        //  <c> = <a> - <b>
        BigInteger ci = ai.subtract(bi).mod(mod);

        //  < c<p/2 >
        BigInteger ti = secureComparisionSub1(partyID, ci, triple, rTuple, mod, reader, writer);

        // < a<b > = 1 - < c<p/2 >
        BigInteger resulti = shareConstant(partyID, BigInteger.ONE).subtract(ti).mod(mod);

        return resulti;
    }

    /*
     * < a < p/2 >
     * mod: p
     */
    private static BigInteger secureComparisionSub1(PartyID partyID, BigInteger ai,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        // <x> = 2<a>
        BigInteger xi = BigInteger.TWO.multiply(ai).mod(mod);

        // <c> = <x> + <r>
        BigInteger ci = xi.add(rTuple.r).mod(mod);

        // open/recover c
        BigInteger c = recover(partyID, ci, mod, reader, writer);

        // <alpha> = <c0 XOR r0>。当c0 = 0，<alpha> = <r0>；当c0 = 1, <alpha> = 1 - <r0>。
        BigInteger c0 = c.mod(BigInteger.TWO);
        BigInteger alphai = (c0.equals(BigInteger.ZERO)) ? rTuple.rBinary[0]
                : shareConstant(partyID, BigInteger.ONE).subtract(rTuple.rBinary[0]);

        //  <beta> = < c<r >
        BigInteger betai = secureComparisionSub2(partyID, c, rTuple.rBinary, triple, rTuple, mod, reader, writer);

        // <x_0> = <beta> + <alpha> - 2 <alpha> <beta>
        BigInteger ti = multiply(partyID, alphai, betai, triple, mod, reader, writer);
        BigInteger x0i = alphai.add(betai).subtract(BigInteger.TWO.multiply(ti)).mod(mod);

        // < a<p/2 > = 1 - <x_0>
        BigInteger resulti = shareConstant(partyID, BigInteger.ONE).subtract(x0i).mod(mod);

        return resulti;
    }

    /*
     * < a < b >
     *
     */
    private static BigInteger secureComparisionSub2(PartyID partyID, BigInteger a, BigInteger[] biArray,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int l = rTuple.l;

        BigInteger[] aBinary = Util.decimalToBinaryV2(a, l);

        BigInteger[] ciArray = new BigInteger[l];
        for (int i = 0; i < l; i++) {
            ciArray[i] = (aBinary[i].equals(BigInteger.ZERO)) ? biArray[i]
                    : shareConstant(partyID, BigInteger.ONE).subtract(biArray[i]);
        }

        BigInteger[] diArray = new BigInteger[l];
        diArray[l - 1] = ciArray[l - 1];
        BigInteger[] eiArray = new BigInteger[l];
        eiArray[l - 1] = diArray[l - 1];
        for (int i = l - 2; i >= 0; i--) {
            // <d_{i+1} > < c_i >
            BigInteger ti = multiply(partyID, diArray[i + 1], ciArray[i], triple, mod, reader, writer);

            // <d_i> =  <d_{i+1} > +  < c_i > - <d_{i+1} > <c_i>
            diArray[i] = diArray[i + 1].add(ciArray[i]).subtract(ti).mod(mod);

            // <e_i> = <d_i> - <d_{i+1}>
            eiArray[i] = diArray[i].subtract(diArray[i + 1]).mod(mod);
        }

        // 计算 < a<b > = SUM( <e_i> <r_i> )
        BigInteger[] tiArray = multiplyS(partyID, eiArray, rTuple.rBinary, triple, mod, reader, writer);

        BigInteger sumi = BigInteger.ZERO;
        for (int i = 0; i < tiArray.length; i++) {
            sumi = sumi.add(tiArray[i]);
        }

        return sumi.mod(mod);
    }

    /**
    * SC S - v2
    * mod: p
    * 0 <= ai, bi < p/2
    *
    *
    */
    public static BigInteger[] secureComparision(PartyID partyID, BigInteger[] aiArray, BigInteger[] biArray,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int arrLen = aiArray.length;

        //  <c> = <a> - <b>
        BigInteger[] ciArray = new BigInteger[arrLen];
        for (int i = 0; i < arrLen; i++) {
            ciArray[i] = aiArray[i].subtract(biArray[i]).mod(mod);
        }

        //  < c<p/2 >
        BigInteger[] tiArray = secureComparisionSub1(partyID, ciArray, triple, rTuple, mod, reader, writer);

        //  < a<b > = 1 - < c<p/2 >
        BigInteger[] resultis = new BigInteger[arrLen];
        for (int i = 0; i < arrLen; i++) {
            resultis[i] = shareConstant(partyID, BigInteger.ONE).subtract(tiArray[i]).mod(mod);
        }

        return resultis;
    }

    /*
     * < a < p/2 >
     * mod: p
     */
    private static BigInteger[] secureComparisionSub1(PartyID partyID, BigInteger[] aiArray,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int arrayLen = aiArray.length;

        // <x> = 2<a>
        BigInteger[] xiArray = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            xiArray[i] = BigInteger.TWO.multiply(aiArray[i]).mod(mod);
        }

        // <c> = <x> + <r>
        BigInteger[] ciArray = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            ciArray[i] = xiArray[i].add(rTuple.r).mod(mod);
        }

        // open/recover c
        BigInteger[] cArray = recover(partyID, ciArray, mod, reader, writer);

        // <alpha> = <c0 XOR r0>。c0 = 0，<alpha> = <r0>；c0 = 1, <alpha> = 1 - <r0>。
        BigInteger[] alphaiArray = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            BigInteger c0 = cArray[i].mod(BigInteger.TWO);
            alphaiArray[i] = (c0.equals(BigInteger.ZERO)) ? rTuple.rBinary[0]
                    : shareConstant(partyID, BigInteger.ONE).subtract(rTuple.rBinary[0]);
        }

        //  <beta> = < c<r >
        BigInteger[][] triArray = new BigInteger[arrayLen][];
        for (int i = 0; i < arrayLen; i++) {
            triArray[i] = rTuple.rBinary;
        }
        BigInteger[] betaiArray = secureComparisionSub2(partyID, cArray, triArray, triple, rTuple, mod, reader, writer);

        // <x_0> = <beta> + <alpha> - 2 <alpha> <beta>
        BigInteger[] tis = multiplyS(partyID, alphaiArray, betaiArray, triple, mod, reader, writer);
        BigInteger[] resultis = new BigInteger[arrayLen];
        for (int i = 0; i < arrayLen; i++) {
            // BigInteger ti = multiply(partyID, alphaiArray[i], betaiArray[i], triple, mod, reader, writer);
            BigInteger x0i = alphaiArray[i].add(betaiArray[i]).subtract(BigInteger.TWO.multiply(tis[i])).mod(mod);

            // < a<p/2 > = 1 - <x_0>
            resultis[i] = shareConstant(partyID, BigInteger.ONE).subtract(x0i).mod(mod);
        }

        return resultis;
    }

    /*
     * < a < b >
     *
     */
    private static BigInteger[] secureComparisionSub2(PartyID partyID, BigInteger[] aArray, BigInteger[][] biArrays,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int arrLen = aArray.length;
        int l = rTuple.l;

        BigInteger[][] aBinarys = new BigInteger[arrLen][];
        for (int i = 0; i < arrLen; i++) {
            aBinarys[i] = Util.decimalToBinaryV2(aArray[i], l);
        }

        BigInteger[][] ciArrays = new BigInteger[arrLen][l];
        for (int j = 0; j < arrLen; j++) {
            for (int i = 0; i < l; i++) {
                ciArrays[j][i] = (aBinarys[j][i].equals(BigInteger.ZERO)) ? biArrays[j][i]
                        : shareConstant(partyID, BigInteger.ONE).subtract(biArrays[j][i]);
            }
        }

        BigInteger[][] diArrays = new BigInteger[arrLen][l];
        BigInteger[][] eiArrays = new BigInteger[arrLen][l];
        for (int i = 0; i < arrLen; i++) {
            diArrays[i][l - 1] = ciArrays[i][l - 1];
            eiArrays[i][l - 1] = diArrays[i][l - 1];
        }
        for (int i = l - 2; i >= 0; i--) {
            // <d_{i+1} > < c_i >
            BigInteger[] tdis = new BigInteger[arrLen];
            BigInteger[] tcis = new BigInteger[arrLen];
            for (int j = 0; j < arrLen; j++) {
                tdis[j] = diArrays[j][i + 1];
                tcis[j] = ciArrays[j][i];
            }
            BigInteger[] tis = multiplyS(partyID, tdis, tcis, triple, mod, reader, writer);

            for (int j = 0; j < arrLen; j++) {
                // <d_i> =  <d_{i+1} > +  < c_i > - <d_{i+1} > <c_i>
                diArrays[j][i] = diArrays[j][i + 1].add(ciArrays[j][i]).subtract(tis[j]).mod(mod);

                // <e_i> = <d_i> - <d_{i+1}>
                eiArrays[j][i] = diArrays[j][i].subtract(diArrays[j][i + 1]).mod(mod);
            }
        }

        // < a<b > = SUM( <e_i> <r_i> )
        BigInteger[] teis = new BigInteger[arrLen * l];
        BigInteger[] trbis = new BigInteger[arrLen * l];
        for (int i = 0; i < arrLen; i++) {
            System.arraycopy(eiArrays[i], 0, teis, i * l, l);
            System.arraycopy(rTuple.rBinary, 0, trbis, i * l, l);
        }
        BigInteger[] tiArray = multiplyS(partyID, teis, trbis, triple, mod, reader, writer);
        BigInteger[] sumiArray = new BigInteger[arrLen];
        for (int i = 0; i < arrLen; i++) {
            sumiArray[i] = BigInteger.ZERO;
            for (int j = 0; j < l; j++) {
                sumiArray[i] = sumiArray[i].add(tiArray[i * l + j]).mod(mod);
            }
        }

        return sumiArray;
    }

    /*
     *
     */
    public static BigInteger secureEqual(PartyID partyID, BigInteger ai, BigInteger bi,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        //  <c> = <a> - <b> + <r>
        BigInteger ci = ai.subtract(bi).add(rTuple.r).mod(mod);

        // open/recover c
        BigInteger c = recover(partyID, ci, mod, reader, writer);

        //  <c=r>
        BigInteger resulti = secureEqualSub(partyID, c, rTuple.r, triple, rTuple, mod, reader, writer);

        return resulti;
    }

    private static BigInteger secureEqualSub(PartyID partyID, BigInteger c, BigInteger ri,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        BigInteger[] cBinary = Util.decimalToBinaryV2(c, rTuple.l);

        BigInteger[] alphaiArray = new BigInteger[rTuple.l];
        for (int i = 0; i < rTuple.l; i++) {
            alphaiArray[i] = (cBinary[i].equals(BigInteger.ONE)) ? rTuple.rBinary[i]
                    : shareConstant(partyID, BigInteger.ONE).subtract(rTuple.rBinary[i]);
        }

        // <c=r> = PROD(<\alpha_i>)
        BigInteger resulti = secureProduct(partyID, alphaiArray, triple, mod, reader, writer);

        return resulti;
    }

    /*
    */
    public static BigInteger[] secureEqual(PartyID partyID, BigInteger[] aiArray, BigInteger[] biArray,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int arrLen = aiArray.length;

        // <c> = <a> - <b> + <r>
        BigInteger[] ciArray = new BigInteger[arrLen];
        for (int i = 0; i < arrLen; i++) {
            ciArray[i] = aiArray[i].subtract(biArray[i]).add(rTuple.r).mod(mod);
        }
        // BigInteger ci = ai.subtract(bi).add(rTuple.r).mod(mod);

        // open/recover c
        BigInteger[] cArray = recover(partyID, ciArray, mod, reader, writer);
        // BigInteger c = recover(partyID, ci, mod, reader, writer);

        // <c=r>
        BigInteger[] riArray = new BigInteger[arrLen];
        Arrays.fill(riArray, 0, arrLen, rTuple.r);
        BigInteger[] resulti = secureEqualSub(partyID, cArray, riArray, triple, rTuple, mod, reader, writer);

        return resulti;
    }

    private static BigInteger[] secureEqualSub(PartyID partyID, BigInteger[] cArray, BigInteger[] riArray,
            MultiplicationTriple triple, RandomNumberTuple rTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {

        int arrLen = cArray.length;
        int l = rTuple.l;

        BigInteger[][] cBinarys = new BigInteger[arrLen][];
        for (int i = 0; i < arrLen; i++) {
            cBinarys[i] = Util.decimalToBinaryV2(cArray[i], l);
        }
        // BigInteger[] cBinary = Util.decimalToBinaryV2(c, rTuple.l);

        BigInteger[][] alphaiArrays = new BigInteger[arrLen][l];
        for (int i = 0; i < arrLen; i++) {
            for (int j = 0; j < l; j++) {
                alphaiArrays[i][j] = (cBinarys[i][j].equals(BigInteger.ONE)) ? rTuple.rBinary[j]
                        : shareConstant(partyID, BigInteger.ONE).subtract(rTuple.rBinary[j]);
            }
        }
        // BigInteger[] alphaiArray = new BigInteger[rTuple.l];
        // for (int i = 0; i < rTuple.l; i++) {
        //     alphaiArray[i] = (cBinary[i].equals(BigInteger.ONE)) ? rTuple.rBinary[i]
        //             : shareConstant(partyID, BigInteger.ONE).subtract(rTuple.rBinary[i]);
        // }

        // <c=r> = PROD(<\alpha_i>)
        // BigInteger resulti = secureProduct(partyID, alphaiArray, triple, mod, reader, writer);
        BigInteger[] resultis = secureProduct(partyID, alphaiArrays, triple, mod, reader, writer);

        return resultis;
    }

    public static void generateRandomData(int bitLength) {
        Random random = new Random();

        // BigInteger mod = Util.getRandomPrimeBigInteger(bitLength - 1, bitLength);
        BigInteger mod = BigInteger.probablePrime(bitLength, random);
        BigInteger a = new BigInteger(bitLength / 2, random);
        BigInteger b = new BigInteger(bitLength / 2, random);
        BigInteger c = a.multiply(b);

        BigInteger[] aSecrets = randomSplit(a, mod);
        BigInteger[] bSecrets = randomSplit(b, mod);
        BigInteger[] cSecrets = randomSplit(c, mod);

        System.out.println("mod = " + mod);
        System.out.println("a = " + a + " : [" + aSecrets[0] + ", " + aSecrets[1] + "]");
        System.out.println("b = " + b + " : [" + bSecrets[0] + ", " + bSecrets[1] + "]");
        System.out.println("c = " + c + " : [" + cSecrets[0] + ", " + cSecrets[1] + "]");
    }

    /**
     * <max(x,y)> = <Bool(x<y)*y + (1-Bool(x<y))*x>
     *
     * @param partyID
     * @param xi
     * @param yi
     * @param triple
     * @param mod
     * @param inputStream
     * @param outputStream
     * @return
     * @throws IOException
     */
    public static BigInteger secureMax(PartyID partyID, BigInteger xi, BigInteger yi, MultiplicationTriple triple,
            ComparisonTuple cTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // BigInteger b1i = secureComparision(partyID, xi, yi, triple, mod, reader, writer); // Bool(x<y)
        BigInteger b1i = secureComparision(partyID, xi.longValue(), yi.longValue(), triple, cTuple, mod.longValue(),
                reader, writer);
        BigInteger t1i = multiply(partyID, b1i, yi, triple, mod, reader, writer); // Bool(x<y) * y

        BigInteger b2i = subtract(shareConstant(partyID, BigInteger.ONE), b1i, mod); // 1 - Bool(x<y)
        // int b2i = (partyID == PartyID.C1) ? subtract(1, b1i, mod) : subtract(0, b1i,
        // mod);
        BigInteger t2i = multiply(partyID, b2i, xi, triple, mod, reader, writer); // (1 - Bool(x<y)) * x

        return add(t1i, t2i, mod);
    }

    /**
     *
     *
     * @param partyID
     * @param pow
     * @param pi
     * @param qi
     * @param triple
     * @param mod
     * @param inputStream
     * @param outputStream
     * @return
     */
    public static BigInteger secureMinkowskiDistance(PartyID partyID, int pow, BigInteger[] pi, BigInteger[] qi,
            MultiplicationTriple triple, ComparisonTuple cTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger result = BigInteger.ZERO;

        if (pow > 1 && (pow % 2) == 0) {
            // z = x - y
            for (int i = 0; i < pi.length; i++) {
                BigInteger zii = subtract(pi[i], qi[i], mod); // zi[i]
                BigInteger t = secureExponentiation(partyID, zii, pow, triple, mod, reader, writer);
                result = add(result, t, mod);
            }
        } else {
            for (int i = 0; i < pi.length; i++) {
                // BigInteger phi1_i = secureComparision(partyID, pi[i], qi[i], triple, mod, reader, writer); // Bool(pi < qi)
                BigInteger phi1_i = secureComparision(partyID, pi[i].longValue(), qi[i].longValue(), triple, cTuple,
                        mod.longValue(), reader, writer); // Bool(pi < qi)

                BigInteger phi2_i = subtract(shareConstant(partyID, BigInteger.ONE), phi1_i, mod); // 1-Bool(pi < qi)

                BigInteger xi = subtract(qi[i], pi[i], mod);
                BigInteger yi = subtract(pi[i], qi[i], mod);

                BigInteger wi = multiply(partyID, phi1_i, xi, triple, mod, reader, writer);
                BigInteger vi = multiply(partyID, phi2_i, yi, triple, mod, reader, writer);

                BigInteger zii = add(wi, vi, mod);

                if (pow <= 0) { // Chebyshev Distance
                    result = secureMax(partyID, result, zii, triple, cTuple, mod, reader, writer);
                } else {
                    BigInteger t = secureExponentiation(partyID, zii, pow, triple, mod, reader, writer);
                    result = add(result, t, mod);
                }
            }
        }

        return result;
    }

    /**
     *
     * If q < l, the minimum distance is l - q;
     * If u < q, the minimum distance is q - u;
     * If l <= q <= u, the minimum distance is 0.
     *
     * @param partyID      C_i
     * @param intervali
     * @param qi
     * @param triple
     * @param mod
     * @param inputStream
     * @param outputStream
     * @return
     */
    public static BigInteger secureMinDistanceFromIntervalToPoint(PartyID partyID, BigInteger[] intervali,
            BigInteger qi,
            MultiplicationTriple triple, ComparisonTuple cTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        // BigInteger phi1i = secureComparision(partyID, qi, intervali[0], triple, mod, reader, writer); // Bool(q < l)
        // BigInteger phi2i = secureComparision(partyID, intervali[1], qi, triple, mod, reader, writer); // Bool(u < q)
        BigInteger phi1i = secureComparision(partyID, qi.longValue(), intervali[0].longValue(), triple, cTuple,
                mod.longValue(), reader, writer);
        BigInteger phi2i = secureComparision(partyID, intervali[1].longValue(), qi.longValue(), triple, cTuple,
                mod.longValue(), reader, writer);

        BigInteger t1i = multiply(partyID, phi1i, subtract(intervali[0], qi, mod), triple, mod, reader, writer); // phi_1 * (l - q)
        BigInteger t2i = multiply(partyID, phi2i, subtract(qi, intervali[1], mod), triple, mod, reader, writer); // phi_2 * (q - u)

        return add(t1i, t2i, mod);
    }

    /**
     *
     *
     * @param partyID      C_i
     * @param pow
     * @param boxi
     * @param qi
     * @param triple
     * @param mod
     * @param inputStream
     * @param outputStream
     * @return
     */
    public static BigInteger secureMinDistanceFromBoxToPoint(PartyID partyID, int pow, BigInteger[][] boxi,
            BigInteger[] qi,
            MultiplicationTriple triple, ComparisonTuple cTuple, BigInteger mod,
            BufferedReader reader, PrintWriter writer) throws IOException {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < qi.length; i++) {
            // <MD([l_i, u_i], q_i)>
            BigInteger zii = secureMinDistanceFromIntervalToPoint(partyID, boxi[i], qi[i], triple, cTuple, mod, reader,
                    writer);

            if (pow <= 0) {
                result = secureMax(partyID, zii, result, triple, cTuple, mod, reader, writer);
            } else {
                // <{MD([l_i, u_i], q_i)}^p>
                zii = secureExponentiation(partyID, zii, pow, triple, mod, reader, writer);
                result = add(result, zii, mod);
            }
        }

        return result;
    }

    public static void main(String[] args) {
        int bitLength = 20;
        Random random = new Random();

        // generateRandomData(20);

        BigInteger mod = BigInteger.valueOf(786431);
        BigInteger x = new BigInteger(bitLength / 2, random);
        BigInteger y = new BigInteger(bitLength / 2, random);

        BigInteger[] xSecrets = randomSplit(x, mod);
        BigInteger[] ySecrets = randomSplit(y, mod);

        System.out.println("mod = " + mod);
        System.out.println("x = " + x + " : [" + xSecrets[0] + ", " + xSecrets[1] + "]");
        System.out.println("y = " + y + " : [" + ySecrets[0] + ", " + ySecrets[1] + "]");

        System.out.println(Long.toBinaryString(bitLength));

        ComparisonTuple cTuple = new ComparisonTuple(1, 2, 3, new long[] { 4, 5 }, 6, 7);
        String json = AdditiveSecretSharing.parseComparisionTupleToJson(cTuple);
        System.out.println(json);
        ComparisonTuple cTuple2 = AdditiveSecretSharing.parseJsonToComparisionTuple(json);
        System.out.println(cTuple2);

        ComparisonTuple[] tuples = generateComparsionTuple(10, BigInteger.TWO.pow(15).longValue());
        System.out.println(AdditiveSecretSharing.parseComparisionTupleToJson(tuples[0]));
        System.out.println(AdditiveSecretSharing.parseComparisionTupleToJson(tuples[1]));

        RandomNumberTuple[] rTuples = generateRandomNumberTuples(10, BigInteger.valueOf(31));
        String rTuple1Json = AdditiveSecretSharing.parseRandomNumberTupleToJson(rTuples[0]);
        String rTuple2Json = AdditiveSecretSharing.parseRandomNumberTupleToJson(rTuples[1]);
        System.out.println(rTuple1Json);
        System.out.println(rTuple2Json);

        RandomNumberTuple trTuple1 = parseJsonToRandomNumberTuple(rTuple1Json);
        RandomNumberTuple trTuple2 = parseJsonToRandomNumberTuple(rTuple2Json);
        System.out.println(trTuple1);
        System.out.println(trTuple2);

        MultiplicationTriple[] triples = generateMultiplicationTriples(mod);
        String triple1Json = parseMultiplicationTripleToJson(triples[0]);
        System.out.println(triple1Json);

        MultiplicationTriple tTriple = parseJsonToMultiplicationTriple(triple1Json);
        System.out.println(parseMultiplicationTripleToJson(tTriple));

        System.out.println();
    }
}
