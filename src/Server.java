import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server
{
    private static ServerSocket listener;

    public Server(ServerSocket serverSocket) {
        listener = serverSocket;
    }

    /*
     * Application Serveur
     */
    public static void main(String[] args) throws Exception
    {
        // Demande le port et l'address IP
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat: ");
        String username = scanner.nextLine();

        // Compteur incremente a chaque connexion d'un client au serveur
        int clientNumber = 0;

        // Adresse et port du serveur
        String serverAddress = "127.0.0.1";
        int serverPort = 5000;

        // Creation de la connexion pour communiquer avec les clients
        listener = new ServerSocket();
        listener.setReuseAddress(true);
        InetAddress serverIP = InetAddress.getByName(serverAddress);

        // Association de l'adresse et du port a la connexion
        listener.bind(new InetSocketAddress(serverIP, serverPort));

        System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);

        try
        {
            /*
             * A chaque fois qu'un nouveau client se connecte, on execute la fonction
             * Run() de l'objet ClientHandler.
             */
            while (true)
            {
                // Important : la fonction accept() est bloquante : attend qu'un prochain client se connecte
                // Une nouvelle connection : on incremente le compteur clientNumber
                new ClientHandler(listener.accept(), clientNumber++).start();
            }
        }
        finally
        {
            // Fermeture de la connexion
            listener.close();
        }
    }

    /*
     * Un thread qui se charge de traiter la demande de chaque client
     * sur un socket particulier
     */
    private static class ClientHandler extends Thread
    {
        private Socket socket;
        private int clientNumber;

        public ClientHandler(Socket socket, int clientNumber)
        {
            this.socket = socket;
            this.clientNumber = clientNumber;
            System.out.println("New connection with client#" + clientNumber + " at " + socket);
        }

        /*
         * Un thread qui se charge d'envoyer au client un message de bienvenue
         */
        public void run()
        {
            try
            {
                // Creation d'un canal sortant pour envoyer des messages au client
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                // Envoie d'un message au client
                out.writeUTF("Hello from server - you are client#" + clientNumber);

            } catch (IOException e)
            {
                System.out.println("Error handling client#" + clientNumber + ": " + e);
            }
            finally
            {
                try
                {
                    // Fermeture de la connexion avec le client
                    socket.close();
                }
                catch (IOException e)
                {
                    System.out.println("Couldn't close a socket, what's going on?");
                }
                System.out.println("Connection with client#" + clientNumber + " closed");
            }
        }
    }
}
