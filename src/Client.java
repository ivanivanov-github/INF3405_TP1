import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable
{
    private Socket socket;
    private String serverAddress;
    private int serverPort;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;
    private boolean clientConstructedProperly;

    public Client(Socket socket, String username, String password) {
        this.serverAddress = "127.0.0.1";
        this.serverPort = 5000;
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.clientConstructedProperly = true;
        } catch (IOException e) {
            this.clientConstructedProperly = false;
            System.out.println("Couldn't construct the client, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (IllegalArgumentException ipAddressException) {
            this.clientConstructedProperly = false;
            System.out.println("The ip address provided doesn't respect the range limit, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMessage() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                bufferedWriter.write(username + ": " + messageToSend);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String msgFromGroupChat;

        while (socket.isConnected()) {
            try {
                msgFromGroupChat = bufferedReader.readLine();
                System.out.println(msgFromGroupChat);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat: ");
        String username = scanner.nextLine();

//        System.out.println("Enter your password for the group chat: ");
//        String password = scanner.nextLine();
        String password = "aa";
//        System.out.println("Enter the server address (127.0.0.1): ");
//        String serverAddress = scanner.nextLine();

//        System.out.println("Enter the server port (5000): ");
//        String serverPort = scanner.nextLine();

//        Socket socket = new Socket(serverAddress, Integer.parseInt(serverPort));
        Socket socket = new Socket("127.0.0.1", 5000);
        Client client = new Client(socket, username, password);
//        client.listenForMessage();
        Thread thread = new Thread(client);
        thread.start();
        client.sendMessage();
    }
}
