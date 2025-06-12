import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;


public class ClientGUI extends JFrame{
    private Client client;

//----- CHAT PANEL COMPNENTS -----
    private JTextArea chat;
    private JTextField inputField;
    private JButton sendButton;
    private JButton backButton;
    private String currentChatIP;

//----- GUI ARCHITECTURE COMPONENTS -----
    private JPanel cards;
    private static final String CLIENT_LIST = "ClientList";
    private static final String CHAT_PANEL = "ChatPanel";

// ----- CLIENT LIST COMPONENTS -----
    private JList<String> clientJList;
    private DefaultListModel<String> clientListModel;

// ----- CHAT HISTORY COMPONENTS -----
    private final Map<String, StringBuilder> chatHistories = new ConcurrentHashMap<>();

    public ClientGUI(Client client) {
        this.client = client;

        setTitle(client.hostName + " [" + client.ip + "]");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
        setLocationRelativeTo(null);

        cards = new JPanel(new CardLayout());

        initClientListPanel();
        initChatPanel();
        updateClientList(client.getConnectionList());

        add(cards);

        setVisible(true);
    }

    private void closeConnection(){
        client.close();
        dispose();
        System.exit(0);
    }

    private void initClientListPanel(){
        JPanel panel = new JPanel(new BorderLayout());

        clientListModel = new DefaultListModel<>();

        clientJList = new JList<>(clientListModel);

        clientJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(clientJList);

        JButton openChatButton = new JButton("Open Chat");

        openChatButton.addActionListener(e -> {
            String selected = clientJList.getSelectedValue();
            if(selected != null){
                String ip = selected.split(" ")[1].replaceAll(".*\\[|\\].*", "");
                String hostName = selected.split(" ")[0];

                openChat(ip, hostName);
            }
        });

        panel.add(new JLabel("Connected Clients: "), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(openChatButton, BorderLayout.SOUTH);

        cards.add(panel, CLIENT_LIST);
    }

    private void initChatPanel(){
        JPanel panel = new JPanel(new BorderLayout());

        chat = (new JTextArea());
        chat.setEditable(false);

        JScrollPane chatScroll = new JScrollPane(chat);

        JPanel bottom = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        backButton = new JButton("Back");

        sendButton.addActionListener(e -> sendMessage());
        backButton.addActionListener(e -> showClientList());

        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        bottom.add(backButton, BorderLayout.WEST);

        panel.add(chatScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        cards.add(panel, CHAT_PANEL);
    }

    private void openChat(String ip, String hostName){
        currentChatIP = ip;
        String currentHostName = hostName;
            StringBuilder history = chatHistories.get(ip);
            if (history != null) {
                chat.setText(history.toString());
            }
        setTitle("Chat with " + currentHostName);

        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, CHAT_PANEL);
    }

    private void showClientList(){
        updateClientList(client.getConnectionList());
        setTitle("Client: " + client.hostName + " [" + client.ip + "]");
        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, CLIENT_LIST);
    }

    private void sendMessage(){
        String msg = inputField.getText().trim();
        String hostName = client.getConnectionList().getOrDefault(currentChatIP, currentChatIP);

        if(msg.isEmpty()){
            return;
        }

         chatHistories
            .computeIfAbsent(hostName, k -> new StringBuilder())
            .append("Me: ").append(msg).append("\n");

        client.createTCPMessage(msg);
        client.pac.destIP = currentChatIP;
        client.sendToRouter();

        chat.append("Me: " + msg + "\n");
        inputField.setText("");
    }

    public void updateClientList(ConcurrentMap<String, String> connectionList){
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for(Map.Entry<String, String> entry : connectionList.entrySet()){
                if(!entry.getKey().equals(client.ip)){
                    clientListModel.addElement(entry.getValue() + " [" + entry.getKey() + "]");
                }
            }
        });
    }
    public void receiveMessage(String senderIP, String msg){
        if(senderIP.equals(currentChatIP)){
            String hostName = client.getConnectionList().getOrDefault(senderIP, senderIP);

            chatHistories
                .computeIfAbsent(hostName, k -> new StringBuilder())
                .append(hostName).append(": ").append(msg).append("\n");

            SwingUtilities.invokeLater(() -> {
                chat.append(hostName + ": " + msg + "\n");
            });
        } else {
            chatHistories
                .computeIfAbsent(senderIP, k -> new StringBuilder())
                .append(client.getConnectionList().getOrDefault(senderIP, senderIP))
                .append(": ").append(msg).append("\n");
        }
    }
}
