import CustomExceptions.InvalidPasswordException;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client implements Runnable
{
    private Socket socket;
    private static String serverAddress;
    private static Integer serverPort;
//    private InetAddress clientIPAddress;
    private String clientPort;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    public Client(Socket socket) {
        try {
            this.socket = socket;
//            this.clientIPAddress = InetAddress.getLocalHost();
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Couldn't construct the client, closing everything");
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public void printFormatedMessage(String msg) {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy@HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        String formattedMessage = "[" + username + " - " + serverAddress + " : " + clientPort + " - " + formattedDate + "]: " + msg;
        System.out.println(formattedMessage);
    }

    public void writeFormatedMessageToSocket(String msg) throws IOException {
//        try {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy@HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);
            String formattedMessage = "[" + username + " - " + serverAddress + " : " + clientPort + " - " + formattedDate + "]:" + msg;
            bufferedWriter.write(formattedMessage);
            bufferedWriter.newLine();
            bufferedWriter.flush();
//        } catch (IOException e) {
//            closeEverything(socket, bufferedReader, bufferedWriter);
//        }
    }

    public void writeMessageToSocket(String msg) {
        try {
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.print("\nL'adresse entrée est invalide \n\n");
            }
        }
    }

    public static void portInputHandler(Scanner scanner) {
        serverPort = 0;
        try {
            while(!isValidPortNumber(serverPort)) {
                System.out.print("Entrez le port d'écoute du serveur: \n");
                serverPort = Integer.parseInt(scanner.nextLine());
                if(!isValidPortNumber(serverPort)) {
                    System.out.print("\nLe port doit être compris entre 5000 et 5050 \n\n");
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

    public void sendIPAddressPortInfos() {
        writeMessageToSocket(serverAddress);
        writeMessageToSocket(Integer.toString(serverPort));
    }

    public void usernameInputHandler(Scanner scanner) {
        System.out.println("Enter username: ");
        username = scanner.nextLine();
        writeMessageToSocket(username);
    }

    public void passwordInputHandler(Scanner scanner) {
        System.out.println("Enter password: ");
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
        try {
            Scanner scanner = new Scanner(System.in);
            while (socketIsConnected()) {
//                System.out.print("Envoyer un message : ");
                String messageToSend = scanner.nextLine();
                if (isUnder200Char(messageToSend)) {
                    printFormatedMessage(messageToSend);
                    writeFormatedMessageToSocket(" " + messageToSend);
                } else {
                    System.out.println("Votre message depasse 200 characteres!");
                }
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String msgFromGroupChat;
        // First message sent from server is the client's ip address
        try {
            clientPort = bufferedReader.readLine();
            System.out.println(clientPort);
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
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
            client.sendIPAddressPortInfos();
            client.sendAuthenticationInfos();

            Thread thread = new Thread(client);
            thread.start();
            // Attendre que le serveur envoie les 15 derniers messages avant que le client puissent envoyer des messages
            Thread.sleep(200);
            client.sendMessage();
        } catch (SocketTimeoutException e) {
            System.out.println("\nAucun server ecoute sur l'adresse IP et port donnees \nVeuillez rentrer l'adresse IP et le port d'un serveur qui est en train de rouler");
        } catch (IllegalArgumentException e) {
            System.out.println("Le port doit etre un nombre entre 5000 et 5050 \n\n");
        } catch (InterruptedException e) {
            System.out.println("Probleme dans le thread sleep \n\n");
        }

    }
}
