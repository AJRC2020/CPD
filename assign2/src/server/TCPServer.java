package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class TCPServer implements Runnable {
    private int udp_port;
    private int tcp_port;
    private String udp_ip;
    private String tcp_ip;
    private Thread thread;
    private int join_counter = 0;
    private String recast_content;
    private boolean recast = false;

    public TCPServer(int udp_port, int tcp_port, String udp_ip, String tcp_ip) {
        this.tcp_ip = tcp_ip;
        this.tcp_port = tcp_port;
        this.udp_ip = udp_ip;
        this.udp_port = udp_port;
    }

    public void run() {

        try (ServerSocket serverSocket = new ServerSocket()) {
            InetAddress address = InetAddress.getByName(tcp_ip);
            SocketAddress endpoint = new InetSocketAddress(address, tcp_port);
            serverSocket.bind(endpoint);
            System.out.println("Server is listening on port " + tcp_port);

            while (true) {
                if (recast) {
                    RecastJoin recaster = new RecastJoin(udp_ip, udp_port, recast_content, tcp_ip);
                    recast = false;
                    recaster.start();
                }
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String message = reader.readLine();
                //System.out.println(message);
                char op = message.charAt(0);
                String value = "";
                if (message.length() != 1)
                    value = message.substring(2, message.length());
                //parse a message
                //resolver cada operacao
                String response = "";
                String[] parse;
                switch(op) {
                    case 'p':
                        parse = value.split(" ");
                        response = put(parse[0], parse[1], tcp_port);
                        break;
                    case 'g':
                        response = get(value);
                        break; 
                    case 'd':
                        response = delete(value);
                        break;
                    case 'j':
                        response = join();
                        break;
                    case 'l':
                        response = leave();
                        break;
                    case 's':
                        parse = value.split(" ");
                        response = createFile(parse[0], parse[1]);
                        break;
                    case 'i':
                        if (join_counter < 3) response = join_server(value);
                        break;
                    case 't':
                        response = get(value);
                        break;
                    case 'q':
                        response = tombstone(value);
                        break;
                    default:
                        response = "Error Server-Side in operations";
                        break;

                } 

                System.out.println("New client connected: ");

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(response);
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }

    private String put (String key, String value, int port) {
        List<String> keys = new ArrayList<>();
        List<String> ips = new ArrayList<>();
        File cluster = new File(tcp_ip + "/cluster.txt");
        Scanner reader;
        try {
            reader = new Scanner(cluster);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fail while opening cluster file";
        }
        while(reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] info = line.split(" ");
            keys.add(info[0]);
            ips.add(info[1]);
        }
        reader.close();

        int[] sending = get3Best(keys, key);
        for (int i = 0; i < 3; i++) {
            if (sending[i] == -1) break;
            else if (ips.get(sending[i]).equals(tcp_ip)) createFile(key, value);
            else sendPut(key, value, ips.get(sending[i]));
        }

        return "File saved in cluster";
    }

    private String get(String key) {
        String value = "";
        /*try {
            File myFile = new File(key);
            Scanner reader = new Scanner(myFile);
            while (reader.hasNextLine()) {
                value += reader.nextLine();
            }
            reader.close();
        } 
        catch (FileNotFoundException e){
            System.out.println("File not found");
            e.printStackTrace();
        }*/

        List<String> keys = new ArrayList<>();
        List<String> ips = new ArrayList<>();
        File cluster = new File(tcp_ip + "/cluster.txt");
        Scanner reader;
        try {
            reader = new Scanner(cluster);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fail while opening cluster file";
        }
        while(reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] info = line.split(" ");
            keys.add(info[0]);
            ips.add(info[1]);
        }
        reader.close();

        int[] sending = get3Best(keys, key);
        for (int i = 0; i < 3; i++) {
            if (sending[i] == -1) break;
            else if (ips.get(sending[i]).equals(tcp_ip)) { 
                value = getFile(key);
                if (!value.equals("fail"))  return value;
            }
            else {
                value = sendGet(key, ips.get(sending[i]));
                if (!value.equals("fail")) return value;
            }
        }

        return "Error getting file";
    }

    private String tombstone(String key) throws FileNotFoundException {
        File file = new File(tcp_ip + "/" + key + ".txt");
        File rename = new File(tcp_ip + "/deleted:" + key + ".txt");

        if (file.renameTo(rename)) {
            System.out.println("deletion");
            return "File deleted";
        } else {
            System.out.println("fail to delete");
            return "Failed to delete";
        }
    }
    
    private String delete(String key) throws FileNotFoundException {
        List<String> keys = new ArrayList<>();
        List<String> ips = new ArrayList<>();
        File cluster = new File(tcp_ip + "/cluster.txt");
        Scanner reader;
        try {
            reader = new Scanner(cluster);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Fail while opening cluster file";
        }
        while(reader.hasNextLine()) {
            String line = reader.nextLine();
            String[] info = line.split(" ");
            keys.add(info[0]);
            ips.add(info[1]);
        }
        reader.close();

        int[] sending = get3Best(keys, key);
        for (int i = 0; i < 3; i++) {
            if (sending[i] == -1) break;
            else if (ips.get(sending[i]).equals(tcp_ip)) tombstone(key);
            else sendDel(key, ips.get(sending[i]));
        }

        return "Sent tombstone order";
    }

    private String join() {
        File file = new File(tcp_ip + "/cluster.txt");
        Scanner scan;
        try {
            scan = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Error while reading file";
        }
        String line = scan.nextLine();
        String message = "j " + line;
        scan.close();
        recast_content = message;
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(udp_ip);
        } catch(UnknownHostException e) {
            e.printStackTrace();
            return "Error creating group";
        }
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, udp_port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error while sending packet";
        }

        recast = true;

        return "Success sending packet first time";
    }

    private String leave() {
        File file = new File(tcp_ip + "/cluster.txt");
        Scanner scan;
        try {
            scan = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "Error while reading file";
        }
        String line = scan.nextLine();
        String message = "l " + line;

        List<String> keys = new ArrayList<>();
        List<String> ips = new ArrayList<>();

        while(scan.hasNextLine()) {
            String lin = scan.nextLine();
            String[] split = lin.split(" ");
            keys.add(split[0]);
            ips.add(split[1]);
        }
        scan.close();
        File dir = new File(tcp_ip + "/");
        LeaveSend sender = new LeaveSend(ips, keys, dir.listFiles(), tcp_port, tcp_ip);
        sender.start();

        InetAddress addr = null;
        try {
            addr = InetAddress.getByName(udp_ip);
        } catch(UnknownHostException e) {
            e.printStackTrace();
            return "Error creating group";
        }
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, udp_port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error while sending packet";
        }

        return "Success sending packet first time";
    }

    private String createFile(String key, String value) {
        String res;
        key += ".txt";
        try {
            File newFile = new File(tcp_ip + "/" + key);
            FileWriter writer = new FileWriter(newFile);
            writer.write(value);
            writer.close();

            System.out.println("Saved file");

            res = "Successfuly stored with key: " + key;

        } catch(IOException e) {
            System.out.println("Error creating file");
            e.printStackTrace();

            res = "Error while storing file";
        }

        return res;
    }
    
    private int[] get3Best(List<String> ips, String key) {
        int[] res = {-1, -1, -1};
        long min1 = 0, min2 = 0, min3 = 0;
        int size = ips.get(0).getBytes(StandardCharsets.UTF_8).length;

        for (int i = 0; i < ips.size(); i++) {
            long diff = byteToLong(ips.get(i).getBytes(StandardCharsets.UTF_8)) - byteToLong(key.getBytes(StandardCharsets.UTF_8));
            if (diff < 0)
                diff += Math.pow(2, size - 1);
            if (i == 0) {
                min1 = diff;
                res[0] = 0;
            }
            else if (i == 1) {
                if (min1 > diff) {
                    min2 = min1;
                    min1 = diff;
                    res[1] = res[0];
                    res[0] = 1;
                }
                else {
                    min2 = diff;
                    res[1] = 1;
                }
            }
            else {
                if (min1 > diff) {
                    min3 = min2;
                    min2 = min1;
                    min1 = diff;
                    res[2] = res[1];
                    res[1] = res[0];
                    res[0] = i;
                }
                else if (min2 > diff) {
                    min3 = min2;
                    min2 = diff;
                    res[2] = res[1];
                    res[1] = i;
                }
                else if (i == 2) {
                    min3 = diff;
                    res[2] = 2;
                }
                else if(min3 > diff) {
                    min3 = diff;
                    res[2] = i;
                }
            }
        }

        return res;
    }

    private long byteToLong (byte[] bytes) {
    long result = 0;
    for (int i = 0; i < Long.BYTES; i++) {
        result <<= Byte.SIZE;
        result |= (bytes[i] & 0xFF);
    }
    return result;
    }

    private void sendPut(String key, String value, String ip) {
        String message = "s " + key + " " + value;

        try (Socket socket = new Socket(ip, tcp_port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String time = reader.readLine();

            System.out.println(time);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private String sendGet(String key, String ip) {
        String message = "t " + key;

        try (Socket socket = new Socket(ip, tcp_port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String time = reader.readLine();

            System.out.println(time);
            return time;

        } catch (UnknownHostException ex) {
            return "fail";
        } catch (IOException ex) {
            return "fail";
        }
    }

    private void sendDel(String key, String ip) {
        String message = "q " + key;

        try (Socket socket = new Socket(ip, tcp_port)) {

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String time = reader.readLine();

            System.out.println(time);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private String join_server(String value) {
        join_counter++;
        if (join_counter == 1) {
            try {
                String[] lines = value.split(" ");
                boolean change = false;
                File cluster = new File(tcp_ip + "/cluster.txt");
                File log = new File(tcp_ip + "/log.txt");
                FileWriter writer2 = new FileWriter(cluster, true);
                FileWriter writer3 = new FileWriter(log, true);
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].equals("split")) change = true;
                    else if (change) {
                        String form = lines[i];
                        i++;
                        form += " " + lines[i];
                        writer2.write("\n");
                        writer2.write(form);
                    }
                    else if (i == 0) writer3.write(lines[i]);
                    else {
                        writer3.write("\n");
                        writer3.write(lines[i]);
                    }
                }
                writer2.close();
                writer3.close();

                return "Success";
            } catch (IOException e) {
                e.printStackTrace();
                return "Fail";
            }

        }

        else if (join_counter < 4) {
            try {
                String[] lines = value.split("\n");
                boolean change = false;
                File cluster = new File(tcp_ip + "/cluster.txt");
                File log = new File(tcp_ip + "/log.txt");
                //File temp = new File(tcp_ip + "/temp.txt");
                Scanner reader = new Scanner(cluster);
                Scanner reader2 = new Scanner(log);
                FileWriter writer1 = new FileWriter(cluster, true);
                //FileWriter writer2 = new FileWriter(temp);
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].equals("split")) change = true;
                    else if (change) {
                        String form = lines[i];
                        i++;
                        form += " " + lines[i];
                        while(reader.hasNextLine()) {
                            String line = reader.nextLine();
                            if (line.equals(form)) {
                                break;
                            }
                        }
                        writer1.write("\n");
                        writer1.write(form);
                    }
                    else {
                        /*while(reader.hasNextLine()) {
                            String line = reader.nextLine();
                            if (line.equals(lines[i])) {
                                break;
                            }
                        }
                        writer2.write("\n");
                        writer2.write(lines[i]);*/
                    }
                }
                //temp.renameTo(log);
                writer1.close();
                reader.close();
                //writer2.close();
                reader2.close();
                if (join_counter == 3) {
                    join_counter = 0;
                    File counter = new File(tcp_ip + "/counter.txt");
                    FileWriter writer3 = new FileWriter(counter, false);
                    writer3.write("0");
                    writer3.close();
                }

                return "Success";
            } catch (IOException e) {
                e.printStackTrace();
                return "Fail";
            }
        }

        else {
            return "Fail";
        }
    }

    private String getFile(String key) {
        String value = "";
        try {
            File file = new File(tcp_ip + "/" + key + ".txt");
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                value += reader.nextLine();
                value += "\n";
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "fail";
        }

        return value;
    }
}
