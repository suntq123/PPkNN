package cn.ac.iscas;

import com.alibaba.fastjson.JSONObject;
import cn.ac.iscas.rtree.Point;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing;
import cn.ac.iscas.secretsharing.AdditiveSecretSharing.ComparisonTuple;
import cn.ac.iscas.sknn.SecureHierarchicalList;
import cn.ac.iscas.utils.DataProcessor;
import cn.ac.iscas.utils.RunningTimeCounter;
import cn.ac.iscas.utils.Util;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * 本实验为sKNN实验，一共四个角色：data owner、C_1、C_2、data user
 * C2：首先接收data owner的T表的秘密分享，然后接受data user的查询请求，与C_1进行两方安全计算，并将结果返回给data user。
 */
public class SecureKNNC2 {

    public static void main(String[] args) throws IOException {

        int portC1 = 8004;
        int portC2 = 8005;

        AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C2;

        Socket socketC1 = new Socket("127.0.0.1", portC1);
        PrintWriter writerC1 = new PrintWriter(socketC1.getOutputStream());
        BufferedReader readerC1 = new BufferedReader(new InputStreamReader(socketC1.getInputStream()));

        ServerSocket serverSocketUser = new ServerSocket(portC2);
        Socket socketUser = serverSocketUser.accept();
        PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
        BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

        int testType = Util.readInt(readerUser);
        BigInteger mod = Util.readBigInteger(readerUser);
        AdditiveSecretSharing.MultiplicationTriple triple = AdditiveSecretSharing
                .parseJsonToMultiplicationTriple(readerUser.readLine());
        int testNumber = Util.readInt(readerUser);
        int pow = Util.readInt(readerUser);

        ComparisonTuple cTuple2 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());

        System.out.println("mod = " + mod);
        // System.out.println("a2 = " + a2 + ", b2 = " + b2 + ", c2 = " + c2);
        System.out.println("testNumber = " + testNumber);
        System.out.println("pow = " + pow);

        // 首先接收data user的T表的秘密分享
        List<DataProcessor.Node> arrayTSecret = DataProcessor.receiveNodeArray(readerUser);
        // System.out.println(arrayTSecret.size());
        // System.out.println(arrayTSecret);

        List<DataProcessor.Entry> entriesSecret = DataProcessor.receiveEntries(readerUser);

        List<DataProcessor.Node> bucketsSecret = DataProcessor.receiveNodeArray(readerUser);

        long timeSum = 0l;
        for (int i = 0; i < testNumber; i++) {

            // 然后接受data user的查询请求
            JSONObject queryCondition = DataProcessor.receiveQueryConditionJson(readerUser);
            int k = queryCondition.getInteger("k");
            Point point = JSONObject.parseObject(queryCondition.getString("point"), Point.class);
            // System.out.println("Receive from data user: k=" + k + "\tpoint=" + point);

            // 与C_1进行两方安全计算
            RunningTimeCounter.startRecord(RunningTimeCounter.COMMUNICATION_TIME);
            List<BigInteger[]> result = null;
            long timePre = System.currentTimeMillis();
            if (testType == 0) {
                result = SecureHierarchicalList.secureKNNSearch(partyID, arrayTSecret, k, point, pow,
                        triple, cTuple2, mod, readerC1, writerC1);
            } else if (testType == 1) {
                result = SecureHierarchicalList.secureLinearKNNSearch(partyID, entriesSecret, k, point,
                        pow, triple, cTuple2, mod, readerC1, writerC1);
            } else if (testType == 2) {
                result = SecureHierarchicalList.secureBucketKNNSearch(partyID, bucketsSecret, k, point, pow, triple,
                        cTuple2, mod, readerC1, writerC1);
            }
            timeSum += System.currentTimeMillis() - timePre;
            // System.out.println(result);

            // 将结果返回给data user
            writerUser.write(JSONObject.toJSONString(result) + "\n");
            writerUser.flush();
        }
        long timeAvg = timeSum / testNumber;
        Util.writeLong(timeAvg, writerUser);

        socketC1.close();
        socketUser.close();
        serverSocketUser.close();
    }
}
