import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ClientGUI extends JFrame{
    private Client client;
    private JTextArea chat;
    private JTextField inputField;
    private JButton sendButton;

    public ClientGUI(Client client) {
        this.client = client;

        setTitle(client.hostName + " [" + client.ip + "]");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        chat = new JTextArea();
        chat.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chat);
        add(scrollPane);

        inputField = new JTextField();
        add(inputField);

        sendButton = new JButton("Send");
        add(sendButton);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                String message = inputField.getText();
                if(!message.isEmpty()){
                    client.createTCPMessage(message); //Maybe Make the send into one method?
                    client.sendToRouter();
                    chat.append("Me: " + message + "\n");
                    inputField.setText("");
                }
            }
        });
        
        setVisible(true);
    
    }

    public void displayMessage(String sender, String msg){
        chat.append(sender + ": " + msg + "\n");
    }
}
