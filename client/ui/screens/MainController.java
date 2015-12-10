package client.ui.screens;

import client.Context;
import client.ui.AbstractScreen;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

/**
 * Главният екран на клиента
 */
public class MainController extends AbstractScreen {

    Context context;

    // FXML controls
    @FXML
    public TextArea messageText;
    @FXML
    public ListView<Friend> friendsList;
    @FXML
    public ListView<String> chatOutputList;
    // eo FXML controls

    private Socket socketConnection; // Връзка със сървъра
    private BufferedReader input = null; // Четене от сървъра
    private BufferedWriter output = null; // Писане към сървъра

    int id;
    String sessionId; // id на сесията
    String name; // Име на логнатия потребител

    String host = "localhost";
    int port = 8008;

    /**
     * Хеш с всички подпрозорци, използвани за чат с други потребители
     */
    HashMap<String, ChatController> children = new HashMap<>();

    /**
     * Нишка, която се грижи за получаването на чат съобщения
     */
    ConversationHandlerThread conversationHandlerThread;

    public MainController() {

    }


    @Override
    public void init() {
        // Вземат се нужните данни от контекста на приложението
        this.host = (String) Context.getInstance().get("host");
        this.port = ((Integer) Context.getInstance().get("port"));
        this.id = (int) Context.getInstance().get("id");
        this.sessionId = (String) Context.getInstance().get("sessionId");
        this.name = (String) Context.getInstance().get("name");

        context = Context.getInstance();

        // При двойно кликване върху потребител от списъка се създава нов чат прозорец, ако вече няма такъв.
        friendsList.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                Friend friend = friendsList.getSelectionModel().getSelectedItem();
                if (!children.containsKey(friend.id)) {
                    createChatWindow(friend.id, friend.name, "");
                }
            }
        });

        try {
            socketConnection = new Socket();
            socketConnection.setReuseAddress(true);
            socketConnection.connect(new InetSocketAddress(host, port)); // Свързване към сървъра
            System.out.println("Connected to " + host
                    + " at port " + port);

            if (socketConnection != null && !socketConnection.isClosed()) {
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                conversationHandlerThread = new ConversationHandlerThread();
                conversationHandlerThread.start(); // Пускане на нишката, която следи за нови съобщения
                FriendGetterThread frs = new FriendGetterThread(); // Създаване и пускане на нишката, която взема списъка с приятели
                frs.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Създаване на нов чат прозорец
     *
     * @param id   С кого?
     * @param name Как се казва?
     * @param msg  Ако не е празен стринг, новият прозорец се стартира със съобщение
     */
    private void createChatWindow(String id, String name, String msg) {
        Platform.runLater(() -> {
            try {
                System.err.println("Creating a new window...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/screens/client/chat.fxml"));
                Parent loadedScreen = (Parent) loader.load();
                Group root = new Group();
                root.getChildren().addAll(loadedScreen);
                Scene scene = new Scene(root);//, root.getLayoutBounds().getWidth(), root.getLayoutBounds().getHeight());
                Stage stage = new Stage();
                stage.setScene(scene);
//                        stage.minHeightProperty().bind(loadedScreen.heightProperty());
//                        stage.minWidthProperty().bind(mainScreen.widthProperty());
                stage.setTitle("Чат с " + name);
                ChatController controller = loader.getController();
                controller.setId(id);
                controller.setName(this.name);
                controller.setParent(this);
                children.put(id, loader.getController());

                stage.setOnCloseRequest(event -> {
                    children.remove(id);
                });

                stage.show();
                if (msg.length() > 0) {
                    controller.addMessage(name, msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
                System.exit(1);
            }
        });
    }

    @Override
    public void close() {
        try {
            socketConnection.close();
            conversationHandlerThread.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Изпращане на съобщение до потребител
     *
     * @param id  до кого
     * @param msg съобщението
     */
    public synchronized void sendTo(String id, String msg) {
        SendThread st = new SendThread(host, port);
        st.start();
        // Построяване на заявката към сървъра
        st.send(String.format("2;%s;%s", id, msg));
        try {
            st.join(); // Край на нишката
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    /**
     * Нишка, която се използва за получаване на чат и онлайн/офлайн съобщенията от сървъра.
     */
    class ConversationHandlerThread extends Thread {

        private Socket socketConnection;
        private BufferedReader input = null;
        private BufferedWriter output = null;

        @Override
        public void run() {
            /*
             * Регистриране на клиента в сървъра
             */
            try {
                socketConnection = new Socket();
                socketConnection.setReuseAddress(true);
                socketConnection.connect(new InetSocketAddress(host, port)); // Свързва се към сървъра
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                if (input != null) {
                    output.write(sessionId); // Изпраща сесията към сървъра
                    output.newLine();
                    output.flush();
                    System.err.println("Read: " + input.readLine()); // Сървърът връща сесията
                    output.write("6;" + id); // Казва на сървъра, че иска да се регистрира като слушател, т.е. ще получава чат съобщения
                    output.newLine();
                    output.flush();
                    System.err.println("Read: " + input.readLine()); // Прочита отново сесията (сървърът я връща във всеки отговор)
                    String line;
                    while ((line = input.readLine()) != null) { // Почва да чете постоянно
                        System.err.println("Message received: " + line);
                        // Проверка дали връзката все още е жива
                        if (socketConnection.isClosed()) { // Ако не е, спираме да се опитваме да четем
                            break;
                        }
                        // Обработване на съобщенията, че някой е дошъл онлайн или офлайн
                        if (line.startsWith("-")) { // Някой е минал офлайн
                            String[] parts = line.split(";"); // съобщението е от вида "-;id", затова го разделяме на части
                            String whom = parts[1]; // Кой е минал офлайн
                            // Ако има отворен чат с този човек, пишем, че е излязъл офлайн
                            if (children.containsKey(whom)) { // Проверява дали има контролер за този човек в хеша с чат контролери
                                children.get(whom).addMessage("SERVER", "Потребителят е офлайн"); // Изпраща съобщението към контролера
                            }
                        }

                        // Някой е дошъл онлайн
                        if (line.startsWith("+")) {
                            String[] parts = line.split(";"); // съобщението е от вида "+;id", затова го разделяме на части
                            String whom = parts[0]; // Кой е дошъл онлайн
                            // Ако има чат с този човек, пишем, че се е появил
                            if (children.containsKey(whom)) { // Проверява дали има контролер за този човек в хеша с чат контролери
                                children.get(whom).addMessage("SERVER", "Потребителят вече е онлайн"); // Изпраща съобщение към контролера
                            }
                        }
                        // Обработване на получено чат съобщение
                        // Получено е чат съобщение
                        if (line.startsWith("msg")) { // Започва с "msg"
                            String[] parts = line.split(";"); // Форматът на съобщението е "msg;id;name;message"
                            String sender = parts[1]; // id на изпращащия
                            String senderName = parts[2]; // Име на изпращащия
                            String msg = parts[3]; // Съобщението

                            ChatController child = children.get(sender); // Взема контролерът, отговорен за чата с този човек
                            if (child == null) { // Ако няма отворен прозорец с чат, създава
                                createChatWindow(sender, senderName, msg); // Създава новия прозорец
                            } else { // Ако вече има отворен прозорец, изпраща съобщението към контролера
                                child.addMessage(senderName, msg);
                            }
                        }
                    }

                    System.err.println("[ConversationHandlerThread] Join");

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void close() {
            try {
                socketConnection.close();
                input.close();
                output.close();
                join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Нишка, която взема приятелите на човека
     */
    class FriendGetterThread extends Thread {

        private Socket socketConnection;
        private BufferedReader input = null;
        private BufferedWriter output = null;

        @Override
        public void run() {

            if (socketConnection != null && socketConnection.isConnected()) {
                System.err.println("Friend getter ok.");
            }
            /*
             * Регистриране на клиента в сървъра
             */
            try {
                socketConnection = new Socket();
                socketConnection.setReuseAddress(true);
                socketConnection.connect(new InetSocketAddress(host, port)); // Свързва се към сървъра
                input = new BufferedReader(new InputStreamReader(
                        socketConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(
                        socketConnection.getOutputStream()));
                if (input != null) {
                    String line;
                    // Първо изпраща сесията
                    output.write(sessionId);
                    output.newLine();
                    output.flush();
                    // После изпраща типа на съобщението (4 = списък с приятели)
                    output.write("4;" + id);
                    output.newLine();
                    output.flush();

                    input.readLine(); // Прочита сесията
                    line = input.readLine();
                    if (line.equals("start_friends")) { // Отговорът на сървъра започва със start_friends
                        String[] parts;
                        while ((line = input.readLine()) != null) {
                            if (line.equals("end_friends")) { // Краят на списъка се отбелязва с end_friends
                                break;
                            }
                            System.out.println(line);
                            parts = line.split(":"); // Форматът е id:име, затова разделяме и вземаме отделните части
                            friendsList.getItems().add(new Friend(parts[0], parts[1])); // Добавяне на нов приятел към списъка с приятели
                        }
                    }
                    System.err.println("[FriendGetterThread] Join"); // Край на нишката
                }
//                join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Нишка, която се грижи за изпращане на произволни съобщения към сървъра
     */
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
                socketConnection.connect(new InetSocketAddress(host, port)); // Свързване към сървъра
                System.out.println("Connected to " + host
                        + " at port " + port);

                if (socketConnection != null && !socketConnection.isClosed()) {
                    input = new BufferedReader(new InputStreamReader(
                            socketConnection.getInputStream()));
                    output = new BufferedWriter(new OutputStreamWriter(
                            socketConnection.getOutputStream()));
                }
                while (!send) { // Нишката спи, докато не ѝ се подаде какво да се изпраща (докато send е false)
                    try {
                        wait(); // Приспиване
                    } catch (Exception e) {
                        System.err.println("well...");
                    }
                }
                sendSession(); // Изпращане на сесията
                output.write(msg);
                output.newLine();
                output.flush();
                String line;
                send = false;
                input.readLine(); // Прочитане на сесията, върната от сървъра
                // Прочитане на отговора
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith("-")) { // Някой е минал офлайн
                        String[] parts = line.split(";"); // съобщението е от вида "-;id", затова го разделяме на части
                        String whom = parts[0]; // Кой е минал офлайн
                        // Ако има отворен чат с този човек, пишем, че е излязъл офлайн
                        if (children.containsKey(whom)) { // Проверява дали има контролер за този човек в хеша с чат контролери
                            children.get(whom).addMessage("SERVER", "Потребителят е офлайн"); // Изпраща съобщението към контролера
                        }
                    }
                }
                System.err.println("End.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Метод за изпращане на сесията
         *
         * @throws IOException
         */
        private synchronized void sendSession() throws IOException {
            output.write(sessionId);
            output.newLine();
            output.flush();
            String sess = input.readLine();
            System.err.printf("Sent:%s, Got: %s\n", sessionId, sess);
        }

        /**
         * Този метод се извиква, когато трябва да се изпрати нещо. Той събужда нишката и изпращането се осъществява.
         *
         * @param msg съобщението, което ще се изпраща към сървъра. Трябва да е във формат "тип[;други_параметри]"
         */
        public synchronized void send(String msg) {
            this.msg = msg; // Съобщението
            send = true; // Може да се изпраща
            notifyAll(); // Събуждане на нишката
        }

    }
}

/**
 * Помощен клас за списъка с приятели
 */
class Friend {
    String id;
    String name;

    /**
     * Конструктор...
     *
     * @param id   id
     * @param name име
     */
    public Friend(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
