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

    public Client(Socket socket) {
        this.serverAddress = "127.0.0.1";
        this.serverPort = 5000;
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Couldn't construct the client, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        } catch (IllegalArgumentException ipAddressException) {
            System.out.println("The ip address provided doesn't respect the range limit, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void writeMessageToSocket(String msg) {
        try {
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }

    }

    public void sendAuthenticationInfos() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter username: ");
        username = scanner.nextLine();
        writeMessageToSocket(username);
        System.out.println("Enter password: ");
        writeMessageToSocket(scanner.nextLine());
    }

    public void sendMessage() {
        Scanner scanner = new Scanner(System.in);
        while (socket.isConnected()) {
            String messageToSend = scanner.nextLine();
            writeMessageToSocket(username + ": " + messageToSend);
        }
    }

    @Override
    public void run() {
        String msgFromGroupChat;

        while (socket.isConnected()) {
            try {
                msgFromGroupChat = bufferedReader.readLine();
                if(msgFromGroupChat == null) throw new IOException();
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
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter the server address (127.0.0.1) and the server port (5000) (ex: 127.0.0.1 5000): ");
//        String serverAddress = scanner.nextLine();

        Socket socket = new Socket("127.0.0.1", 5000);
        Client client = new Client(socket);

        client.sendAuthenticationInfos();

        Thread thread = new Thread(client);
        thread.start();
        client.sendMessage();
    }
}
