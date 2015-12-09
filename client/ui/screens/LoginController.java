package client.ui.screens;

import client.Context;
import client.ui.AbstractScreen;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class LoginController extends AbstractScreen {

    // FXML controls
    @FXML
    public TextField serverField;
    @FXML
    public TextField portField;
    @FXML
    public TextField usernameField;
    @FXML
    public TextField passwordField;
    @FXML
    public Button cancelButton;
    @FXML
    public Label errorLabel;
    // eo FXML controls

    Context context;

    private Socket socketConnection;
    private BufferedReader input = null;
    private BufferedWriter output = null;

    public int id = 0;
    public String sessionId;

    boolean busy = false;

    public void loginAction(ActionEvent actionEvent) {
        errorLabel.setText("");
        if(busy){
            return;
        }

        busy = true;
//        public LoginController() {
        String host = "localhost";
        int port = 8008;

        try {
            host = serverField.getText();
            port = Integer.parseInt(portField.getText());
        } catch (Exception e){
            errorLabel.setText("Невалидни данни за връзка.");
            return;
        }

        context = Context.getInstance();
        try {
            if(socketConnection != null && socketConnection.isConnected()){
                try {
                    socketConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            socketConnection = new Socket();
            socketConnection.setReuseAddress(true);
            socketConnection.connect(new InetSocketAddress(host, port));
            System.out.println("Connected to " + host
                    + "at port " + port);

            if (socketConnection != null && !socketConnection.isClosed()) {
                System.out.println("Sending login...");
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                LoginHandlerThread thr = new LoginHandlerThread();
                thr.start();
            }
        } catch (Exception e) {
            errorLabel.setText("Грешка при свързването.");
            busy = false;
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        errorLabel.setText("");
    }

    @Override
    public void close() {

    }


    class LoginHandlerThread extends Thread {

        @Override
        public void run() {
            if (socketConnection != null && socketConnection.isConnected()) {
                System.err.println("ok.");
            }
            /*
             * Read from from input stream one line at a time
             */
            try {
                if (input != null) {
                    String line;
                    output.write("yo.");
                    output.newLine();
                    output.flush();
                    line = input.readLine();
                    sessionId = line.split(":")[1];
                    System.err.println("Сесия:" + line);
                    Scanner r = new Scanner(System.in);
                    output.write("5;");
                    output.newLine();
                    output.flush();
//                    System.out.print("Име: ");
//                    output.write(r.nextLine());
                    output.write(usernameField.getText());
                    output.newLine();
                    output.flush();
//                    System.out.print("Парола: ");
                    output.write(passwordField.getText());
                    output.newLine();
                    output.flush();

                    if ((line = input.readLine()) != null && line.startsWith("OK")) {
                        System.err.println("Line: " + line);
                        //String clientId = line.split(":")[1];
                        String[] split = line.split(":");
                        for (String s : split) {
                            System.err.println("Server: " + s);
                        }
                        id = Integer.parseInt(split[1]);

                        //new MainController(id, sessionId);
                        Context.getInstance().set("id", id);
                        Context.getInstance().set("sessionId", sessionId);

                        busy = false;
                        parent.showScreen("main");
                        join();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
