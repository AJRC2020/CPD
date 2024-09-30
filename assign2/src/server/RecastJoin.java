package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RecastJoin implements Runnable {
    private String udp_ip;
    private String tcp_ip;
    private int udp_port;
    private String message;
    private Thread thread;

    public RecastJoin(String ip, int port, String message, String ip2) {
        this.udp_ip = ip;
        this.udp_port = port;
        this.message = message;
        this.tcp_ip = ip2;
    }

    public void run() {
        for (int i = 0; i < 2; i++) {
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Error while sleeping");
            }
            try {
                File counter = new File(tcp_ip + "/counter.txt");
                Scanner scan1 = new Scanner(counter);
                int ctn = Integer.parseInt(scan1.nextLine());
                if (ctn == 0) {
                    scan1.close();
                    return;
                }
                scan1.close();
            } catch(IOException e) {
                e.printStackTrace();
                System.out.println("Error while reading file");
            }
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(udp_ip);
            } catch(UnknownHostException e) {
                e.printStackTrace();
                System.out.println("Error creating group");
            }
            try (DatagramSocket socket = new DatagramSocket()) {
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, udp_port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error while sending packet");
            }
            System.out.println("Success sending packet again");
        }
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error while sleeping");
        }
    
        endThread();
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }

    private void endThread() {
        try {
            File counter = new File(tcp_ip + "/counter.txt");
            FileWriter writer = new FileWriter(counter, false);
            writer.write("0");
            writer.close();

            File cluster = new File(tcp_ip + "/cluster.txt");
            Scanner reader = new Scanner(cluster);
            reader.nextLine();
            if (!reader.hasNextLine()) {
                File log = new File(tcp_ip + "/log.txt");
                FileWriter writer2 = new FileWriter(log, true);
                writer2.write("join-" + tcp_ip);
                writer2.close();
                reader.close();

                System.out.println("Created Clustered");
            }

            System.out.println("Joined Cluster");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error while creating/joining cluster");
        }
    }
}
