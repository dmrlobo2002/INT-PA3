import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.HashSet;

import java.util.stream.Collectors;


public class Client {

    private static String SERVER_ADDRESS; // replace with the server's IP address
    private static int SERVER_PORT; // replace with the server's port number

    private String platformFilter;

    private static void printMenuOptions(){
        System.out.println("Menu:");
        System.out.println("random - display a random game");
        System.out.println("random <number> - display <number> random games");
        System.out.println(
                "top <number> - display the top games from 1 to <number>"
        );
        System.out.println("worst <number> - display the worst games from 1 to <number>");
        System.out.println("platform <platform> - enable filter so results are for specific platform");
        System.out.println("platform null - reset filter to expand results to all platforms");
        System.out.println("platforms - view the available platforms you can search for");
        System.out.println("help - print this menu again");
        System.out.println("exit - exit the program");
        System.out.println();
    }

    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Usage: java Client <SERVER_ADDRESS> <SERVER_PORT>");
            System.exit(1);
        }

        String SERVER_ADDRESS = args[0];
        int SERVER_PORT = Integer.parseInt(args[1]);
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader stdin = new BufferedReader(
                        new InputStreamReader(System.in)
                )
        ) {
            //print "hello from server message"
            System.out.println("Connected to " + SERVER_ADDRESS + " on port " + SERVER_PORT);

            System.out.println(in.readLine());
            // Print menu options
            printMenuOptions();

            while (true) {
                // Read user input
                System.out.print("Enter your choice: ");
                String userInput = stdin.readLine();

                // Check if user wants to exit
                if(userInput.equals("help")){
                    printMenuOptions();
                    continue;
                }
                else if (userInput.equals("exit")) {
                    break;
                }

                // Send user input to server
                out.println(userInput);

                // Receive and print response from server
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if(line.equals("<endoftransmission>"))
                        break;
                    response.append(line).append(System.lineSeparator());
                }

                System.out.println(response.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
