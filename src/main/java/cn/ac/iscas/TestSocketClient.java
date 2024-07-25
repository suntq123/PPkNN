package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import cn.ac.iscas.utils.Util;

public class TestSocketClient {

    public static void main(String[] args) {

        // String host = args[0];
        // int port = Integer.parseInt(args[1]);

        String host = "192.168.4.28";
        int port = 8001;

        // try(Socket socket = new Socket("localhost", 8001);) {
        try (Socket socket = new Socket(host, port);) {

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);

            Util.writeBigInteger(new BigInteger("1"), writer);
            Util.writeBigInteger(new BigInteger("2"), writer);
            Util.writeBigInteger(new BigInteger("3"), writer);

            System.out.println(Util.readBigInteger(reader));
            System.out.println(Util.readBigInteger(reader));
            System.out.println(Util.readBigInteger(reader));

            // int x = Util.readInt(inputStream);
            // System.out.println(x);
            // Util.writeInt(1, outputStream);

            // System.out.println(Util.readBigInteger(inputStream));
            // Util.writeBigInteger(new BigInteger("3"), outputStream);

            // System.out.println(Util.readInt(inputStream));
            // System.out.println(Util.readInt(inputStream));
            // System.out.println(Util.readInt(inputStream));
            // System.out.println(Util.readBigInteger(inputStream));
            // System.out.println(Util.readBigInteger(inputStream));
            // System.out.println(Util.readBigInteger(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
