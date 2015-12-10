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


    String host = "localhost";
    int port = 8008;


    public void loginAction(ActionEvent actionEvent) {
        errorLabel.setText("");
        if(busy){
            return;
        }

        busy = true;

        try {
            // Взимане на адреса на сървъра
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
            socketConnection.connect(new InetSocketAddress(host, port)); // Свързване
            System.out.println("Connected to " + host
                    + "at port " + port);

            if (socketConnection != null && !socketConnection.isClosed()) {
                System.out.println("Sending login...");
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                LoginHandlerThread thr = new LoginHandlerThread();
                thr.start(); // Стартиране на нишката, която се занимава с логването
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

    /**
     * Нишка, която се занимава с логването
     */
    class LoginHandlerThread extends Thread {

        @Override
        public void run() {
            if (socketConnection != null && socketConnection.isConnected()) {
                System.err.println("[LoginHandlerThread] ok.");
            }

            try {
                if (input != null) {
                    String line;
                    output.write("yo."); // Изпраща невалидна сесия, за да се създаде нова в сървъра
                    output.newLine();
                    output.flush();
                    line = input.readLine(); // Прочита новата сесия
                    sessionId = line.split(":")[1]; // Формат: OK:session
                    System.err.println("Сесия:" + line);

                    output.write("5;"); // Изпраща съобщение от тип LOGIN
                    output.newLine();
                    output.flush();
//                    System.out.print("Име: ");
//                    output.write(r.nextLine());
                    output.write(usernameField.getText()); // Изпраща потребителското име
                    output.newLine();
                    output.flush();
//                    System.out.print("Парола: ");
                    output.write(passwordField.getText()); // Изпраща паролата
                    output.newLine();
                    output.flush();

                    if ((line = input.readLine()) != null && line.startsWith("OK")) { // Ако е минало успешно
                        System.err.println("Line: " + line);
                        String[] split = line.split(":"); // Разделя съобщението на части
                        for (String s : split) {
                            System.err.println("Server: " + s);
                        }
                        id = Integer.parseInt(split[1]); // Взема id-то на логнатия потребител
                        String name = split[2]; // Името на логнатия потребител

                        // Нужните данни се запазват в контекста на приложението
                        Context.getInstance().set("id", id);
                        Context.getInstance().set("sessionId", sessionId); // id на сесията. Нужно е за всяко следващо обръщение към сървъра
                        Context.getInstance().set("name", name); // Името на потребителя

                        Context.getInstance().set("host", host); // Хост
                        Context.getInstance().set("port", port); // Порт

                        busy = false;
                        parent.showScreen("main"); // Показване на главния екран на клиента
                        join(); // Приключване на нишката
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
