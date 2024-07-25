package cn.ac.iscas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import cn.ac.iscas.utils.Util;

public class TestSocketServer {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8001)) {

            Socket socket = serverSocket.accept();

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

            // Util.writeInt(0, outputStream);
            // int x = Util.readInt(inputStream);
            // System.out.println(x);

            // Util.writeBigInteger(new BigInteger("2"), outputStream);
            // System.out.println(Util.readBigInteger(inputStream));

            // Util.writeInt(4, outputStream);
            // Util.writeInt(5, outputStream);
            // Util.writeInt(6, outputStream);
            // Util.writeBigInteger(new BigInteger("5"), outputStream);
            // Util.writeBigInteger(new BigInteger("6"), outputStream);
            // Util.writeBigInteger(new BigInteger("7"), outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
