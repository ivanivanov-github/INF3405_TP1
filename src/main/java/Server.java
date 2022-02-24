import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import CustomExceptions.UserIsAlreadyConnectedException;
import CustomExceptions.UserNotInDataBaseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Server {
    private ServerSocket listener;
    private String serverAddress;
    private int serverPort;
    public FileWriter writerUsersFile;
    public FileReader readerUsersFile;
    public FileWriter writerMessagesFile;
    public FileReader readerMessagesFile;
    public JSONParser parser;
    public JSONObject usersJsonObj;
    public JSONArray users;
    public JSONObject messagesDataJsonObj;
    public JSONArray messages;
    public FileReader newReader;
    public File usersDBFile;
    public File messagesDBFile;

    public Server() {
        this.configureServerSocket();
        parser = new JSONParser();
        usersDBFile = new File("usersDB.json");
        messagesDBFile = new File("messagesDB.json");
        if (!usersDBFile.isFile()) {
            this.createUsersDB();
        }
        if (!messagesDBFile.isFile()) {
            this.createMessagesDB();
        }
        this.initializeDBReaders();
    }

    public void startServer() {
        try {
            while (!listener.isClosed()) {
                Socket socket = listener.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                if (clientHandler.constructedCorrectly) {
                    Thread thread = new Thread(clientHandler);
                    thread.start();
                }
            }
            System.out.println("prob in startServer");
        } catch (IOException e) {
            System.out.println("prob in startServer");
            e.printStackTrace();
        } catch (java.nio.channels.IllegalBlockingModeException e) {
            System.out.println("prob in startServer");
        }
    }

    public void initializeDBReaders() {
        try {
            readerUsersFile = new FileReader(usersDBFile);
            readerMessagesFile = new FileReader(messagesDBFile);
            usersJsonObj = (JSONObject) parser.parse(readerUsersFile);
            users = (JSONArray) usersJsonObj.get("Users");
            messagesDataJsonObj = (JSONObject) parser.parse(readerMessagesFile);
            messages = (JSONArray) messagesDataJsonObj.get("Messages");
        } catch (IOException e) {
            System.out.println("Couldn't initialize the database readers");
            e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println("Couldn't parse the json files");
            e.printStackTrace();
        }
    }

    public void configureServerSocket() {
        this.serverAddress = "127.0.0.1";
        this.serverPort = 5000;
        try {
            this.listener = new ServerSocket();
            this.listener.setReuseAddress(true);
            InetAddress serverIP = InetAddress.getByName(serverAddress);
            this.listener.bind(new InetSocketAddress(serverIP, serverPort));
        } catch (IOException e) {
            System.out.println("Couldn't configure the server socket");
            e.printStackTrace();
        }
    }

    public void createUsersDB() {
        try {
            usersDBFile.createNewFile();
            readerUsersFile = new FileReader(usersDBFile);
            writerUsersFile = new FileWriter(usersDBFile, true);
            usersJsonObj = new JSONObject();
            users = new JSONArray();
            usersJsonObj.put("Users", users);
            writerUsersFile.write(usersJsonObj.toJSONString());
            writerUsersFile.flush();
        } catch (IOException e) {
            System.out.println("Could not create json database for messages");
        }
    }

    public void createMessagesDB() {
        try {
            messagesDBFile.createNewFile();
            readerMessagesFile = new FileReader(messagesDBFile);
            writerMessagesFile = new FileWriter(messagesDBFile, true);
            messagesDataJsonObj = new JSONObject();
            messages = new JSONArray();
            addWelcomeMessagesTo(messages);
            messagesDataJsonObj.put("Messages", messages);
            writerMessagesFile.write(messagesDataJsonObj.toJSONString());
            writerMessagesFile.flush();
        } catch (IOException e) {
            System.out.println("Could not create json database for users");
        }
    }

    public void addWelcomeMessagesTo(JSONArray messages) {
        JSONObject welcomeMessage = new JSONObject();
        welcomeMessage.put("message", "Bienvenue au meilleur chat serveur du monde! Entrez vos messages en dessous!");
        messages.add(welcomeMessage);
    }

    public boolean verifyAuthentication(String username, String password) throws UserNotInDataBaseException, UserIsAlreadyConnectedException {
        Iterator<JSONObject> iterator = users.iterator();
        while (iterator.hasNext()) {
            JSONObject jsonUser = (JSONObject) iterator.next();
            if (jsonUser.get("Username").equals(username) && jsonUser.get("Password").equals(password)) {
                if (jsonUser.get("isConnected").equals("false")) {
                    jsonUser.put("isConnected", "true");
                    usersJsonObj.put("Users", users);
                    try {
                        FileWriter newFileWriter = new FileWriter(usersDBFile, false);
                        newFileWriter.write(usersJsonObj.toJSONString());
                        newFileWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
            users.add(obj);
            usersJsonObj.put("Users", users);
            FileWriter newFileWriter = new FileWriter(usersDBFile, false);
            newFileWriter.write(usersJsonObj.toJSONString());
            newFileWriter.close();
        } catch (IOException e) {
            System.out.println("Problem while adding user to data base");
            e.printStackTrace();
        }
    }

    public boolean getUserStatus(String username, String password) {
        JSONObject objData;
        try {
            newReader = new FileReader(usersDBFile);
            objData = (JSONObject) parser.parse(newReader);
            JSONArray usersArray = (JSONArray) objData.get("Users");
            Iterator<JSONObject> iterator = usersArray.iterator();
            while (iterator.hasNext()) {
                JSONObject jsonUser = (JSONObject) iterator.next();
                if (jsonUser.get("Username").equals(username) && jsonUser.get("Password").equals(password)) {
                    return(Boolean.parseBoolean((String)jsonUser.get("isConnected")));
                }
            }
        } catch (IOException | org.json.simple.parser.ParseException e) {
            System.out.println("Problem while getting user connexion status");
            e.printStackTrace();
        }
        return false;
    }

    public void disconnectAllConnectedUsers() {
        JSONObject objData;
        try {
            FileReader newReader = new FileReader(usersDBFile);
            objData = (JSONObject) parser.parse(newReader);
            JSONArray usersArray = (JSONArray) objData.get("Users");
            Iterator<JSONObject> iterator = usersArray.iterator();
            while (iterator.hasNext()) {
                JSONObject jsonUser = (JSONObject) iterator.next();
                if (jsonUser.get("isConnected").equals("true")) {
                    jsonUser.put("isConnected", "false");
                    usersJsonObj.put("Users", usersArray);
                    FileWriter newFileWriter = new FileWriter(usersDBFile, false);
                    newFileWriter.write(usersJsonObj.toJSONString());
                    newFileWriter.close();
                }
            }
        } catch (IOException e) {
            System.out.println("Problem while reading users from database to disconnect all the users");
            e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println("Problem while parsing users from database to disconnect all the users");
            e.printStackTrace();
        }
    }

    public void disconnectUser(String username, String password) {
        JSONObject objData;
        try {
            newReader = new FileReader(usersDBFile);
            objData = (JSONObject) parser.parse(newReader);
            JSONArray usersArray = (JSONArray) objData.get("Users");
            Iterator<JSONObject> iterator = usersArray.iterator();
            while (iterator.hasNext()) {
                JSONObject jsonUser = (JSONObject) iterator.next();
                if (jsonUser.get("Username").equals(username) && jsonUser.get("Password").equals(password)) {
                    if (getUserStatus(username, password)) {
                        jsonUser.put("isConnected", "false");
                        usersJsonObj.put("Users", usersArray);
                        FileWriter newFileWriter = new FileWriter(usersDBFile, false);
                        newFileWriter.write(usersJsonObj.toJSONString());
                        newFileWriter.close();
                    }
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Problem while reading users from database to disconnect the user");
            e.printStackTrace();
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println("Problem while parsing users from database to disconnect the user");
            e.printStackTrace();
        }
    }

    public void shutDownHookDisconnectAllUsers(Server server) {
        Thread disconnectHook = new Thread(() -> server.disconnectAllConnectedUsers());
        Runtime.getRuntime().addShutdownHook(disconnectHook);
    }

    public static void main(String[] args) {
            Server server = new Server();
            System.out.format("The server is running on %s: %d%n", server.serverAddress, server.serverPort);
            server.shutDownHookDisconnectAllUsers(server);
            server.startServer();
            System.out.println("Problem in startServer");
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
        private InetSocketAddress clientSocketAddress;
        private boolean constructedCorrectly;
        private FileReader messagesReader;
        private JSONObject messageJsonObj;
        private JSONArray messages;

        public  ClientHandler(Socket socket) {
            try {
                this.socket = socket;
                initializeConnexionInfos();
                this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.clientUsername = bufferedReader.readLine();
                this.clientPassword = bufferedReader.readLine();
                if (verifyAuthentication(clientUsername, clientPassword)) {
                    addClientToChat();
                } else {
                    handleWrongClientAuthentication();
                }
            } catch (IOException e) {
                System.out.println("Problem while reading new input from client through socket");
                closeEverything(socket, bufferedReader, bufferedWriter);
            } catch (NumberFormatException numberFormatException) {
                System.out.println("Client" + clientUsername + " had a prob with broadcasting his message");
            } catch (UserNotInDataBaseException e) {
                System.out.println("Exception user not in data");
                addUserToDB(clientUsername, clientPassword);
                addClientToChat();
            } catch (UserIsAlreadyConnectedException e) {
                this.constructedCorrectly = false;
                System.out.println("User is already connected with this account");
                e.printStackTrace();
                try {
                    bufferedWriter.write("Un client est deja connecte avec ce compte");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    closeEverythingWithoutRemoving(socket, bufferedReader, bufferedWriter);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public void addClientToChat() {
            this.constructedCorrectly = true;
            clientHandlers.add(this);
            get15LatestMessages();
            broadcastMessage("Serveur: " + clientUsername + " est rentre dans le chat!");
        }

        public void handleWrongClientAuthentication() {
            try {
                this.constructedCorrectly = false;
                bufferedWriter.write("Erreur dans la saisie du mot de passe");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                closeEverythingWithoutRemoving(socket, bufferedReader, bufferedWriter);
            } catch (IOException e) {
                System.out.println("Could send message to client through socket");
                e.printStackTrace();
            }
        }

        public void initializeConnexionInfos() {
            this.clientSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
            this.clientIPAddress = clientSocketAddress.getAddress().getHostAddress();
            this.clientPortNumber = clientSocketAddress.getPort();
        }

        public void get15LatestMessages() {
            try {
                messagesReader = new FileReader(messagesDBFile);
                messageJsonObj = (JSONObject) parser.parse(messagesReader);
                messages = (JSONArray) messageJsonObj.get("Messages");
                List<JSONObject> messagesArrayList = (List<JSONObject>) messageJsonObj.get("Messages");
                List<JSONObject> latest15Messages = messagesArrayList.subList(Math.max(messagesArrayList.size() - 15, 0), messagesArrayList.size());
                Iterator<JSONObject> iterator = latest15Messages.iterator();
                while(iterator.hasNext()) {
                    JSONObject message = (JSONObject) iterator.next();
                    bufferedWriter.write((String) message.get("message"));
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            } catch (java.io.IOException | org.json.simple.parser.ParseException e) {
                System.out.println("Probleme while getting the 15 latest messages");
                e.printStackTrace();
            }
        }

        public void addMessageToDataBase(String messageFromClient) {
            JSONObject newMessage = new JSONObject();
            newMessage.put("message", messageFromClient);
            StringWriter out = new StringWriter();
            try {
                messagesReader = new FileReader(messagesDBFile);
                messageJsonObj = (JSONObject) parser.parse(messagesReader);
                messages = (JSONArray) messageJsonObj.get("Messages");
                newMessage.writeJSONString(out);
                messages.add(newMessage);
                messageJsonObj.put("Messages", messages);
                FileWriter newFileWriter = new FileWriter(messagesDBFile, false);
                newFileWriter.write(messageJsonObj.toJSONString());
                newFileWriter.close();
            } catch (IOException | org.json.simple.parser.ParseException e) {
                System.out.println("Probleme while adding a messages to the data base");
                e.printStackTrace();
            }
        }

        public String formatMessage(String msg) {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy@HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);
            String formattedMessage = "[" + clientUsername + " - " + clientIPAddress + " : " + clientPortNumber + " - " + formattedDate + "]:" + msg;
            return formattedMessage;
        }

        @Override
        public void run() {
            String messageFromClient;

            while (socket.isConnected()) {
                try {
                    messageFromClient = bufferedReader.readLine();
                    String formatedMessage = formatMessage(messageFromClient);
                    addMessageToDataBase(formatedMessage);
                    broadcastMessage(formatedMessage);
                } catch (IOException e) {
                    System.out.println("Error handling client" + clientUsername + ": " + e);
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
            }
        }

        public void broadcastMessage(String messageToSend) {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                        clientHandler.bufferedWriter.write(messageToSend);
                        clientHandler.bufferedWriter.newLine();
                        clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    System.out.println("Client" + clientUsername + " had a prob with broadcasting his message");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }

        public void removeClientHandler() {
            disconnectUser(clientUsername, clientPassword);
            clientHandlers.remove(this);
            broadcastMessage("Serveur: " + clientUsername + " " + clientUsername + " est parti du chat!");
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

        public void closeEverythingWithoutRemoving(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
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
