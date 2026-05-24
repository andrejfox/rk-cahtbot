import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Scanner;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class ChatClient extends Thread
{
	protected int serverPort = 1234;

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public ChatClient() throws Exception {
        SSLSocket socket = null;
        DataInputStream in = null;
        DataOutputStream out = null;
        String username = "";

        System.out.print("Choose user (andrej/miha/joze): ");
        Scanner sc = new Scanner(System.in);
        String selectedUser = sc.next();

        System.setProperty(
                "javax.net.ssl.keyStore",
                "src/main/resources/certs/" +
                        selectedUser +
                        "-keystore.p12"
        );

        System.setProperty(
                "javax.net.ssl.keyStorePassword",
                "password"
        );

        System.setProperty(
                "javax.net.ssl.trustStore",
                "src/main/resources/certs/" +
                        selectedUser +
                        "-truststore.p12"
        );

        System.setProperty(
                "javax.net.ssl.trustStorePassword",
                "password"
        );

        // connect to the chat server
        try {

            System.out.println("[system] connecting to chat server ...");

            SSLSocketFactory factory =
                    (SSLSocketFactory)
                            SSLSocketFactory.getDefault();

            socket =
                    (SSLSocket)
                            factory.createSocket(
                                    "localhost",
                                    serverPort
                            );


            socket.setEnabledProtocols(
                    new String[]{"TLSv1.2"}
            );

            socket.setEnabledCipherSuites(
                    new String[]{
                            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
                    }
            );

            socket.startHandshake();

            SSLSession session =
                    socket.getSession();

            X509Certificate cert =
                    (X509Certificate)
                            session.getLocalCertificates()[0];

            String dn =
                    cert.getSubjectX500Principal().getName();

            username =
                    dn.split("=")[1];

            in = new DataInputStream(
                    socket.getInputStream()
            );

            out = new DataOutputStream(
                    socket.getOutputStream()
            );

            System.out.println(
                    "[system] connected as " + username
            );

            ChatClientMessageReceiver message_receiver =
                    new ChatClientMessageReceiver(in);

            message_receiver.start();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        // read from STDIN and send messages to the chat server
        BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        while ((userInput = std_in.readLine()) != null) { // read a line from the console
            Message.Type type;
            String[] input = userInput.split(" ", 2);

            type = switch (input[0]) {
                case "public", "pu" -> Message.Type.Public;
                case "private", "pr" -> Message.Type.Private;
                case "err", "e" -> Message.Type.Error;
                default -> {
                    System.out.print("Invalid type!\n>");
                    yield null;
                }
            };

        if (type == null) continue;

        Message message;

            switch (type) {

                case Private -> {
                    try {
                        input = input[1].split(" ", 2);

                        message = new Message(
                                type,
                                username,
                                input[0],
                                input[1]
                        );

                    } catch (ArrayIndexOutOfBoundsException e) {

                        message = new Message(
                                type,
                                username,
                                input[0],
                                ""
                        );
                    }
                }

                case Public, Error -> {
                    try {

                        message = new Message(
                                type,
                                username,
                                null,
                                input[1]
                        );

                    } catch (ArrayIndexOutOfBoundsException e) {

                        message = new Message(
                                type,
                                username,
                                null,
                                ""
                        );
                    }
                }

                default -> {
                    System.out.print("invalid message type!\n>");
                    continue;
                }
            }

            this.sendMessage(message, out); // send the message to the chat server
    }

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
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
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
                Message ms = Message.fromJson(message);
				System.out.printf("[RKchat] <%s> [%s]: %s\n>", ms.type, ms.sender, ms.body); // print the message to the console
			}
		} catch (EOFException e) {
            System.err.println("[system] Server disconnected");
            System.exit(1);
        } catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
