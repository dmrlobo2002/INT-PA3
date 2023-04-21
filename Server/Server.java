import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server {

  private static int PORT; // replace with your desired port number
  private Map<String, String[]> map = new HashMap<>(); // replace with the map you want to use
  private Set<String> platforms = new HashSet<>(); // Set to store unique platforms in the game database

  private String platformFilter;

  public Server() {
    // Constructor for Server class
    System.out.println("SERVER: Loading game database...");
    readCsv("./games.csv"); // Reads the game database from a CSV file
    Instant start = Instant.now();
    populatePlatforms(); // Populates the 'platforms' set with unique platforms in the game database
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end);
    long millis = duration.toMillis();
    System.out.println(
      "SERVER: Finished reading in: " +
      map.size() +
      " games in " +
      millis +
      " ms"
    );
    platformFilter = null; // Initializes the platform filter to null
  }

  private void populatePlatforms() {
    // Method to populate the 'platforms' set with unique platforms in the game database
    for (Map.Entry<String, String[]> entry : map.entrySet()) {
      String[] value = entry.getValue(); // Retrieves the array of values for the current entry
      platforms.add(value[1]); // Adds the value at index 1 (platform) to the 'platforms' set
    }
  }

  private Map<String, String[]> getFilteredMapByPlatform(
    String platformFilter
  ) {
    if (platformFilter == null) {
      System.out.println(
        "SERVER: RESETTING FILTER TO SEARCH THROUGH ALL PLATFORMS"
      );
      return map; // If no filter is set, return the original map
    }

    // Create a new HashMap to store the filtered games
    Map<String, String[]> filteredMap = new HashMap<>();

    // Iterate through the original map and filter games based on the platform
    for (Map.Entry<String, String[]> entry : map.entrySet()) {
      String[] gameInfo = entry.getValue();
      String platform = gameInfo[1]; // Assuming the platform is at index 1 in the gameInfo array

      if (platform.equalsIgnoreCase(platformFilter)) {
        filteredMap.put(entry.getKey(), gameInfo); // If the game's platform matches the filter, add it to the filtered map
      }
    }

    System.out.println("SERVER: SETTING FILTER TO: " + platformFilter);

    return filteredMap; // Return the filtered map
  }

  //parses through CSV from Kaggle
  private void readCsv(String filePath) {
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      // Open the CSV file for reading using a BufferedReader
      String line;
      List<String> values = new ArrayList<>(); // Store the values of each row in the CSV file
      StringBuilder currentValue = new StringBuilder(); // Store the current value being read
      boolean inQuotes = false; // Whether or not we're currently inside a pair of quotes

      // Skip the first line (header row)
      br.readLine();

      // Read in each line of the CSV file
      while ((line = br.readLine()) != null) {
        // Read each character of the line
        for (int i = 0; i < line.length(); i++) {
          char c = line.charAt(i);

          // If we're inside a pair of quotes, handle the character differently
          if (inQuotes) {
            if (c == '\"') {
              // If the current character is a quote, check if it's a closing quote or an escaped quote
              if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                currentValue.append('\"'); // If it's an escaped quote, append a single quote to the current value
                i++; // Skip the next character since it's part of the escaped quote
              } else {
                inQuotes = false; // If it's a closing quote, we're no longer inside a pair of quotes
              }
            } else {
              currentValue.append(c); // If it's not a quote, append the character to the current value
            }
          } else {
            // If we're not inside a pair of quotes, handle the character differently
            if (c == '\"') {
              inQuotes = true; // If the current character is a quote, we're now inside a pair of quotes
            } else if (c == ',') {
              // If the current character is a comma, we've finished reading a value
              values.add(currentValue.toString().trim()); // Add the current value to the values list
              currentValue.setLength(0); // Clear the current value buffer
            } else {
              currentValue.append(c); // If it's not a quote or a comma, append the character to the current value
            }
          }
        }

        // If we're not inside a pair of quotes, we've finished reading a row
        if (!inQuotes) {
          values.add(currentValue.toString().trim()); // Add the final value to the values list
          currentValue.setLength(0); // Clear the current value buffer

          String[] valuesArray = values.toArray(new String[0]); // Convert the values list to an array
          map.put(valuesArray[0], valuesArray); // Add the row to the map, with the first value as the key

          values.clear(); // Clear the values list for the next row
        } else {
          currentValue.append('\n'); // If we're inside a pair of quotes, append a newline character to the current value
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void start() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server started on port " + PORT);

      // Continuously listen for client connections
      while (true) {
        System.out.println("SERVER: Awaiting client connection");
        Socket clientSocket = serverSocket.accept(); // Waits for a client connection
        System.out.println(
          "SERVER: Accepted connection from " + clientSocket.getInetAddress()
        );

        // Send a "Hello" message to the client
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Hello from server!");

        // Handle client request
        BufferedReader in = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream())
        );
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          String response = handleRequest(inputLine); // Processes the client request
          out.println(response); // Sends the response back to the client
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String handleRequest(String request) {
    String[] tokens = request.split("\\s+"); // split the request into tokens by spaces
    String command = tokens[0]; // the first token is the command
    System.out.println("CLIENT: " + command);
    switch (command) {
      case "random":
        int numRandomGames = 1; // Default to 1 random game
        if (tokens.length > 1) { // if there is a second token, try to parse it as an integer
          try {
            numRandomGames = Integer.parseInt(tokens[1]);
          } catch (NumberFormatException e) { // if it's not a valid integer, return an error message
            return "Invalid number: " + tokens[1] + "\n<endoftransmission>";
          }
        }
        return getRandomGames(numRandomGames); // return a message with numRandomGames random games
      case "top":
        if (tokens.length < 2) { // if there are not enough tokens, return an error message
          return "Invalid command. Usage: top <number> \n<endoftransmission>";
        }
        int numGames;
        try {
          numGames = Integer.parseInt(tokens[1]); // try to parse the second token as an integer
        } catch (NumberFormatException e) { // if it's not a valid integer, return an error message
          return "Invalid number: " + tokens[1];
        }
        return getTopGames(numGames); // return a message with the top numGames games
      case "platform":
        if (tokens.length < 2) { // if there are not enough tokens, return an error message
          return "Invalid command. Usage: platform <platform> \n<endoftransmission>";
        }
        if (tokens[1].equals("null")) platformFilter = null; else if ( // if the second token is "null", clear the platform filter
          !platforms.contains(tokens[1])
        ) return "Invalid platform. Please use command 'platforms' to see a list of available platforms \n<endoftransmission>"; else platformFilter = // if the platform is not in the list of available platforms, return an error message // set the platform filter to the second token
          tokens[1];
        System.out.println("SERVER: Platform set to " + platformFilter);
        return (
          "Platform filter set to: " + platformFilter + "\n<endoftransmission>"
        ); // return a message confirming the platform filter has been set
      case "platforms":
        return getAllPlatforms(); // return a message with all available platforms
      case "worst":
        if (tokens.length < 2) { // if there are not enough tokens, return an error message
          return "Invalid command. Usage: top <number> \n<endoftransmission>";
        }
        int numGamess;
        try {
          numGamess = Integer.parseInt(tokens[1]); // try to parse the second token as an integer
        } catch (NumberFormatException e) { // if it's not a valid integer, return an error message
          return "Invalid number: " + tokens[1] + "\n<endoftransmission>";
        }
        return getWorstGames(numGamess); // return a message with the worst numGamess games
      case "search":
        // Handle search command
        if (tokens.length < 2) {
          return "Invalid command. Usage: search <game_title> \n<endoftransmission>";
        }
        String gameTitle = String.join(
          " ",
          Arrays.copyOfRange(tokens, 1, tokens.length)
        );
        return getByTitle(gameTitle);
      default:
        return "Invalid command: " + command + "\n<endoftransmission>"; // if the command is not recognized, return an error message
    }
  }

  private String getAllPlatforms() {
    // Create a StringBuilder to construct the response message
    StringBuilder sb = new StringBuilder();
    System.out.println("SERVER: RETURNING ALL PLATFORMS TO USER");
    // Add introductory message
    sb.append(
      "Here is a list of platforms for which you can enable a filter for:\n"
    );
    // Iterate through all available platforms
    for (String platform : platforms) {
      // Append each platform name to the response message
      sb.append(platform);
      // Add a new line for formatting
      sb.append("\n");
    }
    // Add end of transmission signal to the response message
    sb.append("<endoftransmission>");
    // Return the response message as a string
    return sb.toString();
  }

  private String getRandomGame() {
    // Create a new Random instance
    Random random = new Random();
    System.out.println("SERVER: GETTING RANDOM GAME");
    // Get an array of all the filtered games based on platform filter
    String[][] games = getFilteredMapByPlatform(platformFilter)
      .values()
      .toArray(new String[0][0]);
    // Get a random index within the range of available games
    int randomIndex = random.nextInt(games.length);
    // Get the game information at the random index
    String[] gameInfo = games[randomIndex];
    // Extract individual fields from the game information
    String name = gameInfo[0];
    String platform = gameInfo[1];
    String releaseDate = gameInfo[2];
    String summary = gameInfo[3];
    String metascore = gameInfo[4];
    String userscore = gameInfo[5];
    // Format the game information into a response message
    String formattedGame = String.format(
      "Title: %s\nPlatform: %s\nRelease Date: %s\nSummary: %s\nMetascore: %s\nUserscore: %s\n",
      name,
      platform,
      releaseDate,
      summary,
      metascore,
      userscore
    );
    // Create a new StringBuilder with the formatted game information
    StringBuilder sb = new StringBuilder(formattedGame);
    // Add end of transmission signal to the response message
    sb.append("<endoftransmission>");

    // Return the response message as a string
    return sb.toString();
  }

  private String getRandomGames(int num) {
    // Create a StringBuilder to construct the response message
    StringBuilder sb = new StringBuilder();
    System.out.println("SERVER: GETTING " + num + " RANDOM GAMES");
    // Generate 'num' random games and append each game to the response message
    for (int i = 0; i < num; i++) {
      // Get a random game and append it to the response message
      int currGame = i + 1;
      System.out.println("SERVER: GETTING GAME " + currGame + "/" + num);
      String randomGame = getRandomGame();
      // Remove the last line containing <endoftransmission> to avoid duplication
      int endIndex = randomGame.lastIndexOf("<endoftransmission>");
      if (endIndex != -1) {
        randomGame = randomGame.substring(0, endIndex);
      }
      sb.append(randomGame);
      // Add a new line for formatting, except for the last game
      if (i < num - 1) {
        sb.append("\n");
      }
    }
    // Add end of transmission signal to the response message
    sb.append("<endoftransmission>");
    // Return the response message as a string
    return sb.toString();
  }

  private String[] appendToArray(String[] arr, String[] element) {
    // Create a new array with length equal to arr length plus 1
    String[] newArr = new String[arr.length + 1];

    // Copy elements from arr to newArr
    for (int i = 0; i < arr.length; i++) {
      newArr[i] = arr[i];
    }

    // Join the elements of the element array with a comma and append the resulting string to the end of newArr
    newArr[arr.length] = String.join(",", element);

    // Return the modified newArr
    return newArr;
  }

  private String getWorstGames(int numGames) {
    // Get all games filtered by platform
    String[][] games = getFilteredMapByPlatform(platformFilter)
      .values()
      .toArray(new String[0][0]);

    System.out.println("SERVER: GETTING THE " + numGames + " WORST GAMES");

    // Filter out games with "tbd" values for Metascore or Userscore using a for loop
    List<String[]> filteredGamesList = new ArrayList<>();
    for (String[] game : games) {
      if (
        !game[4].equalsIgnoreCase("tbd") && !game[5].equalsIgnoreCase("tbd")
      ) {
        filteredGamesList.add(game);
      }
    }
    String[][] filteredGames = filteredGamesList.toArray(new String[0][0]);
    // Clear games to save space on heap
    games = null;

    // Sort games by metascore, then by userscore
    Comparator<String[]> comparator = (g1, g2) -> {
      double metascore1 = Double.parseDouble(g1[4]);
      double metascore2 = Double.parseDouble(g2[4]);
      double userscore1 = Double.parseDouble(g1[5]);
      double userscore2 = Double.parseDouble(g2[5]);
      if (metascore1 == metascore2) {
        return Double.compare(userscore1, userscore2); // higher userscore first
      } else {
        return Double.compare(metascore1, metascore2); // lower metascore first
      }
    };
    Arrays.parallelSort(filteredGames, comparator);

    // Build string array of worst games
    String[] worstGames = new String[numGames + 1];
    for (int i = 0; i < numGames && i < filteredGames.length; i++) {
      String[] game = filteredGames[i];
      String name = game[0];
      String metascore = game[4];
      String userscore = game[5];
      worstGames[i] =
        name + " (Metascore: " + metascore + ", Userscore: " + userscore + ")";
    }
    // Add end of transmission signal to the last element of the array
    worstGames[numGames] = "<endoftransmission>";
    // Join the array elements into a single string with newline separator
    return String.join("\n", worstGames);
  }

  //gets top <numGames> of games
  private String getTopGames(int numGames) {
    //Gets games according to platform filter, if null, it just returns map
    System.out.println("SERVER: GETTING THE TOP " + numGames + " GAMES");
    String[][] games = getFilteredMapByPlatform(platformFilter)
      .values()
      .toArray(new String[0][0]);

    // Filter out games with "tbd" values for Metascore or Userscore using a for loop
    List<String[]> filteredGamesList = new ArrayList<>();
    for (String[] game : games) {
      if (
        !game[4].equalsIgnoreCase("tbd") && !game[5].equalsIgnoreCase("tbd")
      ) {
        filteredGamesList.add(game);
      }
    }
    String[][] filteredGames = filteredGamesList.toArray(new String[0][0]);
    //save space on heap
    games = null;

    // Sort games by metascore, then by userscore
    Comparator<String[]> comparator = (g1, g2) -> {
      double metascore1 = Double.parseDouble(g1[4]);
      double metascore2 = Double.parseDouble(g2[4]);
      double userscore1 = Double.parseDouble(g1[5]);
      double userscore2 = Double.parseDouble(g2[5]);
      if (metascore1 == metascore2) {
        return Double.compare(userscore2, userscore1); // higher userscore first
      } else {
        return Double.compare(metascore2, metascore1); // higher metascore first
      }
    };
    Arrays.parallelSort(filteredGames, comparator);

    // Build string array of top games
    String[] topGames = new String[numGames + 1];
    for (int i = 0; i < numGames && i < filteredGames.length; i++) {
      String[] game = filteredGames[i];
      String name = game[0];
      String metascore = game[4];
      String userscore = game[5];
      topGames[i] =
        name + " (Metascore: " + metascore + ", Userscore: " + userscore + ")";
    }
    topGames[numGames] = "<endoftransmission>";
    return String.join("\n", topGames);
  }

  private String getByTitle(String gameTitle) {
    String[][] games = getFilteredMapByPlatform(platformFilter)
      .values()
      .toArray(new String[0][0]);

    System.out.println("SERVER: SEARCHING FOR GAME WITH TITLE " + gameTitle);
    // Filter games by title
    List<String[]> filteredGamesList = new ArrayList<>();
    for (String[] game : games) {
      if (game[0].toLowerCase().contains(gameTitle.toLowerCase())) {
        filteredGamesList.add(game);
      }
    }
    String[][] filteredGames = filteredGamesList.toArray(new String[0][0]);
    //save space on heap
    games = null;

    // Build string array of matching games
    String[] matchingGames = new String[filteredGames.length + 1];
    for (int i = 0; i < filteredGames.length; i++) {
      String[] game = filteredGames[i];
      String name = game[0];
      String platform = game[1];
      String releaseDate = game[2];
      String summary = game[3];
      String metascore = game[4];
      String userscore = game[5];
      String formattedGame = String.format(
        "Title: %s\nPlatform: %s\nRelease Date: %s\nSummary: %s\nMetascore: %s\nUserscore: %s\n",
        name,
        platform,
        releaseDate,
        summary,
        metascore,
        userscore
      );
      matchingGames[i] = formattedGame;
    }
    matchingGames[filteredGames.length] = "<endoftransmission>";
    return String.join("\n", matchingGames);
  }

  // Main method to start the server
  public static void main(String[] args) {
    // Check if the correct number of arguments have been provided
    if (args.length != 1) {
      System.out.println("Usage: java Server <PORT>");
      System.exit(1);
    }
    // Parse the port number from command-line arguments
    PORT = Integer.parseInt(args[0]);
    // Create the server and start it
    Server server = new Server();
    server.start();
  }
}
