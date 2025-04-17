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
 *
 */
public class SecureKNNC1 {

    public static void main(String[] args) throws IOException {
        int portC1 = 8004;

        AdditiveSecretSharing.PartyID partyID = AdditiveSecretSharing.PartyID.C1;

        ServerSocket serverSocket = new ServerSocket(portC1);
        Socket socketC2 = serverSocket.accept();
        PrintWriter writerC2 = new PrintWriter(socketC2.getOutputStream());
        BufferedReader readerC2 = new BufferedReader(new InputStreamReader(socketC2.getInputStream()));

        Socket socketUser = serverSocket.accept();
        PrintWriter writerUser = new PrintWriter(socketUser.getOutputStream());
        BufferedReader readerUser = new BufferedReader(new InputStreamReader(socketUser.getInputStream()));

        int testType = Util.readInt(readerUser);
        BigInteger mod = Util.readBigInteger(readerUser);
        AdditiveSecretSharing.MultiplicationTriple triple = AdditiveSecretSharing
                .parseJsonToMultiplicationTriple(readerUser.readLine());
        int testNumber = Util.readInt(readerUser);
        int pow = Util.readInt(readerUser);

        ComparisonTuple cTuple1 = AdditiveSecretSharing.parseJsonToComparisionTuple(readerUser.readLine());

        System.out.println("mod = " + mod);
        // System.out.println("a1 = " + a1 + ", b1 = " + b1 + ", c1 = " + c1);
        System.out.println("testNumber = " + testNumber);
        System.out.println("pow = " + pow);

        List<DataProcessor.Node> arrayTSecret = DataProcessor.receiveNodeArray(readerUser);
        // System.out.println(arrayTSecret.size());
        // System.out.println(arrayTSecret);

        List<DataProcessor.Entry> entriesSecret = DataProcessor.receiveEntries(readerUser);

        List<DataProcessor.Node> bucketsSecret = DataProcessor.receiveNodeArray(readerUser);

        long timeSum = 0l;
        long communicationTimeSum = 0l;
        for (int i = 0; i < testNumber; i++) {

            JSONObject queryCondition = DataProcessor.receiveQueryConditionJson(readerUser);
            int k = queryCondition.getInteger("k");
            Point point = JSONObject.parseObject(queryCondition.getString("point"), Point.class);

            RunningTimeCounter.startRecord(RunningTimeCounter.COMMUNICATION_TIME);
            List<BigInteger[]> result = null;
            long timePre = System.currentTimeMillis();
            if (testType == 0) {
                result = SecureHierarchicalList.secureKNNSearch(partyID, arrayTSecret, k, point, pow,
                        triple, cTuple1, mod, readerC2, writerC2);
            } else if (testType == 1) {
                result = SecureHierarchicalList.secureLinearKNNSearch(partyID, entriesSecret, k, point, pow,
                        triple, cTuple1, mod, readerC2, writerC2);
            } else if (testType == 2) {
                result = SecureHierarchicalList.secureBucketKNNSearch(partyID, bucketsSecret, k, point, pow, triple,
                        cTuple1, mod, readerC2, writerC2);
            }
            timeSum += System.currentTimeMillis() - timePre;
            communicationTimeSum += RunningTimeCounter.get(RunningTimeCounter.COMMUNICATION_TIME);
            // System.out.println(result);

            writerUser.write(JSONObject.toJSONString(result) + "\n");
            writerUser.flush();
        }
        long timeAvg = timeSum / testNumber;
        long communicationTimeAvg = communicationTimeSum / testNumber;
        long computingTimeAvg = timeAvg - communicationTimeAvg;
        Util.writeLong(timeAvg, writerUser);
        Util.writeLong(communicationTimeAvg, writerUser);
        Util.writeLong(computingTimeAvg, writerUser);

        socketC2.close();
        socketUser.close();
        serverSocket.close();
    }
}
