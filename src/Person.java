//importing libraries

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Person class is used for storing person information. It contains methods to manipulate a person
 * in a database.
 *
 * @author Nisheel Singh
 * @version 1.00
 */
public class Person {

  private int id;
  private String role;
  private String name;
  private String contactNumber;
  private String email;
  private String address;

  /**
   * This is a constructor to initialize a Person object from values passed in.
   *
   * @param role          role of this person
   * @param name          name of this person
   * @param contactNumber contact number of this person
   * @param email         email address of this person
   * @param address       physical address of this person
   */
  public Person(String role, String name, String contactNumber, String email, String address) {
    this.role = role;
    this.name = name;
    this.contactNumber = contactNumber;
    this.email = email;
    this.address = address;
  }

  /**
   * Returns the person object's ID.
   *
   * @return the person object's ID
   */
  public int getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Sets the ID based on the last ID in the column.
   *
   * @param connection database connection
   * @param tableName name of the table
   * @throws SQLException if there are errors accessing the database
   */
  public void setNewId(Connection connection, String tableName) throws SQLException {
    String columnID = "";
    switch (tableName) {
      case "structural_engineers":
        columnID = "struc_eng_id";
        break;
      case "project_managers":
        columnID = "proj_mgr_id";
        break;
      case "architects":
        columnID = "architect_id";
        break;
      case "customers":
        columnID = "cust_id";
        break;
    }
    String query =
        "SELECT " + columnID + " FROM " + tableName + " ORDER BY " + columnID + " DESC LIMIT 1";
    PreparedStatement preparedStatement = connection.prepareStatement(query);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.id = resultSet.getInt(1) + 1;
    }
  }

  /**
   * Adds a person to the database.
   *
   * @param connection database connection
   * @throws SQLException if there are errors accessing the database
   */
  public void addPersonToDB(Connection connection) throws SQLException {
    String statement = "INSERT INTO $ VALUES(?,?,?,?,?)";
    String tableName = "";
    switch (this.role) {
      case "Structural Engineer":
        tableName = "structural_engineers";
        break;
      case "Project Manager":
        tableName = "project_managers";
        break;
      case "Architect":
        tableName = "architects";
        break;
      case "Customer":
        tableName = "customers";
        break;
      default:
        System.out.println("Unable to add person to database.");
    }
    String update = statement.replace("$", tableName);
    PreparedStatement preparedStatement = connection.prepareStatement(update);
    preparedStatement.setInt(1, this.id);
    preparedStatement.setString(2, this.name);
    preparedStatement.setString(3, this.contactNumber);
    preparedStatement.setString(4, this.email);
    preparedStatement.setString(5, this.address);
    preparedStatement.executeUpdate();
  }

  /**
   * Overrides this Person object toString method.
   *
   * @return string of this Person object's attributes and values
   */
  @Override
  public String toString() {
    return "[" + role + "]\nName: " + name + "\nPhone Number: " + contactNumber
        + "\nE-mail Address: " + email + "\nPhysical Address: " + address;
  }

  /**
   * Sets the contact number for this person.
   *
   * @param contactNumber contact number for this person
   */
  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  /**
   * Sets the email address for this person.
   *
   * @param email email address for this person
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Sets the physical address for this person.
   *
   * @param address physical address for this person
   */
  public void setAddress(String address) {
    this.address = address;
  }

  /**
   * Sets the name for this person.
   *
   * @param name name to set for person
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the name of this person.
   *
   * @return name of this person
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the contact number of this person.
   *
   * @return contact number of this person
   */
  public String getContactNumber() {
    return contactNumber;
  }

  /**
   * Gets the email address of this person.
   *
   * @return email of this person
   */
  public String getEmail() {
    return email;
  }

  /**
   * Gets the physical address of this person.
   *
   * @return physical address of this person
   */
  public String getAddress() {
    return address;
  }

}
