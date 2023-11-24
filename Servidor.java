import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static final int PUERTO = 6789;
    private static Set<ClienteHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) throws IOException {
        System.out.println("El servidor se ha inizializado y está en uso.");
        ServerSocket listener = new ServerSocket(PUERTO);

        try {
            while (true) {
                // Espera a que un cliente se conecte y crea un nuevo hilo para manejarlo
                Socket socket = listener.accept();
                ClienteHandler handler = new ClienteHandler(socket);
                clientHandlers.add(handler);
                handler.start();
            }
        } finally {
            listener.close();
        }
    }

    private static class ClienteHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nombreUsuario;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
            System.out.println("Usuario que ingresó al chat: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }

        public void run() {
            try {
                // Configura los flujos de entrada y salida para la comunicación con el cliente
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Lee el nombre de usuario del cliente y lo difunde a todos los clientes conectados
                nombreUsuario = in.readLine().trim();
                if (!nombreUsuario.isEmpty()) {
                    broadcast(nombreUsuario + " Ha ingresado.");
                }

                String input;
                // Escucha los mensajes del cliente y los difunde a todos los clientes
                while ((input = in.readLine()) != null) {
                    broadcast(nombreUsuario + ": " + input);
                }
            } catch (SocketException e) {
                System.out.println(nombreUsuario + " Ha salido.");
            } catch (IOException e) {
                System.out.println("Error en ClienteHandler: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Maneja la desconexión del cliente
                if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                    System.out.println(nombreUsuario + " Ha abandonado el Chat.");
                    broadcast(nombreUsuario + " Ha salido.");
                }
                closeSocket();
                clientHandlers.remove(this);
            }
        }

        private void broadcast(String message) {
            // Difunde el mensaje a todos los clientes conectados
            for (ClienteHandler handler : clientHandlers) {
                handler.out.println(message);
            }
        }

        private void closeSocket() {
            // Cierra el socket del cliente
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error cerrando la sesión: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}