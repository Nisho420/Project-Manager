/*
 *
 *  L2T21 Compulsory Task
 *
 */

//importing libraries to create date and scanner objects

import java.sql.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * This class contains the main method and is used to run the program. It requests user input and
 * performs the appropriate operation according to the input. The operations are for managing and
 * manipulating data of projects stored in a database.
 *
 * @author Nisheel Singh
 * @version 1.00
 */
public class Main {

  private static Scanner scanner = new Scanner(System.in);
  private static Connection connection = null;

  /**
   * Main method that controls the program and performs an operation based on user input.
   *
   * @param args command line arguments
   * @throws SQLException if there are errors accessing the database
   */
  public static void main(String[] args) throws SQLException {
    //connecting to database
    try {
      connection = DriverManager.getConnection(
          "jdbc:mysql://localhost:3306/poisepms?allowPublicKeyRetrieval=true&useSSL=false",
          "otheruser",
          "28961234");
      connection.setAutoCommit(false);
    } catch (SQLException error) {
      //handling exception if unable to connect
      System.out.println("Error connecting to database !");
      System.out.println(error.getMessage());
      error.printStackTrace();
      return;
    }

    while (true) {
      //savepoint to rollback changes to if an error occurs
      Savepoint savepoint = null;
      //try-catch to handle SQL exceptions
      try {
        //loop that keeps the program running until user decides to close it
        while (true) {
          //setting savepoint
          savepoint = connection.setSavepoint();
          System.out.println("______________________\nProject Manager\n______________________");
          //method to show menu options
          showOptions();
          System.out.println("\nSelect an option: ");
          String choice = scanner.nextLine();
          //switch to handle the user's input and call the appropriate method
          switch (choice) {
            case "0":
              showOptions();
              break;
            case "1":
              newProject();
              break;
            case "2":
              // project selected by the user
              ResultSet resultSet = userSelectProjectFromDB();
              //allowing user to update the project details, finalise it or go back
              if (resultSet.next()) {
                String projectInfo = getProjectStringFromResultSet(resultSet)
                    + getStakeholdersString(resultSet.getInt("struc_eng_id"),
                    resultSet.getInt("proj_mgr_id"),
                    resultSet.getInt("architect_id"), resultSet.getInt("cust_id"));
                System.out.println(projectInfo + "\n\n-- Options:"
                    + "\n1 - Update"
                    + "\n2 - Mark as finalised"
                    + "\nEnter option (any other key to go back):");
                String option = scanner.nextLine();
                if (option.equals("1")) {
                  updateProject(resultSet);
                } else if (option.equals("2")) {
                  finalise(resultSet);
                } else {
                  System.out.println("Back to Main menu...");
                }
              } else {
                System.out.println("Project not found.");
              }
              break;
            case "3":
              viewProjectsToComplete();
              break;
            case "4":
              viewPastDueProjects();
              break;
            case "5":
              scanner.close();
              connection.close();
              System.out.println("Closing Project Manager...");
              return;
            default:
              System.out.println("Option not found. Try again.");
              break;
          }
        }
      } catch (SQLException | NullPointerException error) {
        connection.rollback(savepoint);
        System.out.println("An error has occurred.");
      } catch (NumberFormatException | InputMismatchException inputError) {
        connection.rollback(savepoint);
        System.out.println("An error has occurred. Invalid input.");
      }
    }
  }

  /**
   * Finalises a project selected by the user and generates an invoice if the user has not settled
   * their fees.
   *
   * @param project the project to be finalised
   * @throws SQLException         if an error occurs with updating the project in the database
   * @throws NullPointerException if null pointer exception occurs
   */
  private static void finalise(ResultSet project) throws SQLException, NullPointerException {
    //if project already finalised
    if (project.getDate("completion_date") != null) {
      System.out.println("\nProject has already been finalised!");
      return;
    }

    // calculating the amount due
    double totalFee = project.getDouble("total_fee");
    double amountPaid = project.getDouble("amount_paid");
    double amountDue = totalFee - amountPaid;
    int projNum = project.getInt("proj_num");
    //setting the completion date as current date
    Date completionDate = Date.valueOf(LocalDate.now());
    Statement statement = connection.createStatement();
    statement.executeUpdate(
        "UPDATE projects SET completion_date='" + completionDate + "' WHERE proj_num='" + projNum
            + "'");
    System.out
        .println("\nProject Finalised.\n___________________________________\nProject: " + projNum);
    System.out.println(project.getString("proj_name"));
    System.out.println("Completion date: " + completionDate);

    //retrieving and printing the details of the customer for the project
    Person customer = createPersonObjectFromResultSet(
        getPersonFromDB("customers", project.getInt("cust_id")), "Customer");
    System.out.println(customer.toString());

    //if there is an amount owed by the customer generate an invoice
    if (amountDue > 0) {
      //generating the invoice for the customer
      System.out.print(
          "\nInvoice\n______________\nProject Fee\n___________________________________"
              + "\nTotal fee: R"
              + String
              .format("%.2f", totalFee)
              + "\nAmount paid: R" + String.format("%.2f", amountPaid)
              + "\n___________________________________\nAmount due: R" + String
              .format("%.2f", amountDue)
              + "\n___________________________________\n");
    } else {
      //if the customer has already paid
      System.out.println("Customer has paid total fee [R" + String
          .format("%.2f", totalFee) + "].\n");
    }
    //committing changes to database
    connection.commit();
  }

  /**
   * Returns the ResultSet for a person in a table.
   *
   * @param tableName the table to query
   * @param id        the primary key for the person in the table
   * @return the ResultSet for a person in a table
   * @throws SQLException if an SQL exception occurs
   */
  private static ResultSet getPersonFromDB(String tableName, int id) throws SQLException {
    //sql query
    String query = "SELECT * FROM " + tableName + " WHERE id=?";
    String sqlQuery = "";
    //completing the query based on the table and primary key (id) passed in
    switch (tableName) {
      case "structural_engineers":
        sqlQuery = query.replace("id", "struc_eng_id");
        break;
      case "project_managers":
        sqlQuery = query.replace("id", "proj_mgr_id");
        break;
      case "architects":
        sqlQuery = query.replace("id", "architect_id");
        break;
      case "customers":
        sqlQuery = query.replace("id", "cust_id");
        break;
      default:
        System.out.println("Table not found.");
        break;
    }
    PreparedStatement prepStatement = connection.prepareStatement(sqlQuery);
    //setting the id value in the statement
    prepStatement.setInt(1, id);
    return prepStatement.executeQuery();
  }

  /**
   * Creates a Person object from a ResultSet.
   *
   * @param resultSet the resultSet for a person
   * @param role      the role of the person
   * @return a Person object
   * @throws SQLException         if a SQL exception occurs
   * @throws NullPointerException if a null pointer exception occurs
   */
  private static Person createPersonObjectFromResultSet(ResultSet resultSet, String role)
      throws SQLException, NullPointerException {
    if (resultSet.next()) {
      Person person = new Person(role, resultSet.getString(2), resultSet.getString(3),
          resultSet.getString(4), resultSet.getString(5));
      person.setId(resultSet.getInt(1));
      return person;
    }
    return null;
  }

  /**
   * Updates data for a Person in the database.
   *
   * @param role      role of the person
   * @param tableName table the person is in
   * @param id        primary key for the person
   * @throws SQLException if an SQL exception occurs
   */
  private static void updatePerson(String role, String tableName, int id) throws SQLException {
    //creating a person object
    ResultSet personResultSet = getPersonFromDB(tableName, id);
    Person person = createPersonObjectFromResultSet(personResultSet, role);
    String columnPrefix = "";
    if (role.equals("Structural Engineer")) {
      columnPrefix = "struc_eng_";
    }
    if (role.equals("Project Manager")) {
      columnPrefix = "proj_mgr_";
    }
    if (role.equals("Architect")) {
      columnPrefix = "architect_";
    }
    if (role.equals("Customer")) {
      columnPrefix = "cust_";
    }

    if (person != null) {
      while (true) {
        //displaying the current details to the user and requesting them to choose
        //what they want to update
        System.out.println("Update " + role + ":\n-------------------");
        String name = person.getName();
        String phoneNum = person.getContactNumber();
        String email = person.getEmail();
        String address = person.getAddress();
        String personDetails = "1. Name         | " + name
            + "\n2. Phone number | " + phoneNum
            + "\n3. Email        | " + email
            + "\n4. Address      | " + address
            + "\nEnter option (any other key to go back):";
        System.out.println(personDetails);
        String choice = scanner.nextLine();
        if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3") && !choice
            .equals("4")) {
          exitUpdatingOptions();
          return;
        }
        //updating the details for the Person object
        System.out.println("Enter update:");
        String update = scanner.nextLine();
        String column = "";
        String oldData = "";
        switch (choice) {
          case "1":
            column = columnPrefix + "name";
            person.setName(update);
            oldData = name;
            break;
          case "2":
            column = columnPrefix + "phone_num";
            person.setContactNumber(update);
            oldData = phoneNum;
            break;
          case "3":
            column = columnPrefix + "email";
            person.setEmail(update);
            oldData = email;
            break;
          case "4":
            column = columnPrefix + "address";
            person.setAddress(update);
            oldData = address;
            break;
        }
        //updating those changes in the database
        String query = "UPDATE " + tableName + " SET " + column + "=? WHERE column=?";
        String sqlUpdate = query.replace("column", columnPrefix + "id");
        PreparedStatement prepStatement = connection.prepareStatement(sqlUpdate);
        prepStatement.setString(1, update);
        prepStatement.setInt(2, id);
        int rows = prepStatement.executeUpdate();
        if (rows > 0) {
          System.out.println("Stakeholder updated [ " + oldData + " -> '" + update + "'].\n");
        } else {
          System.out.println("Update failed !");
        }
      }
    }
  }

  /**
   * Updates data for a project in the database.
   *
   * @param resultSet of the project to be updated
   * @throws SQLException           if a SQL exception occurs
   * @throws NumberFormatException  if a null pointer exception occurs
   * @throws InputMismatchException if an input mismatch exception occurs
   */
  private static void updateProject(ResultSet resultSet)
      throws SQLException, NumberFormatException, InputMismatchException {
    System.out.println("\nUpdate project selected.");
    // project selected by the user
    String statement = "UPDATE projects SET column=? WHERE proj_num=?";
    String projectInfo = getProjectStringFromResultSet(resultSet);
    int strucEngId = resultSet.getInt("struc_eng_id");
    int projMgrId = resultSet.getInt("proj_mgr_id");
    int architectId = resultSet.getInt("architect_id");
    int custId = resultSet.getInt("cust_id");
    while (true) {
      System.out.println(
          projectInfo + getStakeholdersString(strucEngId, projMgrId, architectId, custId));
      //showing update options
      showUpdateProjectOptions();
      //getting input from user for choice from options
      String choice = scanner.nextLine();
      if (!choice.equals("1") && !choice.equals("2") && !choice.equals("3") && !choice.equals("4")
          && !choice.equals("5") && !choice.equals("6") && !choice.equals("7") && !choice
          .equals("8") && !choice.equals("9") && !choice.equals("10") && !choice.equals("11")
          && !choice.equals("12")) {
        exitUpdatingOptions();
        break;
      }
      String column = "";
      switch (choice) {
        case "1":
          column = "proj_name";
          break;
        case "2":
          column = "building_type";
          break;
        case "3":
          column = "address";
          break;
        case "4":
          column = "erf_num";
          break;
        case "5":
          column = "total_fee";
          break;
        case "6":
          column = "amount_paid";
          break;
        case "7":
          column = "deadline";
          break;
        case "8":
          if (resultSet.getDate(10) == null) {
            System.out
                .println("\nCannot update Completion Date -- Project has not been finalised.\n");
            return;
          }
          column = "completion_date";
          break;
        case "9":
          //calling updatePerson() to update details for a person
          updatePerson("Structural Engineer", "structural_engineers", strucEngId);
          break;
        case "10":
          updatePerson("Project Manager", "project_managers", projMgrId);
          break;
        case "11":
          updatePerson("Architect", "architects", architectId);
          break;
        case "12":
          updatePerson("Customer", "customers", custId);
          break;
      }
      //if updating a project detail that is not for a person
      if (!choice.equals("9") && !choice.equals("10") && !choice.equals("11")
          && !choice.equals("12")) {
        String query = statement.replace("column", column);
        int id = resultSet.getInt(1);
        PreparedStatement prepStatement = connection.prepareStatement(query);
        prepStatement.setInt(2, id);
        System.out.println("Enter update:");
        String update = "";
        String oldData = "";
        int oldDataIndex = Integer.parseInt(choice) + 2;
        if (choice.equals("1") || choice.equals("2") || choice.equals("3") || choice
            .equals("4")) {
          update = scanner.nextLine();
          prepStatement.setString(1, update);
          oldData = resultSet.getString(oldDataIndex);
        }

        if (choice.equals("5") || choice.equals("6")) {
          double amount = Double.parseDouble(scanner.nextLine());
          prepStatement.setDouble(1, amount);
          oldData = "R" + String.format("%.2f", resultSet.getDouble(oldDataIndex));
          update = "R" + String.format("%.2f", amount);
        }
        if (choice.equals("7") || choice.equals("8")) {
          Date date = getDateInput();
          update = date.toString();
          prepStatement.setDate(1, date);
          oldData = resultSet.getDate(oldDataIndex).toString();
        }
        //updating the project
        int rows = prepStatement.executeUpdate();
        if (rows > 0) {
          System.out.println("Update complete [" + oldData + " -> " + update + "].\n");
          projectInfo = projectInfo.replace(oldData, update);
        } else {
          System.out.println("Update failed.");
        }
      }
    }
  }

  /**
   * Exits update options and allows user option to save changes to database or not.
   *
   * @throws SQLException if an SQL exception occurs
   */
  private static void exitUpdatingOptions() throws SQLException {
    while (true) {
      System.out.println("Exiting update...\nDo you want to save changes (Y/N) ?\nEnter option:");
      String option = scanner.nextLine().toLowerCase();
      switch (option) {
        case "y":
          System.out.println("Saving changes...");
          connection.commit();
          return;
        case "n":
          System.out.println("Discarding changes...");
          connection.rollback();
          return;
        default:
          System.out.println("Invalid input. Please select an option.");
          break;
      }
    }
  }

  /**
   * Shows a list of incomplete projects.
   *
   * @throws SQLException if an SQL exception occurs
   */
  private static void viewProjectsToComplete() throws SQLException {
    System.out.println("\n_________________________\nProjects to be completed:"
        + "\n_________________________");
    Statement statement = connection.createStatement();
    //getting all incomplete projects and printing them
    ResultSet resultSet = statement
        .executeQuery("SELECT * FROM projects WHERE completion_date IS NULL");
    while (resultSet.next()) {
      System.out.println(getProjectStringFromResultSet(resultSet));
      System.out.println("-----------------------------------------------------------");
    }
    returnToMenu();
  }

  /**
   * Shows a list of projects incomplete and past due date.
   *
   * @throws SQLException if an SQL exception occurs
   */
  private static void viewPastDueProjects() throws SQLException {
    System.out.println("\n________________________\nProjects Past Deadline:"
        + "\n________________________");
    Date date = Date.valueOf(LocalDate.now());
    Statement statement = connection.createStatement();
    ResultSet resultSet = statement
        .executeQuery(
            "SELECT * FROM projects WHERE deadline<'" + date + "' AND completion_date IS NULL");
    while (resultSet.next()) {
      System.out.println(getProjectStringFromResultSet(resultSet));
      System.out.println("-----------------------------------------------------------");
    }
    returnToMenu();
  }


  /**
   * Returns to the main menu.
   */
  private static void returnToMenu() {
    System.out.println("\nPress any key + Enter to go back to Menu.");
    Scanner scanner = new Scanner(System.in);
    if (scanner.hasNext()) {
      System.out.println("Returning to Menu...");
    }
  }

  /**
   * Shows the menu options.
   */
  private static void showOptions() {
    System.out.println("Main Menu\n_____________\n"
        + "1 - Create new\n"
        + "2 - Search - view, update or finalise projects.\n"
        + "3 - View projects to be completed\n"
        + "4 - View projects past due date\n"
        + "5 - Close Project Manager");
  }

  /**
   * Shows the menu options for updating an existing project.
   */
  private static void showUpdateProjectOptions() {
    System.out.println("\nProject update options:\n---------------------\n"
        + "1. Project name\n2. Building Type\n3. Address\n4. ERF number\n"
        + "5. Total fee\n6. Amount paid\n7. Deadline\n8. Completion date\n9. Structural Engineer\n"
        + "10. Project Manager\n11. Architect\n12. Customer\n"
        + "-- Select option (any other key - go back to Main Menu:");
  }


  /**
   * Creates a new project from values passed in.
   *
   * @throws SQLException if an SQL exception occurs
   */
  private static void newProject() throws SQLException {
    System.out.println("\nCreate New Project\n----------------------");
    System.out.println("Project name: ");
    String name = scanner.nextLine();
    /* asking user to try again or return to main menu if they enter a name
     * that is already taken
     */
    while (Project.projectExists(name, connection)) {
      System.out.println(
          "This name is already taken. \nEnter: 1 - Try again\nAny other key - back to Menu");
      String option = scanner.nextLine();
      if (option.equals("1")) {
        System.out.println("Project name: ");
        name = scanner.nextLine();
      } else {
        showOptions();
        return;
      }
    }
    // taking in rest of values needed to create project object
    System.out.println("Building type: ");
    String buildingType = scanner.nextLine();
    System.out.println("Address: ");
    String address = scanner.nextLine();
    System.out.println("ERF no.: ");
    String erfNum = scanner.nextLine();
    double totalFee;
    double amountPaid;
    //loop to request input until valid values are entered
    while (true) {
      //try catch block to handle exception
      try {
        System.out.println("Total Fee: ");
        totalFee = Double.parseDouble(scanner.nextLine());
        System.out.println("Amount Paid: ");
        amountPaid = Double.parseDouble(scanner.nextLine());
        break;
      } catch (NumberFormatException error) {
        System.out.println("Invalid entry. Try again.");
      }
    }
    // creating a date object for the deadline from user input
    Date deadline = getDateInput();
    // getting details for architect, contractor and customer
    String role = "Structural Engineer";
    System.out.println("\nProject Stakeholders\n\nEnter details for:\nStructural Engineer");
    /* creating person object by setting role
     * and calling getStakeholderInput() to get rest of details from user*/
    Person strucEng = getStakeholderInput(role, "structural_engineers");

    role = "Project Manager";
    System.out.println("\nProject Stakeholders\n\nEnter details for:\nProject Manager");
    Person projMgr = getStakeholderInput(role, "project_managers");

    role = "Architect";
    System.out.println("\nProject Stakeholders\nEnter details for:\nArchitect");
    Person architect = getStakeholderInput(role, "architects");

    role = "Customer";
    System.out.println("\nProject Stakeholders\nEnter details for:\nCustomer");
    Person customer = getStakeholderInput(role, "customers");
    // if the project name has not been entered use building type and surname as the name
    if (name.equals("")) {
      //try catch block to handle exception
      try {
        String[] names = customer.getName().split(" ");
        name = buildingType + " " + names[1];
      } catch (ArrayIndexOutOfBoundsException error) {
        System.out.println("Invalid entry.");
      }
    }
    // creating project object
    Project project = Project.createNewProject(name, buildingType, address, erfNum,
        totalFee, amountPaid, deadline);
    //setting project number
    project.setNumForNewProject(connection);
    // adding the stakeholders to the stakeholders array list in the project object
    project.addStakeholder(strucEng);
    project.addStakeholder(projMgr);
    project.addStakeholder(architect);
    project.addStakeholder(customer);

    /*if project object adds to the project list successfully display project details
     *and that the project has been added.*/
    if (project.addProjectToDb(connection)) {
      connection.commit();
      System.out.println(
          "\n__________________\nProject added.\n__________________\n" + project.toString());
    } else {
      System.out.println("Unable to add project.");
    }
  }

  /**
   * Requests input for a stakeholder for the project and returns a new or existing person object.
   *
   * @param role role of this person
   * @return person object
   * @throws SQLException if an SQL exception occurs
   */
  /* method that takes in a specified role and requests the user to input the details
   * for the person in that role - Architect, Contractor or Customer
   * and returns a person object.*/
  private static Person getStakeholderInput(String role, String tableName) throws SQLException {
    while (true) {
      //allows user to select from existing or create new
      System.out.println("Select an existing " + role + " (Y/N)?");
      String choice = scanner.nextLine();
      switch (choice.toLowerCase()) {
        //getting an existing person
        case "y":
          Statement statement = connection.createStatement();
          ResultSet countSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);
          countSet.next();
          int count = countSet.getInt(1);
          if (count > 0) {
            for (int i = 1; i <= count; i++) {
              ResultSet personSet = getPersonFromDB(tableName, i);
              personSet.next();
              String name = personSet.getString(2);
              String phoneNum = personSet.getString(3);
              System.out.println(i + " - " + name + " | " + phoneNum);
            }
            while (true) {
              System.out.println("Select an option (0 - to go back):");
              String option = scanner.nextLine();
              try {
                int index = Integer.parseInt(option);
                if (index == 0) {
                  break;
                } else if (index <= count) {
                  return createPersonObjectFromResultSet(getPersonFromDB(tableName, index), role);
                } else {
                  System.out.println("Invalid input. Try again.\n");
                }
              } catch (NumberFormatException error) {
                System.out.println("Invalid input. Enter a number.\n");
              }
            }
          } else {
            System.out.println("\nNo existing " + role + "s.\n");
          }
          break;
        //creating and updating a new person for the database
        case "n":
          String firstName;
          String surname;
          while (true) {
            System.out.println("First name: ");
            firstName = scanner.nextLine();
            System.out.println("Surname: ");
            surname = scanner.nextLine();
            // handling user input
            if (!firstName.equals(" ") && !surname.equals(" ") && !firstName.isEmpty()
                && !surname
                .isEmpty()) {
              //break loop if input is valid - names have been entered
              break;
            } else {
              System.out.println("Invalid entry. Please enter name and surname.\n");
            }
          }
          //requesting rest of the details to create the person object
          String name = firstName + " " + surname;
          System.out.println("Phone number: ");
          String contactNum = scanner.nextLine();
          System.out.println("Email address: ");
          String email = scanner.nextLine();
          System.out.println("Physical address: ");
          String address = scanner.nextLine();
          //returning a Person object with the values inputted
          Person newPerson = new Person(role, name, contactNum, email, address);
          newPerson.setNewId(connection, tableName);
          //adding to the database
          newPerson.addPersonToDB(connection);
          return newPerson;
        default:
          System.out.println("\nInvalid input. Try again.\n");
      }
    }
  }

  /**
   * Gets input for the project deadline.
   *
   * @return deadline
   * @throws DateTimeException      if date time exception occurs
   * @throws InputMismatchException if mismatched input entered
   */
  // method used to get input from user for deadline date
  // and create a date object
  private static Date getDateInput() {
    int day;
    int month;
    int year;
    //requesting input for the deadline date util valid date entered
    while (true) {
      //try catch to handle exceptions
      try {
        System.out.println("Set Deadline (numeric):\n- Day: ");
        day = scanner.nextInt();
        System.out.println("- Month: ");
        month = scanner.nextInt();
        System.out.println("- Year: ");
        year = scanner.nextInt();
        scanner.nextLine();
        return Date.valueOf(year + "-" + month + "-" + day);

      } catch (IllegalArgumentException error) {
        System.out.println("Invalid date entry. Try again.\n");
      } catch (InputMismatchException errorMismatch) {
        System.out.println("Invalid date entry. Try again.\n");
        scanner.nextLine();
      }
    }
  }

  /**
   * Searches a project by number or name and returns it.
   *
   * @return resultSet for project
   * @throws SQLException if an SQL exception occurs
   */
  private static ResultSet userSelectProjectFromDB() throws SQLException {
    String statement = "SELECT * FROM projects WHERE col=$";
    String query;
    while (true) {
      System.out.print("Search for project by:\t1 - Project name\t2 - Project number"
          + "\nEnter option (1 or 2):");
      String choice = scanner.nextLine();
      if (choice.equals("1")) {
        System.out.println("Search by project name:");
        // user to search for the project by name
        query = statement.replace("col", "proj_name")
            .replace("$", "'" + scanner.nextLine().trim() + "'");
        break;
      } else if (choice.equals("2")) {
        System.out.println("Search by project number:");
        String projectNum = scanner.nextLine();
        query = statement.replace("col", "proj_num").replace("$", projectNum);
        break;
      } else {
        System.out.println("Incorrect input. Please select an option.\n");
      }
    }
    PreparedStatement prepStatement = connection.prepareStatement(query);
    return prepStatement.executeQuery();
  }

  /**
   * Returns a string with the information for a project.
   *
   * @param resultSet of the project
   * @return a string with the information for a project
   * @throws SQLException if an SQL exception occurs
   */
  private static String getProjectStringFromResultSet(ResultSet resultSet) throws SQLException {

    //string with all the object properties and values
    int id = resultSet.getInt(1);
    String projectInfo =
        "\nProject details:\n-----------------\n[Project no.: " + id + "]\nName: " + resultSet
            .getString(3)
            + "\nBuilding type: " + resultSet.getString(4)
            + "\nAddress: " + resultSet.getString(5) + "\nERF no.: " + resultSet.getString(6)
            + "\nTotal fee: R" + String
            .format("%.2f", resultSet.getDouble(7))
            + "\nAmount paid: R" + String.format("%.2f", resultSet.getDouble(8)) + "\nDeadline: "
            + resultSet.getDate(9);
    String finalised;
    // if project has been finalised include the completion date in the string
    if (resultSet.getDate(10) != null) {
      finalised = "Yes";
      projectInfo += "\nCompletion date: " + resultSet.getDate(10);
    } else {
      finalised = "No";
    }
    projectInfo += "\nFinalised: " + finalised;
    return projectInfo;
  }

  /**
   * Returns a string with the details of the stakeholders for a project using the primary key(id)
   * passed in.
   *
   * @param strucEngId  primary key for a structural engineer
   * @param projMgrId   primary key for a project manager
   * @param architectId primary key for an architect
   * @param custId      primary key for a customer
   * @return a string with the details of a person
   * @throws SQLException         if a SQL exception occurs
   * @throws NullPointerException if a null pointer exception occurs
   */
  private static String getStakeholdersString(int strucEngId, int projMgrId, int architectId,
      int custId) throws SQLException, NullPointerException {
    String stakeholders = "\n\nStakeholders\n-------------\n";
    //creating person object from the database
    Person strucEng = createPersonObjectFromResultSet(
        getPersonFromDB("structural_engineers", strucEngId),
        "Structural Engineer");
    Person projMgr = createPersonObjectFromResultSet(getPersonFromDB("project_managers", projMgrId),
        "Project Manager");
    Person architect = createPersonObjectFromResultSet(getPersonFromDB("architects", architectId),
        "Architect");
    Person customer = createPersonObjectFromResultSet(getPersonFromDB("customers", custId),
        "Customer");
    //string containing the stakeholders details
    stakeholders += strucEng.toString() + "\n\n"
        + projMgr.toString() + "\n\n"
        + architect.toString() + "\n\n"
        + customer.toString();

    return stakeholders;
  }
}

