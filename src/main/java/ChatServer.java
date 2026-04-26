import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.util.*;

public class ChatServer {

	protected int serverPort = 1234;
	protected HashMap<Socket, String> clients = new HashMap<>(); // list of clients

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				synchronized(this) {
					clients.put(newClientSocket, ""); // add client to the list of clients
				}
				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket, clients); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(Message message) throws Exception {
		Iterator<Socket> i = clients.keySet().iterator();
		while (i.hasNext()) { // iterate through the client list
			Socket socket = (Socket) i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
                sendMessage(message, out);
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}



	public void removeClient(Socket socket) {
		synchronized(this) {
			clients.remove(socket);
		}
	}

    private void sendMessage(Message message, DataOutputStream out) {
        try {
            out.writeUTF(message.toJson()); // send the message to the chat server
            out.flush(); // ensure the message has been sent
        } catch (IOException e) {
            System.err.println("[system] could not send message");
            e.printStackTrace(System.err);
        }
    }

    // send a message to all clients connected to the server
    public void sendToClients(List<Socket> soc, Message message) throws Exception {
        Iterator<Socket> i = soc.iterator();
        while (i.hasNext()) { // iterate through the client list
            Socket socket = (Socket) i.next(); // get the socket for communicating with this client
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client
                sendMessage(message, out);
            } catch (Exception e) {
                System.err.println("[system] could not send message to a client");
                e.printStackTrace(System.err);
            }
        }
    }
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
    private HashMap<Socket, String> clients;

	public ChatServerConnector(ChatServer server, Socket socket, HashMap<Socket, String> clients) {
		this.server = server;
		this.socket = socket;
        this.clients = clients;
	}

	public void run() {
		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

        try {
            System.out.println("[system] connected [" + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort() + "] ->  [" + socket.getInetAddress().getHostName() + ":" + socket.getLocalPort() + "] ");
            System.out.println(clients.toString());
        } catch (Exception e) {
            System.err.println("[system] failed to read username");
            this.server.removeClient(socket);
            return;
        }

		while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client
			} catch (EOFException e) {
                System.out.println("Client [" + this.socket.getPort() + "] " + clients.get(socket) + " Disconnected");
                this.server.removeClient(this.socket);
                return;
            } catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

			if (msg_received.isEmpty()) // invalid message
				continue;

			Message message;
            try {
				message = Message.fromJson(msg_received);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            switch (message.type) {
                case SenderName: {
                    clients.put(socket, message.sender);
                    System.out.println("[RKchat] (n) [" + clients.get(socket) + ":" + socket.getLocalPort() + "] set their name to: " + clients.get(socket)); // print the incoming message in the console

                    try {
                        this.server.sendToAllClients(message); // send message to all clients
                    } catch (Exception e) {
                        System.err.println("[system] there was a problem while sending the message to all clients");
                        e.printStackTrace(System.err);
                        continue;
                    }
                    break;
                }
                case Public: {
                    System.out.println("[RKchat] (pu) [" + clients.get(socket) + ":" + socket.getLocalPort() + "]: " + msg_received); // print the incoming message in the console

                    try {
                        this.server.sendToAllClients(message); // send message to all clients
                    } catch (Exception e) {
                        System.err.println("[system] there was a problem while sending the message to all clients");
                        e.printStackTrace(System.err);
                        continue;
                    }
                    break;
                }
                case Private: {
                    System.out.println("[RKchat] (pr) [" + clients.get(socket) + ":" + socket.getLocalPort() + "]: " + msg_received); // print the incoming message in the console


                    String targetValue = message.recipient;
                    List<Socket> result = new ArrayList<>();
                    for (Map.Entry<Socket, String> entry : clients.entrySet()) {
                        if (entry.getValue().equals(targetValue)) {
                            result.add(entry.getKey());
                        }
                    }

                    if (result.isEmpty()) {
                        result.add(socket);
                        try {
                            Message ms = new Message(Message.Type.Error, "Server", "", "No user with name: " + message.recipient);
                            this.server.sendToClients(result, ms);
                        } catch (Exception e) {
                            System.err.println("[system] there was a problem while sending the message to all clients");
                            e.printStackTrace(System.err);
                            continue;
                        }
                        break;
                    }

                    try {
                        this.server.sendToClients(result, message); // send message to all clients
                    } catch (Exception e) {
                        System.err.println("[system] there was a problem while sending the message to all clients");
                        e.printStackTrace(System.err);
                        continue;
                    }
                    break;
                }
            }
		}
	}
}
