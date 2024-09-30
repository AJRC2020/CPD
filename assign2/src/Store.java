package g07.assign2.src;

import java.io.*;
import java.net.*;
import java.util.*;
import server.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Store {
    public static void main(String[] args) {
        if (args.length != 4)
            return;

        String tcp_ip = args[0];
        int tcp_port = Integer.parseInt(args[1]);
        String udp_ip = args[2];
        int udp_port = Integer.parseInt(args[3]);

        File file = new File(tcp_ip + "/");

        if (file.mkdir()) {
            if(createStore(tcp_ip + "/", tcp_ip)) {
                return;
            }
        }

        UDPServer udp_server = new UDPServer(udp_port, tcp_port, udp_ip, tcp_ip);
        TCPServer tcp_server = new TCPServer(udp_port, tcp_port, udp_ip, tcp_ip);

        udp_server.start();
        tcp_server.start();
    }

    private static boolean createStore(String folder, String tcp_ip) {
        try {
            File counter = new File(folder + "counter.txt");
            FileWriter writer = new FileWriter(counter);
            writer.write("1");
            writer.close();
        
            File cluster = new File(folder + "cluster.txt");
            FileWriter writer2 = new FileWriter(cluster);
            String ip_key;

            try { 
               ip_key = calculateKey(tcp_ip);
            } catch(NoSuchAlgorithmException e) {
                e.printStackTrace();
                System.out.println("Error generating key");
                writer2.close();
                return true;
            }

            writer2.write(ip_key + " " + tcp_ip);
            writer2.close();

            File log = new File(folder + "log.txt");
            FileWriter writer3 = new FileWriter(log);
            writer3.write("");
            writer3.close();
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Error creating folder");
            return true;
        }

        return false;
    }

    private static String calculateKey(String value) throws NoSuchAlgorithmException {
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            byte[] decimalKey = digester.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder(decimalKey.length * 2);
            for (int i = 0; i < decimalKey.length; i++) {
                String hex = Integer.toHexString(0xff & decimalKey[i]);
                if (hex.length() == 1)
                    builder.append("0");
                builder.append(hex);
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error while creating key");
            e.printStackTrace();
        }

        return null;
    }
}

//servidor tcp (multi-thread)(Jankov)
//comunicacao udp, sockets, datagramsPackage
//receber/mandar do client e de outros nodes
//put diferente - key = hash(key) mandar para um node
//recebe texto
//cada um tem um folder, key e o nome do ficheirp
