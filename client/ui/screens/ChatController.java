package client.ui.screens;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.Date;
import java.util.HashMap;

/**
 * Контролер за екрана chat
 */
public class ChatController {
    public ListView<String> chatList;
    public TextField msgField;
    private MainController parent;
    private String id;
    private String name;

    private HashMap<String, String> emoticons;

    public ChatController() {
        emoticons = new HashMap<>();
        emoticons.put(":shrug:", "¯\\_(ツ)_/¯");
        emoticons.put(":lenny:", "( ͡° ͜ʖ ͡°)");
        emoticons.put(":disappr:", "ಠ_ಠ");
        emoticons.put(":tableflip:", "（╯°□°）╯︵ ┻━┻");

    }

    private String processMessage(String msg) {
        String ret = msg;
        // Проверява дали съобщението съдържа емотиконки. Ако съдържа, си свършва работата
        for (String emote : emoticons.keySet()) {
            if (msg.contains(emote)) {
                ret = msg.replaceAll(emote, emoticons.get(emote));
            }
        }
        return ret;
    }

    /**
     * Метод за получаване на съобщение. Взема от кого е и го добавя в списъка със съобщения
     *
     * @param from Изпращач
     * @param msg  Съобщение
     */
    public synchronized void addMessage(String from, String msg) {
        System.out.println("Adding a message: " + msg);

        msg = this.processMessage(msg);
        // Добавя съобщението като изписва и датата
        final String finalMsg = msg;
        Platform.runLater(() -> {
            chatList.getItems().add(String.format("[%s] %s: %s", new Date().toString(), from, finalMsg));
            chatList.scrollTo(chatList.getItems().size() - 1);
        });
    }

    /**
     * Изпраща съобщение към човека, с когото е чата
     *
     * @param actionEvent JavaFX event
     */
    public void sendAction(ActionEvent actionEvent) {
        String msg = this.processMessage(msgField.getText());
        chatList.getItems().add(String.format("[%s] %s: %s", new Date().toString(), name, msg));
        parent.sendTo(id, msg);
        chatList.scrollTo(chatList.getItems().size() - 1);
        msgField.setText("");
    }


    public void setParent(MainController parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
