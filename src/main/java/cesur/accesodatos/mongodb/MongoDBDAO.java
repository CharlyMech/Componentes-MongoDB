package cesur.accesodatos.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

/**
 * MongoDB component.
 * Interfaces implemented:
 * - {@link IDAO} for data operations.
 * - {@link ConnectionInterface} for database server connection management.
 * - {@link Menu} for user related interactions.
 * All implemented methods first check if the connection was successful checking a boolean variable so the user can't execute anything without calling the connection method first
 *
 * @author Carlos Sánchez Recio.
 */

public class MongoDBDAO implements IDAO, ConnectionInterface, Menu {
	// TERMINAL OUTPUT COLORS -> All these variables are static and final. They're meant to be used for terminal messages so the user can understand them better.
	/**
	 * BLACK_FONT -> Static and final {@link String} variable that stores ASCII code for black font color.
	 */
	static final String BLACK_FONT = "\u001B[30m";
	/**
	 * GREEN_FONT -> Static and final {@link String} variable that stores ASCII code for green font color.
	 */
	static final String GREEN_FONT = "\u001B[32m";
	/**
	 * WHITE_BG -> Static and final {@link String} variable that stores ASCII code for white background color.
	 */
	static final String WHITE_BG = "\u001B[47m";
	/**
	 * RESET -> Static and final {@link String} variable that stores ASCII code to reset terminal colors.
	 */
	static final String RESET = "\u001B[0m";
	/**
	 * USER_INPUT -> Static and final {@link String} variable that stores a simple prompt for the user when he has to introduce any data.
	 */
	static final String USER_INPUT = String.format("%s%s>%s ", BLACK_FONT, WHITE_BG, RESET);
	// Class variables
	/**
	 * connectionFlag -> Boolean variable that indicates whether the connection with the database has been established or not. Set to false by default.
	 */
	private boolean connectionFlag = false;
	/**
	 * connectionFlag -> Boolean variable for program execution. Set to true by default.
	 */
	private boolean executionFlag = true;
	/**
	 * isr -> {@link InputStreamReader} variable that will allow the user to insert data through terminal.
	 */
	private final InputStreamReader isr = new InputStreamReader(System.in);
	/**
	 * ipPattern -> {@link Pattern} variable that stores the IPv4 regular expression. Also allows the localhost IP.
	 */
	private final Pattern ipPattern = Pattern.compile("(localhost)|(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)");
	/**
	 * client -> {@link MongoClient} variable for MongoDB actions through program execution.
	 */
	private MongoClient client;
	/**
	 * client -> {@link MongoDatabase} variable for MongoDB database setting.
	 */
	private MongoDatabase db;

	// Constructor

	/**
	 * Empty constructor.
	 */
	public MongoDBDAO() {
	}

	// Class methods

	/**
	 * Method to transform a {@link Document} object to {@link Department} object.
	 *
	 * @param doc {@link Document} object with the required {@link Department} information from database.
	 * @return Transformed {@link Department} object.
	 */
	private Department fromDocumentToDepartment(Document doc) {
		Department dept = new Department();
		// Set all attributes to the object
		dept.setDepno(doc.getInteger("depno"));
		dept.setName(doc.getString("nombre"));
		dept.setLocation(doc.getString("ubicacion"));
		return dept;
	}

	/**
	 * Method to transform a {@link Document} object to {@link Employee} object.
	 *
	 * @param doc {@link Document} object with the required {@link Employee} information from database.
	 * @return Transformed {@link Employee} object.
	 */
	private Employee fromDocumentToEmployee(Document doc) {
		Employee emp = new Employee();
		// Set all attributes to the object
		emp.setEmpno(doc.getInteger("empno"));
		emp.setName(doc.getString("nombre"));
		emp.setPosition(doc.getString("puesto"));
		emp.setDepno(doc.getInteger("depno"));
		return emp;
	}

	// Implementation from IDAO interface
	@Override
	public List<Employee> findAllEmployees() {
		List<Employee> employeeList = new ArrayList<Employee>(); // List that will contain all retrieved Employee
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
			try (MongoCursor<Document> cursor = employees.find().sort(Sorts.ascending("empno")).iterator()) { // Create MongoCursor and iterate to add the transformed Document into the list
				while (cursor.hasNext()) {
					Document empDoc = cursor.next();
					employeeList.add(this.fromDocumentToEmployee(empDoc));
				}
			}
			return employeeList; // Return the filled list
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public Employee findEmployeeById(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
			Document doc = employees.find(eq("empno", id)).first(); // Query for the Document
			// Check if the query Document value is null
			if (doc == null) {
				return null;
			}
			Employee emp = this.fromDocumentToEmployee(doc); // Transform the Document into Employee
			return emp; // Return Employee
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public void addEmployee(Employee employee) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
			Document doc = new Document() // Create Document object and append all data
					.append("empno", employee.getEmpno()).append("nombre", employee.getName()).append("puesto", employee.getPosition()).append("depno", employee.getDepno());
			employees.insertOne(doc); // Execute insert operation
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from IDAO interface
	@Override
	public Employee updateEmployee(Object id) {
		BufferedReader reader = new BufferedReader(this.isr); // To read user input
		MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
		Document doc = employees.find(eq("empno", id)).first(); // Get Employee's Document to be updated
		try {
			if (this.connectionFlag) { // Ask the user for all required data to update the Employee
				System.out.println("Insert updated Employee's NAME:");
				System.out.print(USER_INPUT);
				String updateName = reader.readLine();
				if (updateName.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return null;
				}
				System.out.println("Insert updated Employee's POSITION:");
				System.out.print(USER_INPUT);
				String updatePosition = reader.readLine();
				if (updatePosition.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return null;
				}
				System.out.println("Insert updated Employee's DEPNO:");
				System.out.print(USER_INPUT);
				String updateDepno = reader.readLine();
				if (!updateDepno.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Departments' ID are Integer values");
					return null;
				} else if (findDepartmentById(Integer.parseInt(updateDepno)) == null) { // There is no Department with introduced DEPNO
					System.err.println("ERROR: There is no Department with DEPNO " + updateDepno);
					return null;
				}

				// Everything is good -> Update information in DB and create Employee object to be returned
				Bson updates = Updates.combine( // Specify new updates to the Document
						Updates.set("nombre", updateName), Updates.set("puesto", updatePosition), Updates.set("depno", Integer.parseInt(updateDepno)));
				UpdateResult result = employees.updateOne(doc, updates); // Execute update operation
				System.out.printf("Emplyee with EMPNO %s updated successfully!!\n->%s\n", id, result.getModifiedCount());
				return new Employee((Integer) id, updateName, updatePosition, Integer.parseInt(updateDepno)); // Return Employee object
			} else {
				System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
				return null;
			}
		} catch (IOException ioe) {
			System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			return null;
		} catch (MongoException me) {
			System.err.println("ERROR: MongoException error reported: " + me);
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public Employee deleteEmployee(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
			Employee deletedEmployee = this.fromDocumentToEmployee(Objects.requireNonNull(employees.find(eq("empno", id)).first())); // This is checked not to be null
			Bson query = eq("empno", id); // Query equality filter
			DeleteResult result = employees.deleteOne(query); // Execute delete operation
			System.out.println("Employee deleted! " + result.getDeletedCount()); // Print deleted items just as a visual check
			// Return deleted Employee object
			return deletedEmployee;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public List<Department> findAllDepartments() {
		if (this.connectionFlag) {
			List<Department> departmentList = new ArrayList<Department>();  // List that will contain all retrieved Departments
			MongoCollection<Document> departments = this.db.getCollection("departamento"); // Get collection from database
			try (MongoCursor<Document> cursor = departments.find().sort(Sorts.ascending("depno")).iterator()) { // Create MongoCursor and iterate to add the transformed Document into the list
				while (cursor.hasNext()) {
					Document deptDoc = cursor.next();
					departmentList.add(this.fromDocumentToDepartment(deptDoc));
				}
			}
			return departmentList; // Return the filled list
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public Department findDepartmentById(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> departments = this.db.getCollection("departamento"); // Get collection from database
			Document doc = departments.find(eq("depno", id)).first(); // Query for the Document
			// Check if the query Document value is null
			if (doc == null) {
				return null;
			}
			Department dept = this.fromDocumentToDepartment(doc); // Transform the Document into Department
			return dept; // Return Department
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public void addDepartment(Department department) {
		if (this.connectionFlag) {
			MongoCollection<Document> departments = this.db.getCollection("departamento"); // Get collection from database
			Document doc = new Document() // Create Document object and append all data
					.append("depno", department.getDepno()).append("nombre", department.getName()).append("ubicacion", department.getLocation());
			departments.insertOne(doc); // Execute insert operation
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from IDAO interface
	@Override
	public Department updateDepartment(Object id) {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			MongoCollection<Document> departments = this.db.getCollection("departamento");
			Document doc = departments.find(eq("depno", id)).first(); // Get Department's Document to be updated
			try { // Ask the user for all required data to update the Employee
				System.out.println("Insert updated Department's NAME:");
				System.out.print(USER_INPUT);
				String updateName = reader.readLine();
				if (updateName.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return null;
				}
				System.out.println("Insert updated Department's LOCATION:");
				System.out.print(USER_INPUT);
				String updateLocation = reader.readLine();
				if (updateLocation.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return null;
				}

				// Everything is good -> Update information in DB and create Department object to be returned
				Bson updates = Updates.combine( // Specify new updates to the Document
						Updates.set("nombre", updateName), Updates.set("ubicacion", updateLocation));
				UpdateResult result = departments.updateOne(doc, updates); // Execute update operation
				System.out.printf("Department with DEPNO %s updated successfully!!\n->%s\n", id, result.getModifiedCount());
				return new Department((Integer) id, updateName, updateLocation); // Return Department object
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
				return null;
			} catch (MongoException me) {
				System.err.println("ERROR: MongoException error reported: " + me);
				return null;
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public Department deleteDepartment(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> departments = this.db.getCollection("departamento"); // Get collection from database
			Department deletedDepartment = this.fromDocumentToDepartment(Objects.requireNonNull(departments.find(eq("depno", id)).first())); // This is checked not to be null
			Bson query = eq("depno", id); // Query equality filter
			DeleteResult result = departments.deleteOne(query); // Execute delete operation
			System.out.println("Department deleted! " + result.getDeletedCount()); // Print deleted items just as a visual check
			// Return deleted Employee object
			return deletedDepartment;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from IDAO interface
	@Override
	public List<Employee> findEmployeesByDept(Object idDept) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado"); // Get collection from database
			Bson equals = eq("depno", idDept); // Query equality filter
			List<Employee> departmentEmployees = new ArrayList<Employee>(); // Create Employees list
			try (MongoCursor<Document> cursor = employees.find(equals).iterator()) { // Loop through matches and store the Employee object in the list
				while (cursor.hasNext()) {
					Document doc = cursor.next();
					departmentEmployees.add(this.fromDocumentToEmployee(doc));
				}
			}
			return departmentEmployees; // Return the filled list
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	// Implementation from ConnectionInterface interface
	@Override
	public boolean connectDB() {
		// These two lines remove the red messages in the terminal while the program starts
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.SEVERE);
		BufferedReader reader = new BufferedReader(isr); // I do not use the try-with-resources since I'll need the stream for the next operations in the menu
		try {
			// StringBuilder for String connection
			StringBuilder connectionBuilder = new StringBuilder();
			connectionBuilder.append("mongodb://"); // Append DBM

			System.out.println("Insert the Database server IP:"); // DB Server IP
			System.out.print(USER_INPUT);
			String serverIp = reader.readLine();
			if (!ipPattern.matcher(serverIp).matches()) { // Check IP regular expression
				System.err.println("ERROR: The provided IP address is not valid");
				reader.close(); // Close reader
				return false;
			}
			System.out.println("Insert the Database server PORT:"); // DB Server PORT
			System.out.print(USER_INPUT);
			String serverPort = reader.readLine();
			if (!serverPort.matches("\\d+")) { // Check for bad server port input
				System.err.println("ERROR: The provided port is not valid");
				reader.close(); // Close reader
				return false;
			}
			System.out.println("Insert the Database NAME (case sensitive!):"); // DB Name
			System.out.print(USER_INPUT);
			String dbName = reader.readLine();
			System.out.println("LOGIN: indicate the username and password"); // DB Server IP
			System.out.print("username> ");
			String username = reader.readLine();
			System.out.print("password> ");
			String passwd = reader.readLine(); // TODO -> Do not show the password while user types
			// ? Database Name, username and password are not checked to be null because I don't consider it necessary

			// Build the ConnectionInterface String
			connectionBuilder.append(username).append(":").append(passwd).append("@").append(serverIp).append(":").append(serverPort);
			ConnectionString connectionString = new ConnectionString(connectionBuilder.toString());

			// Try to connect the DB and create the client and database Objects
			MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).build();
			this.client = MongoClients.create(settings);
			this.db = this.client.getDatabase(dbName);
			// Test the connection (not necessary but recommended)
			Bson cmd = new BsonDocument("ping", new BsonInt64(1));
			Document cmdResult = db.runCommand(cmd);
			if (cmdResult == null) {
				System.err.println("ERROR: Server is not oline");
				reader.close(); // Close reader
				return false;
			}
			this.connectionFlag = true;
			return true;
			// TODO
			//! BUG!! The DB name must be checked before executing the menu or setting connectionFlag to true
		} catch (MongoException me) { // Catch MongoException
			System.err.println("ERROR: MongoException error reported: " + me.getMessage());
		} catch (IOException ioe) {
			System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
		}
		return false;
	}

	// Implementation from ConnectionInterface interface
	@Override
	public void closeConnection() {
		if (this.connectionFlag) {
			this.client.close(); // Execute close method
			System.out.printf("%s- Database connection closed -%s\n", GREEN_FONT, RESET);
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeMenu() {
		BufferedReader reader = new BufferedReader(this.isr); // At this point the Stream is still opened -> At finally block I'll close it
		try {
			while (this.executionFlag) {
				System.out.printf("%s%s- WELCOME TO THE COMPANY -%s\n", "\u001B[46m", BLACK_FONT, RESET);
				System.out.println("Select an option:" + "\n\t1) List all Employees" + "\n\t2) Find Employee by its ID" + "\n\t3) Add new Employee" + "\n\t4) Update Employee" + "\n\t5) Delete Employee" + "\n\t6) List all Departments" + "\n\t7) Find Department by its ID" + "\n\t8) Add new Department" + "\n\t9) Update Department" + "\n\t10) Delete Department" + "\n\t11) Find Employees by Department" + "\n\t0) Exit program");
				System.out.print(USER_INPUT);
				String optStr = reader.readLine(); // Read user input and check its value for bad inputs
				if (optStr.isEmpty()) {
					System.err.println("ERROR: Please indicate the option number");
					continue;
				} else if (!optStr.matches("\\d{1,2}")) {
					System.err.println("ERROR: Please provide a valid input for option! The input must be an Integer value");
					continue;
				}
				int opt = Integer.parseInt(optStr);
				switch (opt) { // Execute corresponding method for user input
					case 1 -> executeFindAllEmployees();
					case 2 -> executeFindEmployeeByID();
					case 3 -> executeAddEmployee();
					case 4 -> executeUpdateEmployee();
					case 5 -> executeDeleteEmployee();
					case 6 -> executeFindAllDepartments();
					case 7 -> executeFindDepartmentByID();
					case 8 -> executeAddDepartment();
					case 9 -> executeUpdateDepartment();
					case 10 -> executeDeleteDepartment();
					case 11 -> executeFindEmployeesByDept();
					case 0 -> this.executionFlag = false;
					default -> System.err.println("Please provide a valid option");
				}
			}
		} catch (IOException ioe) {
			System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
		} finally {
			try {
				reader.close(); // Close reader
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error on reader close reported: " + ioe.getMessage());
			}
			closeConnection(); // Close connection method
		}
		System.out.printf("%s%s- SEE YOU SOON -%s\n", "\u001B[46m", BLACK_FONT, RESET); // Program execution end
	}

	// Implementation from Menu interface
	@Override
	public void executeFindAllEmployees() {
		if (this.connectionFlag) {
			String row = "+" + "-".repeat(7) + "+" + "-".repeat(16) + "+" + "-".repeat(16) + "+" + "-".repeat(7) + "+";
			List<Employee> employees = this.findAllEmployees(); // Get the Employees list
			if (employees != null) { // Check if the returned list is not null
				System.out.println(row);
				System.out.printf("| %-5s | %-14s | %-14s | %-5s |\n", "EMPNO", "NOMBRE", "PUESTO", "DEPNO");
				System.out.println(row);
				for (Employee e : employees) {
					System.out.printf("| %-5s | %-14s | %-14s | %-5s |\n", e.getEmpno(), e.getName(), e.getPosition(), e.getDepno());
				}
				System.out.println(row);
			} else {
				System.out.println("There are currently no Employees stored");
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeFindEmployeeByID() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input)); // Get the Employee object by querying it by the ID
				if (returnEmp != null) {
					System.out.println("Employee's information:");
					System.out.println(returnEmp.toString());
				} else { // There is no Employee with the indicated ID
					System.out.println("There is no Employee with EMPNO " + input);
				}
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeAddEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try { // Ask for all required information to create a new Employee
				System.out.println("Insert new Employee's ID:");
				System.out.print(USER_INPUT);
				String id = reader.readLine();
				if (!id.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				} else if (findEmployeeById(Integer.parseInt(id)) != null) { // There is already an Employee with that ID
					System.err.println("ERROR: There is already an Employee with the same ID");
					return;
				}
				System.out.println("Insert new Employee's NAME:");
				System.out.print(USER_INPUT);
				String name = reader.readLine();
				if (name.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Employee's ROLE:");
				System.out.print(USER_INPUT);
				String role = reader.readLine();
				if (role.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Employee's DEPNO:");
				System.out.print(USER_INPUT);
				String depno = reader.readLine();
				if (!depno.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Departments' ID are Integer values");
					return;
				} else if (findDepartmentById(Integer.parseInt(depno)) == null) { // There is no Department with introduced DEPNO
					System.err.println("ERROR: There is no Department with DEPNO " + depno);
					return;
				}
				// Everything is good to execute the method
				Employee newEmployee = new Employee(Integer.parseInt(id), name, role, Integer.parseInt(depno)); // Create Employee object
				this.addEmployee(newEmployee);
				System.out.printf("%sNew Employee added successfully!%s\n", GREEN_FONT, RESET);
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeUpdateEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input));
				if (returnEmp == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Employee with EMPNO " + input);
					return;
				}
				// Execute IDAO method
				Employee updated = updateEmployee(Integer.parseInt(input));
				System.out.println(updated.toString());
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeDeleteEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input));
				if (returnEmp == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Employee with EMPNO " + input);
					return;
				}
				// Execute IDAO method
				Employee deleted = deleteEmployee(Integer.parseInt(input));
				System.out.println(deleted.toString());
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeFindAllDepartments() {
		if (this.connectionFlag) {
			String row = "+" + "-".repeat(7) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+";
			List<Department> departments = this.findAllDepartments();
			if (departments != null) { // Check if the returned list is null or empty
				System.out.println(row);
				System.out.printf("| %-5s | %-18s | %-14s |\n", "DEPNO", "NOMBRE", "UBICACION");
				System.out.println(row);
				for (Department d : departments) {
					System.out.printf("| %-5s | %-18s | %-14s |\n", d.getDepno(), d.getName(), d.getLocation());
				}
				System.out.println(row);
			} else {
				System.out.println("There are currently no Department stored");
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeFindDepartmentByID() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Department's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				}
				Department returnDept = this.findDepartmentById(Integer.parseInt(input));
				if (returnDept != null) { // Check if the returning Department is null
					System.out.println("Department's information:");
					System.out.println(returnDept.toString());
				} else { // There is no Employee with the indicated ID
					System.out.println("There is no Department with DEPNO " + input);
				}
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeAddDepartment() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert new Department's ID:");
				System.out.print(USER_INPUT);
				String depno = reader.readLine();
				if (!depno.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				} else if (findDepartmentById(Integer.parseInt(depno)) != null) { // There is already an Employee with that ID
					System.err.println("ERROR: There is already an Department with the same ID");
					return;
				}
				System.out.println("Insert new Department's NAME:");
				System.out.print(USER_INPUT);
				String name = reader.readLine();
				if (name.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Department's LOCATION:");
				System.out.print(USER_INPUT);
				String location = reader.readLine();
				if (location.isEmpty()) { // Check for empty input
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				// Everything is good to execute the method
				Department newDepartment = new Department(Integer.parseInt(depno), name, location); // Create Employee object
				this.addDepartment(newDepartment);
				System.out.printf("%sNew Department added successfully!%s\n", GREEN_FONT, RESET);
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeUpdateDepartment() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Department's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				}
				Department returnDept = this.findDepartmentById(Integer.parseInt(input));
				if (returnDept == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Department with DEPNO " + input);
					return;
				}
				// Execute IDAO method
				Department updated = updateDepartment(Integer.parseInt(input));
				System.out.println(updated.toString());
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeDeleteDepartment() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Department's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				}
				Department returnDept = this.findDepartmentById(Integer.parseInt(input));
				if (returnDept == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Department with DEPNO " + input);
					return;
				}
				// Execute IDAO method
				Department deleted = deleteDepartment(Integer.parseInt(input));
				System.out.println(deleted.toString());
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	// Implementation from Menu interface
	@Override
	public void executeFindEmployeesByDept() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Department's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if (!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				}
				Department returnDept = this.findDepartmentById(Integer.parseInt(input));
				if (returnDept == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Department with DEPNO " + input);
					return;
				}
				// Execute IDAO method
				ArrayList<Employee> departmentEmployees = (ArrayList<Employee>) findEmployeesByDept(Integer.parseInt(input));
				String row = "+" + "-".repeat(7) + "+" + "-".repeat(16) + "+" + "-".repeat(16) + "+";
				if (departmentEmployees == null || departmentEmployees.isEmpty()) { // No Employees in Department case
					System.out.println("There are currently no Employees in the Department");
				} else {
					System.out.println(row);
					System.out.printf("| %-5s | %-14s | %-14s |\n", "EMPNO", "NOMBRE", "PUESTO");
					System.out.println(row);
					for (Employee e : departmentEmployees) {
						System.out.printf("| %-5s | %-14s | %-14s |\n", e.getEmpno(), e.getName(), e.getPosition());
					}
					System.out.println(row);
				}
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}
}
