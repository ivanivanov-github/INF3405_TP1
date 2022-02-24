import CustomExceptions.InvalidPasswordException;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable
{
    private Socket socket;
    private static String serverAddress;
    private static Integer serverPort;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Couldn't construct the client, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void writeMessageToSocket(String msg) {
        try {
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public static boolean isValidIPAddress(String[] addressBytes) {
        try {
            int addressByte = Integer.parseInt(addressBytes[0]);
            if (addressByte > 255 || addressByte < 1) return false;
            for (int i = 1; i < addressBytes.length; i++) {
                addressByte = Integer.parseInt(addressBytes[i]);
                if (addressByte >  255 || addressByte < 0) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            System.out.println("L'adresse IP doit etre du format Y.X.X.X (Y et X sur 8 bits) avec Y : 1-255 et X : 0-255");
            return false;
        }
    }

    public static boolean isValidPortNumber(Integer portInput) {
        try {
            return portInput <= 5050 && portInput >= 5000;
        } catch (NumberFormatException e) {
            System.out.println("Le port doit contenir seulement des chiffres");
            return false;
        }
    }

    public static void ipAddressInputHandler(Scanner scanner) {
        String[] addressBytes = {"256", "256", "256", "256"};
        while(!isValidIPAddress(addressBytes) || addressBytes.length != 4) {
            System.out.println("Entrez l'adresse IP du serveur: ");
            serverAddress = scanner.nextLine();
            addressBytes = serverAddress.split("\\.", 0);
            if (!isValidIPAddress(addressBytes) || addressBytes.length != 4){
                System.out.print("\nL'adresse entree est invalide \n\n");
            }
        }
    }

    public static void portInputHandler(Scanner scanner) {
        serverPort = 0;
        try {
            while(!isValidPortNumber(serverPort)) {
                System.out.print("Entrez le port d'ecoute du serveur: \n");
                serverPort = Integer.parseInt(scanner.nextLine());
                if(!isValidPortNumber(serverPort)) {
                    System.out.print("\nLe port doit etre compris entre 5000 et 5050 \n\n");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Le port doit etre un nombre entre 5000 et 5050 \n\n");
            portInputHandler(new Scanner(System.in));
        }
    }

    public static void serverInfosInputHandler() {
        try {
            Scanner scanner = new Scanner(System.in);
            ipAddressInputHandler(scanner);
            portInputHandler(scanner);
            System.out.println("SVP veuillez attendre pendant qu'on essaie de vous connecter a un serveur avec les informations que vous avez rentre...");
            TimeUnit.MILLISECONDS.sleep(300);
        }  catch (InterruptedException e) {
            System.out.println("Something went wrong");
        }
    }

    public void usernameInputHandler(Scanner scanner) {
        System.out.println("Entrez votre nom d'utilisateur: ");
        username = scanner.nextLine();
        writeMessageToSocket(username);
    }

    public void passwordInputHandler(Scanner scanner) {
        System.out.println("Entrez votre mot de passe: ");
        writeMessageToSocket(scanner.nextLine());
    }

    public void sendAuthenticationInfos() {
        Scanner scanner = new Scanner(System.in);
        usernameInputHandler(scanner);
        passwordInputHandler(scanner);
    }

    public boolean socketIsConnected() {
        try {
            socket.getInputStream();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isUnder200Char(String msg) {
        return msg.length() < 200;
    }

    public void sendMessage() {
        Scanner scanner = new Scanner(System.in);
        while (socketIsConnected()) {
            String messageToSend = scanner.nextLine();
            if (isUnder200Char(messageToSend)) {
                writeMessageToSocket(" " + messageToSend);
            } else {
                System.out.println("Votre message depasse 200 characteres!");
            }
        }
        scanner.close();
    }

    @Override
    public void run() {
        String msgFromGroupChat;
        while (socketIsConnected()) {
            try {
                msgFromGroupChat = bufferedReader.readLine();
                if(msgFromGroupChat == null) throw new IOException();
                if (msgFromGroupChat.equals("Erreur dans la saisie du mot de passe")) throw new InvalidPasswordException();
                System.out.println(msgFromGroupChat);
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            } catch (InvalidPasswordException e) {
                System.out.println("Erreur dans la saisie du mot de passe \n");
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        System.out.println("Fermeture de votre connection avec le serveur... \nVeuillez recommencer votre session");
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
        serverInfosInputHandler();

        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(serverAddress, serverPort);
            socket.connect(socketAddress, 5000);
            Client client = new Client(socket);
            client.sendAuthenticationInfos();

            Thread thread = new Thread(client);
            thread.start();
            client.sendMessage();
        } catch (SocketTimeoutException e) {
            System.out.println("\nAucun serveur n'ecoute sur l'adresse IP et port donnees \nVeuillez rentrer l'adresse IP et le port d'un serveur qui est en marche");
        }
    }
}
