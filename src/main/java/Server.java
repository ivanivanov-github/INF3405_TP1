import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import CustomExceptions.UserIsAlreadyConnectedException;
import CustomExceptions.UserNotInDataBaseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Server {
    private ServerSocket listener;
    private int clientNumber;
    private String serverAddress;
    private int serverPort;
    public FileWriter writer;
    public FileReader reader;
    public JSONParser parser;
    public JSONObject dataJsonObj;
    public JSONArray users;

    public Server(ServerSocket serverSocket) {
        parser = new JSONParser();
        this.serverAddress = "127.0.0.1";
        this.serverPort = 5000;
        try {
            this.listener = serverSocket;
            writer = new FileWriter("data.json", true);
            reader = new FileReader("data.json");
            dataJsonObj = (JSONObject) parser.parse(reader);
            users = (JSONArray) dataJsonObj.get("Users");
//            this.listener.setReuseAddress(true);
//            InetAddress serverIP = InetAddress.getByName(serverAddress);
//            this.listener.bind(new InetSocketAddress(serverIP, serverPort));
        } catch (IOException | org.json.simple.parser.ParseException e) {
//            this.serverConstructedProperly = false;
//            System.out.println("Couldn't construct the server, what's going on?");
//            e.printStackTrace();
        }
    }

    public void startServer() {
        try {
            while (!listener.isClosed()) {
                Socket socket = listener.accept();
                System.out.println("A new client has connected!");
                ClientHandler clientHandler = new ClientHandler(socket, clientNumber++);

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
//            try {
//                listener.close();
//            } catch (IOException serverSocketException) {
//                System.out.println("Couldn't close the ServerSocket, what's going on?");
//                serverSocketException.printStackTrace();
//            }
        }
    }

    public boolean verifyAuthentication(String username, String password) throws UserNotInDataBaseException, UserIsAlreadyConnectedException {
        Iterator<JSONObject> iterator = users.iterator();
        while (iterator.hasNext()) {
            JSONObject jsonUser = (JSONObject) iterator.next();
            if (jsonUser.get("Username").equals(username) && jsonUser.get("Password").equals(password)) {
                if (jsonUser.get("isConnected").equals(false)) {
                    jsonUser.put("isConnected", "true");
                    return true;
                }
                else throw new UserIsAlreadyConnectedException();
            }
            else if (jsonUser.get("Username").equals(username) && !jsonUser.get("Password").equals(password)) return false;
        } throw new UserNotInDataBaseException();
    }

    public void addUserToDB(String username, String password) {
        JSONObject obj = new JSONObject();
        obj.put("Username", username);
        obj.put("Password", password);
        obj.put("isConnected", "true");
        StringWriter out = new StringWriter();
        try {
            obj.writeJSONString(out);
            String jsonText = out.toString();
            users.add(obj);
            dataJsonObj.put("Users", users);
            FileWriter newFileWriter = new FileWriter("data.json", false);
            newFileWriter.write(dataJsonObj.toJSONString());
            newFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServerSocket() {
        try {
            if (listener != null) {
                listener.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        Server server = new Server(serverSocket);
        System.out.format("The server is running on %s: %d%n", server.serverAddress, server.serverPort);

        server.startServer();
    }

    private class ClientHandler implements Runnable {
        public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
        private Socket socket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String clientUsername;
        private String clientPassword;
        private String clientIPAddress;
        private int clientPortNumber;
        private int clientNumber;


        public ClientHandler(Socket socket, int clientNumber) {
            try {
                this.socket = socket;
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.clientNumber = clientNumber;
                this.clientUsername = bufferedReader.readLine();
                System.out.println("Username " + this.clientUsername);
                this.clientPassword = bufferedReader.readLine();
                System.out.println("Password " + this.clientPassword);
                if (verifyAuthentication(clientUsername, clientPassword)) {
                    clientHandlers.add(this);
                    System.out.println("user in database");
                    broadcastMessage("Server: " + clientUsername + " has entered the chat!");
                } else {
                    bufferedWriter.write("Erreur dans la saisie du mot de passe");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    System.out.println("wrong password");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
//                JSONObject obj = new JSONObject();
//                obj.put("Username", clientUsername);
//                obj.put("Password", clientPassword);
//                writer.write(obj.toJSONString());
//                writer.close();
//                this.clientIPAddress = bufferedReader.readLine();
//                System.out.println("IPAddress" + this.clientIPAddress);
//                this.clientPortNumber = Integer.parseInt(bufferedReader.readLine());

            } catch (IOException e) {
                System.out.println("Exception thrown, closing everything");
                closeEverything(socket, bufferedReader, bufferedWriter);
            } catch (NumberFormatException numberFormatException) {
                System.out.println("Client" + clientUsername + " had a prob with broadcasting his message");
            } catch (UserNotInDataBaseException e) {
                System.out.println("Exception user not in data");
                addUserToDB(clientUsername, clientPassword);
            } catch (UserIsAlreadyConnectedException e) {
                System.out.println("User is already connected with this account");
                try {
                    bufferedWriter.write("Un client est deja connecte avec ce compte");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    closeEverything(socket, bufferedReader, bufferedWriter);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        }

        @Override
        public void run() {
            String messageFromClient;

            while (socket.isConnected()) {
                try {
                    messageFromClient = bufferedReader.readLine();
                    broadcastMessage(messageFromClient);
                } catch (IOException e) {
                    System.out.println("Error handling client#" + clientNumber + ": " + e);
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
//                } finally {
//                    closeEverything(socket, bufferedReader, bufferedWriter);
//                    System.out.println("Connection with client#" + clientNumber + " closed");
//                    break;
//                }
            }
        }

        public void broadcastMessage(String messageToSend) {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (!clientHandler.clientUsername.equals(clientUsername)) {
                        clientHandler.bufferedWriter.write(messageToSend);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Client" + clientNumber + " had a prob with broadcasting his message");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }

        public void removeClientHandler() {
            clientHandlers.remove(this);
            broadcastMessage("Server: " + clientUsername + " " + clientNumber + " has left the chat!");
        }

        public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
            removeClientHandler();
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
                System.out.println("Couldn't close a socket, or a buffered reader/writer, what's going on?");
                e.printStackTrace();
            }
        }
    }
}
