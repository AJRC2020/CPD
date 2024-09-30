package g07.assign2.src;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TestClient {
    public static void main(String[] args) {
        if (args.length < 2)
            return;

        String message;

        switch (args[2]) {
            case "put":
                message = put(args[3]);
                if (message.equals("put ")) {
                    System.out.println("Error reading file");
                    return;
                } 
                break;
            case "get":
                message = get(args[3]);
                break;
            case "delete":
                message = delete(args[3]);
                break;
            case "join":
                message = join();
                break;
            case "leave":
                message = leave();
                break;
            default:
                System.out.println("Operation does not exists");
                return;
        }

        System.out.println("message = " + message);

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(hostname, port)) {

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

    //apenas para perceber argumentos e mandar menssages
    private static String put(String file)  {
        //ficheiro manda o conteudo(value)
        String value = "";
        try {
            File myFile = new File(file);
            Scanner reader = new Scanner(myFile);
            while (reader.hasNextLine()) {
                value += reader.nextLine();
                value += "\n";
            }
            reader.close();
        } 
        catch (FileNotFoundException e){
            System.out.println("File not found");
            e.printStackTrace();
        } 

        String key;
        try {
            key = calculateKey(value);
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "Fail while creating key";
        }

        return "p " + key + " " + value;
    }
    private static String get(String key){
        return "g " + key;
    } //mesma coisa em store
    private static String delete(String key) {
        return "d " + key;
    }
    private static String join() {
        return "j";
    }
    private static String leave() {
        return "l";
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
