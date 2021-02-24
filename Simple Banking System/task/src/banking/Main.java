package banking;

import java.io.File;
import java.io.Reader;
import java.sql.*;
import java.util.*;

public class Main {
  public static void main(String[] args) {
    Controller controller = new Controller();
    // controller.startProgram(args[1]);
    if (args[0].equals("-filename") && args[1] != null) {
      controller.startProgram(args[1]);
    } else {
      System.out.println("No data file specified. Exiting program.");
    }
  }
}

class Card {
  private final Random rand = new Random();
  private final String pin;
  private final String number;
  private int balance;

  public Card() {
    this.pin = generatePin();
    this.number = generateNumber();
    this.balance = 0;
  }

  public Card(String pin, String number, int balance) {
    this.pin = pin;
    this.number = number;
    this.balance = balance;
  }

  private int generateChecksum(List<Integer> lst) {
    int sum = 0;
    List<Integer> check = new ArrayList<Integer>(lst);
    for (int i = 0; i < 15; i++) {
      if (i % 2 == 0) {
        check.set(i, check.get(i) * 2);
      }
      if (check.get(i) > 9) {
        check.set(i, check.get(i) - 9);
      }
      sum += check.get(i);
    }
    int checksum = 10 - sum % 10;
    if (checksum == 10)
      checksum = 0;
    return checksum;
  }

  private String generateNumber() {
    List<Integer> card = new ArrayList<Integer>();
    card.add(4);
    for (int i = 0; i < 5; i++) {
      card.add(0);
    }
    for (int i = 5; i < 14; i++) {
      card.add(rand.nextInt(10));
    }
    card.add(generateChecksum(card));
    StringBuilder cardNumber = new StringBuilder();
    for (Integer number : card) {
      cardNumber.append(number);
    }
    return cardNumber.toString();
  }


  private String generatePin() {
    StringBuilder pin = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      pin.append(rand.nextInt(10));
    }
    return pin.toString();
  }

  public int getBalance() {
    return balance;
  }

  public String getPin() {
    return pin;
  }

  public String getNumber() {
    return number;
  }

  public void setBalance(int newBalance) {
    balance = newBalance;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof Card)) {
      return false;
    }
    Card other = (Card) obj;
    return this.number.equals(other.number) && this.pin.equals(other.pin);
  }

  @Override
  public int hashCode() {
    return pin.hashCode() ^ number.hashCode();
  }

  @Override
  public String toString() {
    return "Card number: " + number + ", and pin: " + pin;
  }

}

class Controller {
  private Database database;
  private final Scanner console = new Scanner(System.in);

  public void startProgram(String filename) {
    String url = "jdbc:sqlite:" + filename;
    this.database = new Database(url);
    // database.createNewDatabase();
    // database.dropTable();
    // database.createNewTable();
    display();

  }

  private void createAccount() {
    Card card = new Card();
    database.insert(card.getNumber(), card.getPin(), 0);
    System.out.println("\nYour card has been created");
    System.out.println("Your card number:");
    System.out.println(card.getNumber());
    System.out.println("Your card PIN:");
    System.out.println(card.getPin());
    System.out.println();
  }

  private boolean login() {
    List<Card> cardsList = new ArrayList<>(database.getCards());
    System.out.println("\nEnter your card number:");
    String number = console.next();
    System.out.println("Enter your PIN:");
    String pin = console.next();
    Card activeCard = new Card(pin, number, 0);
    if (!cardsList.contains(activeCard)) {
      System.out.println("\nWrong card number or PIN!\n");
    } else {
      for (Card card : cardsList) {
        if (activeCard.equals(card)) {
          activeCard = card;
        }
      }
      System.out.println("\nYou have successfully logged in!\n");
      boolean running = true;
      do {
        cardsList = new ArrayList<>(database.getCards());
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
        String choice = console.next();
        switch (choice) {
          case "1":
            System.out.println("\nBalance: " + activeCard.getBalance() + "\n");
            break;
          case "2":
            System.out.println("\nEnter income:");
            int income = console.nextInt();
            activeCard.setBalance(activeCard.getBalance() + income);
            database.addIncome(income, activeCard.getNumber());
            System.out.println("Income was added!\n");
            break;
          case "3":
            System.out.println("\nTransfer");
            System.out.println("Enter card number:");
            String cardToTransfer = console.next();
            if (activeCard.getNumber().equals(cardToTransfer)) {
              showError(2);
            } else if (!isValid(cardToTransfer)) {
              showError(3);
            } else if (!cardExists(cardToTransfer, cardsList)) {
              showError(4);
            } else {
              System.out.println("Enter how much you want to transfer:");
              int transfer = console.nextInt();
              if (activeCard.getBalance() < transfer) {
                showError(1);
              } else {
                activeCard.setBalance(activeCard.getBalance() - transfer);
                database.makeTransfer(activeCard.getNumber(), cardToTransfer, transfer);
                System.out.println("Success!\n");
              }
            }
            break;
          case "4":
            cardsList.remove(activeCard);
            database.removeCard(activeCard.getNumber());
            System.out.println("\nThe account has been closed!\n");
            running = false;
            break;
          case "5":
            System.out.println("\nSuccessfully logged out!\n");
            running = false;
            break;
          case "0":
            System.out.println("\nBye!");
            return false;
        }
      } while (running);
    }
    return true;
  }

  private boolean cardExists(String number, List<Card> cardsList) {
    for (Card card : cardsList) {
      if (card.getNumber().equals(number)) {
        return true;
      }
    }
    return false;
  }

  private boolean isValid(String number) {
    List<Integer> lst = new ArrayList<Integer>();
    for (char digit : number.toCharArray()) {
      lst.add(Integer.parseInt("" + digit));
    }
    int sum = 0;
    List<Integer> check = new ArrayList<Integer>(lst);
    for (int i = 0; i < 15; i++) {
      if (i % 2 == 0) {
        check.set(i, check.get(i) * 2);
      }
      if (check.get(i) > 9) {
        check.set(i, check.get(i) - 9);
      }
      sum += check.get(i);
    }
    sum += check.get(15);
    return sum % 10 == 0;
  }

  private void display() {
    boolean running = true;
    do {
      System.out.println("1. Create account\n" +
              "2. Log into account\n" +
              "0. Exit");
      int input = console.nextInt();
      switch (input) {
        case 1:
          createAccount();
          break;
        case 2:
          if (!login()) {
            running = false;
          }
          break;
        case 0:
          System.out.println("\nBye!");
          running = false;
        default:
      }
    } while (running);
    console.close();
  }

  private void showError(int errorCode) {
    if (errorCode == 1) {
      System.out.println("Not enough money!\n");
    } else if (errorCode == 2) {
      System.out.println("You can't transfer money to the same account!\n");
    } else if (errorCode == 3) {
      System.out.println("Probably you made a mistake in the card number. Please try again!\n");
    } else if (errorCode == 4) {
      System.out.println("Such a card does not exist.\n");
    } else {
      System.out.println("Unrecognized error.\n");
    }
  }
}

class Database {
  private final String url;

  public Database(String url) {
    this.url = url;
  }

  public void createNewDatabase() {
    try (Connection conn = DriverManager.getConnection(url)){
      if (conn != null) {
        DatabaseMetaData meta = conn.getMetaData();
        System.out.println("The driver name is " + meta.getDriverName());
        System.out.println("A new database has been created.");
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void createNewTable() {
    // SQL statement for creating a new table
    String sql = "CREATE TABLE IF NOT EXISTS card (\n"
            + "	id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
            + "	number TEXT,\n"
            + "	pin TEXT,\n"
            + " balance INTEGER DEFAULT 0"
            + ");";
    //
    try (Connection conn = DriverManager.getConnection(url);
      Statement stmt = conn.createStatement()) {
      // create a new table
      stmt.execute(sql);
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void dropTable() {
    String sql = "DROP TABLE IF EXISTS card";
    //
    try (Connection conn = DriverManager.getConnection(url);
         Statement stmt  = conn.createStatement()) {
      // delete table
      stmt.execute(sql);
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void insert(String number, String pin, int balance) {
    String sql = "INSERT INTO card (number, pin, balance) VALUES(?, ?, ?)";
    try (Connection conn = DriverManager.getConnection(url);
         PreparedStatement pstmt  = conn.prepareStatement(sql)){
      pstmt.setString(1, number);
      pstmt.setString(2, pin);
      pstmt.setInt(3, balance);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public List<Card> getCards(){
    List<Card> cardsList = new ArrayList<>();
    String sql = "SELECT id, number, pin, balance FROM card";
    try (Connection conn = DriverManager.getConnection(url);
         Statement stmt  = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      //
      // loop through the result set
      while (rs.next()) {
        Card card = new Card(rs.getString("pin"), rs.getString("number"), rs.getInt("balance"));
        cardsList.add(card);
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
    return cardsList;
  }

  public void addIncome(int income, String number) {
    String sql = "UPDATE card SET balance = balance + ? WHERE number = ?";
    try (Connection conn = DriverManager.getConnection(url);
         PreparedStatement pstmt  = conn.prepareStatement(sql)){
      pstmt.setInt(1, income);
      pstmt.setString(2, number);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void removeCard(String number) {
    String sql = "DELETE FROM card WHERE number = ?";
    try (Connection conn = DriverManager.getConnection(url);
         PreparedStatement pstmt  = conn.prepareStatement(sql)){
      pstmt.setString(1, number);
      pstmt.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void makeTransfer(String sender, String receiver, int transfer) {
    String decreaseBalanceSQL = "UPDATE card SET balance = balance - ? WHERE number = ?";
    String increaseBalanceSQL = "UPDATE card SET balance = balance + ? WHERE number = ?";
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(url);
      conn.setAutoCommit(false);
      try (PreparedStatement decreaseBalance = conn.prepareStatement(decreaseBalanceSQL);
           PreparedStatement increaseBalance = conn.prepareStatement(increaseBalanceSQL)) {
      decreaseBalance.setInt(1, transfer);
      decreaseBalance.setString(2, sender);
      decreaseBalance.executeUpdate();

      increaseBalance.setInt(1, transfer);
      increaseBalance.setString(2, receiver);
      increaseBalance.executeUpdate();

      conn.commit();
      }
    } catch (SQLException e) {
      if (conn != null) {
        try {
          System.out.println(e.getMessage());
          System.err.print("Transaction is being rolled back");
          conn.rollback();
        } catch (SQLException excep) {
          excep.printStackTrace();
        }
      }
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
      } catch (SQLException e) {
        System.out.println(e.getMessage());
      }
    }
  }

  public void listCards() {
    String sql = "SELECT * FROM card";
    try (Connection conn = DriverManager.getConnection(url);
         Statement stmt  = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery(sql)){
      while (rs.next()) {
        System.out.println(rs.getInt("id") +  "\t" +
                rs.getString("number") + "\t" +
                rs.getString("pin") + "\t" +
                rs.getInt("balance"));
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }


}



