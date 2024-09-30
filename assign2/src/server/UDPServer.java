package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class UDPServer implements Runnable {
    private int udp_port;
    private int tcp_port;
    private String udp_ip;
    private String tcp_ip;
    private Thread thread;
    private MulticastSocket socket;

    public UDPServer(int udp_port, int tcp_port, String udp_ip, String tcp_ip) {
        this.tcp_ip = tcp_ip;
        this.tcp_port = tcp_port;
        this.udp_ip = udp_ip;
        this.udp_port = udp_port;
    }

    public void run() {
        try {
            socket = new MulticastSocket(udp_port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        InetAddress group = null;

        try {
            group = InetAddress.getByName(udp_ip);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        //InetSocketAddress group2 = new InetSocketAddress(group, udp_port);
        NetworkInterface netIf = null;

        try {
            netIf = NetworkInterface.getByName("bge0");
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        try {
            socket.joinGroup(new InetSocketAddress(group, 0), netIf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] buffer = new byte[256];
        //DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String message = new String(packet.getData(), 0, packet.getLength());
            /*try {
                socket.leaveGroup(group2, netIf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
            char op = message.charAt(0);
            String value = "";
            if (message.length() != 1)
                value = message.substring(2, message.length());
            String respose = "";
            switch(op) {
                case 'j':
                    respose = join(value);
                    System.out.println(respose);
                    break;
                
                case 'l':
                    respose = leave(value);
                    System.out.println(respose);
                    break;

                default:
                    System.out.println("Error on UDP Server");
                    break;
            }
            //socket.close();
        }
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }

    private String join(String value) {
        try {
            String[] verify = value.split(" ");
            if (verify[1] == tcp_ip) {
                return "Same node";
            }

            File counter = new File(tcp_ip + "/counter.txt");
            Scanner scan1 = new Scanner(counter);
            int ctn = Integer.parseInt(scan1.nextLine()); //NoSuchElementException
            scan1.close();
            if (ctn % 2 != 0) {
                return "Store not part of cluster";
            }

            if (checkMembership(verify[1])) return "Already sent";
            else {
                System.out.println("Inside");
                JoinSend sender = new JoinSend(verify[1], tcp_ip, tcp_port, verify[0]);
                sender.start();
            }
            
            String sending = "";
            File log = new File(tcp_ip + "/log.txt");
            File temp = new File(tcp_ip + "/temp.txt");
            Scanner reader_log = new Scanner(log);
            FileWriter author = new FileWriter(temp);
            author.write("join-" + verify[1]);
            sending += ("join-" + verify[1]);
            sending += " ";
            while(reader_log.hasNextLine()) {
                String line = reader_log.nextLine();
                if (line.equals("leave-" + verify[1])) continue;
                author.write("\n");
                author.write(line);
                sending += line;
                sending += " ";
            }
            reader_log.close();
            author.close();
            if (temp.renameTo(log)) System.out.println("Added join to log");
            else System.out.println("Failed to add join message to log");
            
            sending += "split ";

            File cluster = new File(tcp_ip + "/cluster.txt");
            FileWriter writer = new FileWriter(cluster, true);
            Scanner scan2 = new Scanner(cluster);
            
            while (scan2.hasNextLine()) {
                sending += scan2.nextLine();
                sending += " ";
            }

            scan2.close();
            writer.write("\n");
            writer.write(value);
            writer.close();

            if (sendCluster(sending, verify[1]))
                return "Sending information about the cluster";
            else
                return "Fail to send information";
        } 
        catch (IOException e) {
            e.printStackTrace();
            return "File not found";
        }
    }

    private boolean sendCluster(String value, String ip) {
        try (Socket ssocket = new Socket(ip, tcp_port)) {
            OutputStream output = ssocket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String message = "i " + value;
            writer.println(message);

            InputStream input = ssocket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String res = reader.readLine();
            if (res.equals("Success")) {
                return true;
            }
            else {
                return false;
            }
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
            return false;

        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
            return false;
        }
    }

    private boolean checkMembership(String ip) {
        try {
            File cluster = new File(tcp_ip + "/cluster.txt");
            Scanner reader = new Scanner(cluster);
            while (reader.hasNextLine()) {
                String[] line = reader.nextLine().split(" ");
                if (line[1].equals(ip)) {
                    reader.close();
                    return true;
                }
            }
            reader.close();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String leave(String value) {
        try {
            String[] verify = value.split(" ");
            if (verify[1] == tcp_ip) {
                return "Same node";
            }

            File counter = new File(tcp_ip + "/counter.txt");
            Scanner scan1 = new Scanner(counter);
            int ctn = Integer.parseInt(scan1.nextLine()); //NoSuchElementException
            scan1.close();
            if (ctn % 2 != 0) {
                return "Store not part of cluster";
            }

            File log = new File(tcp_ip + "/log.txt");
            File tempo = new File(tcp_ip + "/temp.txt");
            Scanner reader_log = new Scanner(log);
            FileWriter author = new FileWriter(tempo);
            author.write("leave-" + verify[1]);
            while(reader_log.hasNextLine()) {
                String line = reader_log.nextLine();
                if (line.equals("join-" + verify[1])) continue;
                author.write("\n");
                author.write(line);
            }
            if (tempo.renameTo(log)) System.out.println("Added leave to log");
            else System.out.println("Failed to add leave message to log");
            reader_log.close();
            author.close();

            File cluster = new File(tcp_ip + "/cluster.txt");
            File temp = new File(tcp_ip + "/temp.txt");
            BufferedReader reader = new BufferedReader(new FileReader(cluster));
            BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
            //String next, line = reader.readLine();
            String current = reader.readLine();
            writer.write(current.trim());
            while ((current = reader.readLine()) != null) {
                String trim = current.trim();
                if (trim.equals(value)) continue;
                writer.write("\n" + trim);
            }
            /*for (boolean last = (line == null); !last; line = next) {
                last = ((next = reader.readLine()) == null);
                String trim = line.trim();
                if (trim.equals(value)) continue;
                if (last) {
                    writer.write(trim);
                } else {
                    writer.write(trim +);
                }
            }*/

            writer.close();
            reader.close();
            if (temp.renameTo(cluster)) return "Eliminated node from cluster";
            else return "Failed to eliminate node";

        } catch(IOException e) {
            e.printStackTrace();
            return "Failed eliminating node";
        }
    }
}
