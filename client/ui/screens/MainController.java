package client.ui.screens;

import client.Context;
import client.ui.AbstractScreen;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by Mihail Chilyashev on 12/8/15.
 * All rights reserved, unless otherwise noted.
 */
public class MainController extends AbstractScreen {

    Context context;

    // FXML controls
    @FXML
    public TextArea messageText;
    @FXML
    public ListView<String> friendsList;
    @FXML
    public ListView<String> chatOutputList;
    // eo FXML controls

    private boolean serverRunning = false;

    private Socket socketConnection;
    private BufferedReader input = null;
    private BufferedWriter output = null;

    int id;
    String sessionId;

    String host = "localhost";
    int port = 8008;


    @Override
    public void init() {
        this.id = (int) Context.getInstance().get("id");
        this.sessionId = (String) Context.getInstance().get("sessionId");

        context = Context.getInstance();
        try {
            socketConnection = new Socket();
            socketConnection.setReuseAddress(true);
            socketConnection.connect(new InetSocketAddress(host, port));
            System.out.println("Connected to " + host
                    + " at port " + port);

            if (socketConnection != null && !socketConnection.isClosed()) {
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                ConversationHandlerThread thr = new ConversationHandlerThread();
                thr.start();
/*
                Scanner s = new Scanner(System.in);

                while (s.hasNext()) {
                    String msg = s.nextLine();
                    SendThread st = new SendThread(host, port);
                    st.start();
                    st.send(msg);
                    st.join();
                }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {

    }

    public void sendMessageAction(ActionEvent actionEvent) {
        SendThread st = new SendThread(host, port);
        st.start();
        st.send(messageText.getText());
        try {
            st.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public MainController() {
    }

    class ConversationHandlerThread extends Thread {
        private Socket socketConnection;
        private BufferedReader input = null;
        private BufferedWriter output = null;

        @Override
        public void run() {

            if (socketConnection != null && socketConnection.isConnected()) {
                System.err.println("ok.");
            }
            /*
             * Регистриране на клиента в сървъра
             */
            try {
                System.err.println("new.");
                socketConnection = new Socket();
                socketConnection.setReuseAddress(true);
                socketConnection.connect(new InetSocketAddress(host, port));
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                if (input != null) {
                    System.err.println("Session.");
                    output.write(sessionId);
                    output.newLine();
                    output.flush();
                    System.err.println("Read: " + input.readLine());
                    System.err.println("Sending register message");
                    output.write("6;" + id);
                    output.newLine();
                    output.flush();
                    System.err.println("Read: " + input.readLine());
                    String line;
                    while ((line = input.readLine()) != null) {
                        /*if (line.equals("end")) {
                            break;
                        }*/
                        System.out.println("Received: " + line);

                        //friendsList.getItems().add(line);
                    }
                    System.err.println("Join");
                    /*// Първо сесията
                    output.write(sessionId);
                    output.newLine();
                    output.flush();
                    // После приятели
                    output.write("4;" + id);
                    output.newLine();
                    output.flush();

                    input.readLine(); // Прочитаме сесията
                    line = input.readLine();
                    if (line.equals("start_friends")) {
                        // YEAH!
                        System.out.println("Приятели: ");
                        while ((line = input.readLine()) != null) {
                            if (line.equals("end_friends")) {
                                break;
                            }
                            System.out.println(line);
                            //friendsList.getItems().add(line);
                        }
                    }*/
                }
//                join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class SendThread extends Thread {
        private final String host;
        String msg;
        private final int port;
        private boolean send = false;

        public SendThread(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socketConnection = new Socket();
                socketConnection.setReuseAddress(true);
                socketConnection.connect(new InetSocketAddress(host, port));
                System.out.println("Connected to " + host
                        + " at port " + port);

                if (socketConnection != null && !socketConnection.isClosed()) {
                    input = new BufferedReader(new InputStreamReader(
                            socketConnection.getInputStream()));
                    output = new BufferedWriter(new OutputStreamWriter(
                            socketConnection.getOutputStream()));
                }
                System.err.println("?????");
                while (!send) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                System.err.println("!!!!");
                sendSession();
                output.write(msg);
                output.newLine();
                output.flush();
                String line;
                send = false;
                while ((line = input.readLine()) != null) {
/*
                    if (line.equals("end_friends")) {
                        break;
                    }
*/
                    System.out.println(line);
                    //friendsList.getItems().add(line);
                }
                System.err.println("End.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private synchronized void sendSession() throws IOException {
            output.write(sessionId);
            output.newLine();
            output.flush();
            String sess = input.readLine();
            System.err.printf("Sent:%s, Got: %s\n", sessionId, sess);
        }

        public synchronized void send(String msg) {
            this.msg = msg;
            send = true;
            notifyAll();
        }
    }

}
