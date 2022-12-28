//importing libraries

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * This class stores the information for a project. It contains
 * methods to manipulate the projects in a database.
 *
 * @author Nisheel Singh
 * @version 1.00
 */
public class Project {

  private int num;
  private String name;
  private String buildingType;
  private String address;
  private String erfNum;
  private double totalFee;
  private double amountPaid;
  private Date deadline;
  private ArrayList<Person> stakeholders;
  private String finalised;
  private Date completionDate;

  /**
   * Constructor to initialise Project object.
   *
   * @param name         name for this project
   * @param buildingType building type for this project
   * @param address      physical address for this project
   * @param erfNum       ERF number for this project
   * @param totalFee     total fee for this project
   * @param amountPaid   amount paid to date for this project
   * @param deadline     deadline for this project
   */
  public Project(String name, String buildingType, String address, String erfNum, double totalFee,
      double amountPaid, Date deadline) {
    this.name = name;
    this.buildingType = buildingType;
    this.address = address;
    this.erfNum = erfNum;
    this.totalFee = Math.round(totalFee);
    this.amountPaid = Math.round(amountPaid);
    this.deadline = deadline;
    this.finalised = "No";
    this.completionDate = null;
    this.stakeholders = new ArrayList<>();
  }

  /**
   * Sets the project number.
   *
   * @param connection database connection
   * @throws SQLException if there are errors accessing the database
   */
  public void setNumForNewProject(Connection connection) throws SQLException {
    String query = "SELECT proj_num FROM projects ORDER BY proj_num DESC LIMIT 1";
    try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
      ResultSet resultSet = preparedStatement.executeQuery();
      if (resultSet.next()) {
        this.num = resultSet.getInt(1) + 1;
      }
    }
  }

  /**
   * Creates a project object.
   *
   * @param name         name for this project
   * @param buildingType building type for this project
   * @param address      physical address for this project
   * @param erfNum       ERF number for this project
   * @param totalFee     total fee for this project
   * @param amountPaid   amount paid to date for this project
   * @param deadline     deadline for this project
   * @return new project object
   */
  public static Project createNewProject(String name, String buildingType, String address,
      String erfNum, double totalFee, double amountPaid, Date deadline) {
    return new Project(name, buildingType, address, erfNum, totalFee, amountPaid, deadline);
  }

  /**
   * Overrides the project object toString method and returns a string containing all project
   * details. Attributes and values in string.
   *
   * @return string containing all project details
   */
  @Override
  public String toString() {
    StringBuilder people = new StringBuilder();
    // adding the string for each person object to the project string
    for (Person person : this.stakeholders) {
      people.append(person.toString()).append("\n\n");
    }
    //string with all the object properties and values
    String objString =
        "Project no.: " + num + "\nName: " + name + "\nBuilding type: " + buildingType
            + "\nAddress: " + address + "\nERF no.: " + erfNum + "\nTotal fee: R" + String
            .format("%.2f", totalFee)
            + "\nAmount paid: R" + String.format("%.2f", amountPaid) + "\nDeadline: " + deadline
            + "\nFinalised: " + finalised;

    // if project has been finalised include the completion date in the string
    if (finalised.equals("Yes")) {
      objString +=
          "\nCompletion date: " + completionDate + "\n\nStakeholders\n-------------\n" + people;
    } else {
      objString += "\n\nStakeholders\n-------------\n" + people;
    }

    return objString;
  }

  /**
   * Adds a person to this project's stakeholder list
   *
   * @param person the stakeholder to be added to the project's stakeholder list
   */
  // adding stakeholder to this project stakeholders list
  public void addStakeholder(Person person) {
    stakeholders.add(person);
  }

  /**
   * Adds a project to the database.
   *
   * @param connection database connection
   * @return true if added else return false
   * @throws SQLException if there are errors accessing the database
   */
  public boolean addProjectToDb(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection
        .prepareStatement("INSERT INTO projects VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)");
    preparedStatement.setInt(1, this.num);
    preparedStatement.setInt(2, this.stakeholders.get(0).getId());
    preparedStatement.setString(3, this.name);
    preparedStatement.setString(4, this.buildingType);
    preparedStatement.setString(5, this.address);
    preparedStatement.setString(6, this.erfNum);
    preparedStatement.setDouble(7, this.totalFee);
    preparedStatement.setDouble(8, this.amountPaid);
    preparedStatement.setDate(9, this.deadline);
    preparedStatement.setDate(10, this.completionDate);
    preparedStatement.setInt(11, this.stakeholders.get(1).getId());
    preparedStatement.setInt(12, this.stakeholders.get(2).getId());
    preparedStatement.setInt(13, this.stakeholders.get(3).getId());

    int rows = preparedStatement.executeUpdate();
    return rows > 0;
  }

   /**
   * Searches for a project by name and returns true if found else false.
   *
   * @param projectName name to search for
   * @param connection database connection
   * @return true if found, else false
    * @throws SQLException if there are errors accessing the database
   */
  public static boolean projectExists(String projectName, Connection connection)
      throws SQLException {
    PreparedStatement preparedStatement = connection
        .prepareStatement("SELECT proj_name FROM projects WHERE proj_name='" + projectName + "'");

    ResultSet resultSet = preparedStatement.executeQuery();
    // return true if an existing project found else false
    return resultSet.next();
  }
}