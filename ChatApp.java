import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatApp {
    private JFrame frame;
    private JTextPane textPaneChat;
    private JTextField textFieldInput;
    private JButton sendButton;
    private PrintWriter out;
    private Socket socket;
    private StyledDocument doc;
    private Map<String, Color> userColors;
    private SimpleAttributeSet userStyle;
    private String nombreUsuario;

    public ChatApp() {
        userColors = new LinkedHashMap<>();
        userStyle = new SimpleAttributeSet();

        initializeGUI();
        establishListeners();
        initiateConnection();
    }

    private void initializeGUI() {
        // Configura la interfaz gráfica del cliente
        frame = new JFrame("David's Chat Interface");
        textPaneChat = new JTextPane();
        textPaneChat.setEditable(false);
        doc = textPaneChat.getStyledDocument();
        textFieldInput = new JTextField("ESCRIBA SU USUARIO");
        textFieldInput.setForeground(Color.GRAY);
        textFieldInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textFieldInput.getText().equals("ESCRIBA SU USUARIO")) {
                    textFieldInput.setText("");
                    textFieldInput.setForeground(Color.BLACK);
                }
            }
        });
        sendButton = new JButton("Enter");

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(textFieldInput, BorderLayout.CENTER);
        panelInferior.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(textPaneChat), BorderLayout.CENTER);
        frame.add(panelInferior, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setVisible(true);
    }

    private void establishListeners() {
        // Configura los listeners para el campo de entrada y el botón de enter
        ActionListener actionListener = e -> processUserInput();
        textFieldInput.addActionListener(actionListener);
        sendButton.addActionListener(actionListener);
    }

    private void processUserInput() {
        // Procesa la entrada del usuario y maneja el nombre de usuario y los mensajes
        if (nombreUsuario == null) {
            nombreUsuario = textFieldInput.getText().toUpperCase();
            textFieldInput.setText("");
            out.println(nombreUsuario);
        } else {
            sendMessage();
        }
    }

    private void initiateConnection() {
        try {
            // Establece la conexión con el servidor y configura flujos de entrada/salida (red Wifi)
            socket = new Socket("172.100.0.168", 6789);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Inicia un hilo para escuchar mensajes del servidor y actualizar la interfaz gráfica
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        appendToPane(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            // Muestra un mensaje de error si la conexión con el servidor falla
            JOptionPane.showMessageDialog(null, "¡Error al conectar al servidor!", "Sin conexión", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void sendMessage() {
        // Envía un mensaje al servidor
        String message = textFieldInput.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            textFieldInput.setText("");
        }
    }

    private void appendToPane(String message) {
        // Actualiza la interfaz gráfica con nuevos mensajes del servidor
        SwingUtilities.invokeLater(() -> {
            try {
                if (message.startsWith("Ingresó_un_Usuario_Nuevo") || message.startsWith("Usuario_ha_salido")) {
                    // Establece el estilo para mensajes de nuevo usuario o usuario desconectado
                    StyleConstants.setBold(userStyle, true);
                    doc.insertString(doc.getLength(), message + "\n", userStyle);
                } else {
                    // Analiza y muestra mensajes de chat normales
                    int colonIndex = message.indexOf(":");
                    if (colonIndex > -1) {
                        String userName = message.substring(0, colonIndex).trim();
                        String text = message.substring(colonIndex + 1).trim();
                        Color color = userColors.computeIfAbsent(userName, k -> getNextColor());
                        StyleConstants.setForeground(userStyle, color);
                        doc.insertString(doc.getLength(), userName + ": ", userStyle);
                        doc.insertString(doc.getLength(), text + "\n", null);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private Color getNextColor() {
        // Genera un color aleatorio para representar a un usuario
        return new Color(
            (int) (Math.random() * 256),
            (int) (Math.random() * 256),
            (int) (Math.random() * 256)
        );
    }

    public static void main(String[] args) {
        // Inicia la interfaz gráfica del cliente en un hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> new ChatApp());
    }
}
