package cesur.accesodatos.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBDAO implements IDAO, ConnectionInterface, Menu {
	// Terminal outputs and colors
	static final String BLACK_FONT = "\u001B[30m";
	static final String GREEN_FONT = "\u001B[32m";
	static final String WHITE_BG = "\u001B[47m";
	static final String RESET = "\u001B[0m";
	static final String USER_INPUT = String.format("%s%s>%s ", BLACK_FONT, WHITE_BG, RESET);
	// Class variables
	private boolean connectionFlag = false;
	private boolean executionFlag = true;
	private final InputStreamReader isr = new InputStreamReader(System.in);
	private final Pattern ipPattern = Pattern.compile("(localhost)|(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)");
	private MongoClient client;
	private MongoDatabase db;

	// Constructor
	public MongoDBDAO() {
	}

	@Override
	public List<Employee> findAllEmployees() {
		List<Employee> employeeList = new ArrayList<Employee>();
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado");
			try (MongoCursor<Document> cursor = employees.find().sort(Sorts.ascending("empno")).iterator()) {
				while (cursor.hasNext()) {
					Document empDoc = cursor.next();
					employeeList.add((new Employee()).fromDocumentToEmployee(empDoc));
				}
			}
			return employeeList;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public Employee findEmployeeById(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado");
			Document doc = employees.find(eq("empno", id)).first();
			if (doc == null) {
				return null;
			}
			Employee emp = (new Employee()).fromDocumentToEmployee(doc);
			return emp;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public void addEmployee(Employee employee) {
		if (this.connectionFlag) {
			MongoCollection<Document> employees = this.db.getCollection("empleado");
			Document doc = new Document()
					.append("empno", employee.getEmpno())
					.append("nombre", employee.getName())
					.append("puesto", employee.getPosition())
					.append("depno", employee.getDepno());
			employees.insertOne(doc);
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public Employee updateEmployee(Object id) {
		BufferedReader reader = new BufferedReader(this.isr); // To read user input
		MongoCollection<Document> employees = this.db.getCollection("empleado");
		Document doc = employees.find(eq("empno", id)).first(); // Get Employee's Document to be updated
		try {
			if (this.connectionFlag) {
				System.out.println("Insert updated Employee's NAME:");
				System.out.print(USER_INPUT);
				String updateName = reader.readLine();
				if (updateName.isEmpty()) {
					System.err.println("ERROR: You can't leave the information empty");
					return null;
				}
				System.out.println("Insert updated Employee's POSITION:");
				System.out.print(USER_INPUT);
				String updatePosition = reader.readLine();
				if (updatePosition.isEmpty()) {
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
						Updates.set("nombre", updateName),
						Updates.set("puesto", updatePosition),
						Updates.set("depno", Integer.parseInt(updateDepno)));

				UpdateResult result = employees.updateOne(doc, updates); // Execute update operation
				System.out.printf("Emplyee with EMPNO %s updated successfully!!\n->%s\n",id , result.getModifiedCount());
				return new Employee((Integer) id, updateName, updatePosition, Integer.parseInt(updateDepno));
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

	@Override
	public Employee deleteEmployee(Object id) {
		if (this.connectionFlag) {
			return null;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public List<Department> findAllDepartments() {
		if (this.connectionFlag) {
			List<Department> departmentList = new ArrayList<Department>();
			if (this.connectionFlag) {
				MongoCollection<Document> departments = this.db.getCollection("departamento");
				try (MongoCursor<Document> cursor = departments.find().sort(Sorts.ascending("depno")).iterator()) {
					while (cursor.hasNext()) {
						Document deptDoc = cursor.next();
						departmentList.add((new Department()).fromDocumentToDepartment(deptDoc));
					}
				}
				return departmentList;
			} else {
				System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
				return null;
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public Department findDepartmentById(Object id) {
		if (this.connectionFlag) {
			MongoCollection<Document> departments = this.db.getCollection("departamento");
			Document doc = departments.find(eq("depno", id)).first();
			if (doc == null) {
				return null;
			}
			Department dept = (new Department()).fromDocumentToDepartment(doc);
			return dept;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public void addDepartment(Department department) {
		if (this.connectionFlag) {
			MongoCollection<Document> departments = this.db.getCollection("departamento");
			Document doc = new Document()
					.append("depno", department.getDepno())
					.append("nombre", department.getName())
					.append("ubicacion", department.getLocation());
			departments.insertOne(doc);
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public Department updateDepartment(Object id) {
		if (this.connectionFlag) {
			return null;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public Department deleteDepartment(Object id) {
		if (this.connectionFlag) {
			return null;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public List<Employee> findEmployeesByDept(Object idDept) {
		if (this.connectionFlag) {
			return null;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public boolean connectDB() {
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
			if (!ipPattern.matcher(serverIp).matches()) {
				System.err.println("ERROR: The provided IP address is not valid");
				reader.close(); // Close reader
				return false;
			}
			System.out.println("Insert the Database server PORT:"); // DB Server PORT
			System.out.print(USER_INPUT);
			String serverPort = reader.readLine();
			System.out.println("Insert the Database NAME (case sensitive!):"); // DB Name
			System.out.print(USER_INPUT);
			String dbName = reader.readLine();
			System.out.println("LOGIN: indicate the username and password"); // DB Server IP
			System.out.print("username> ");
			String username = reader.readLine();
			System.out.print("password> ");
			String passwd = reader.readLine(); // TODO -> Do not show the password while user types

			// Build the ConnectionInterface String
			connectionBuilder
					.append(username).append(":").append(passwd)
					.append("@")
					.append(serverIp).append(":").append(serverPort);
			ConnectionString connectionString = new ConnectionString(connectionBuilder.toString());

			// Try to connect the DB and create the client and database Objects
			MongoClientSettings settings = MongoClientSettings.builder()
					.applyConnectionString(connectionString)
					.build();
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

	@Override
	public void closeConnection() {
		if (this.connectionFlag) {
			this.client.close();
			System.out.printf("%s- Database connection closed -%s\n", GREEN_FONT, RESET);
		}
	}

	@Override
	public void executeMenu() {
		BufferedReader reader = new BufferedReader(this.isr); // At this point the Stream is still opened -> At finally block I'll close it
		try {
			while (this.executionFlag) {
				System.out.printf("%s%s- WELCOME TO THE COMPANY -%s\n", "\u001B[46m", BLACK_FONT, RESET);
				System.out.println("Select an option:" +
						"\n\t1) List all Employees" +
						"\n\t2) Find Employee by its ID" +
						"\n\t3) Add new Employee" +
						"\n\t4) Update Employee" +
						"\n\t5) Delete Employee" +
						"\n\t6) List all Departments" +
						"\n\t7) Find Department by its ID" +
						"\n\t8) Add new Department" +
						"\n\t9) Update Department" +
						"\n\t10) Delete Department" +
						"\n\t11) Find Employees by Department" +
						"\n\t0) Exit program");
				System.out.print(USER_INPUT);
				String optStr = reader.readLine();
				if (optStr.isEmpty()) {
					System.err.println("ERROR: Please indicate the option number");
					continue;
				} else if(!optStr.matches("\\d{1,2}")) {
					System.err.println("ERROR: Please provide a valid input for option! The input must be an Integer value");
					continue;
				}
				int opt = Integer.parseInt(optStr);
				switch (opt) {
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
				reader.close();
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error on reader close reported: " + ioe.getMessage());
			}
			closeConnection();
		}
		System.out.printf("%s%s- SEE YOU SOON -%s\n", "\u001B[46m", BLACK_FONT, RESET); // Program execution end
	}

	@Override
	public void executeFindAllEmployees() {
		if (this.connectionFlag) {
			String row = "+" + "-".repeat(7) + "+" + "-".repeat(16) + "+" + "-".repeat(16) + "+" + "-".repeat(7) + "+";
			List<Employee> employees = this.findAllEmployees();
			if(employees != null) {
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

	@Override
	public void executeFindEmployeeByID() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if(!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input));
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

	@Override
	public void executeAddEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert new Employee's ID:");
				System.out.print(USER_INPUT);
				String id = reader.readLine();
				if(!id.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				} else if (findEmployeeById(Integer.parseInt(id)) != null) { // There is already an Employee with that ID
					System.err.println("ERROR: There is already an Employee with the same ID");
					return;
				}
				System.out.println("Insert new Employee's NAME:");
				System.out.print(USER_INPUT);
				String name = reader.readLine();
				if(name.isEmpty()) {
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Employee's ROLE:");
				System.out.print(USER_INPUT);
				String role = reader.readLine();
				if(role.isEmpty()) {
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Employee's DEPNO:");
				System.out.print(USER_INPUT);
				String depno = reader.readLine();
				if(!depno.matches("\\d+")) { // Check if the output is not numeric
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

	@Override
	public void executeUpdateEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if(!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input));
				if (returnEmp == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Employee with EMPNO " + input);
					return;
				}
				// Execute IDAO method
				updateEmployee(Integer.parseInt(input));
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public void executeDeleteEmployee() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Employee's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if(!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Employee ID. Employee's ID are Integer values");
					return;
				}
				Employee returnEmp = this.findEmployeeById(Integer.parseInt(input));
				if (returnEmp == null) { // Check if there is an Employee with the indicated ID
					System.out.println("There is no Employee with EMPNO " + input);
					return;
				}
				// Execute IDAO method
				deleteEmployee(Integer.parseInt(input));
			} catch (IOException ioe) {
				System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
			}
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public void executeFindAllDepartments() {
		if (this.connectionFlag) {
			String row = "+" + "-".repeat(7) + "+" + "-".repeat(20) + "+" + "-".repeat(16) + "+";
			List<Department> departments = this.findAllDepartments();
			if(departments != null) {
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

	@Override
	public void executeFindDepartmentByID() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert Department's ID:");
				System.out.print(USER_INPUT);
				String input = reader.readLine();
				if(!input.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				}
				Department returnDept = this.findDepartmentById(Integer.parseInt(input));
				if (returnDept != null) {
					System.out.println("Department's information:");
					System.out.println(returnDept.toString());
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

	@Override
	public void executeAddDepartment() {
		if (this.connectionFlag) {
			BufferedReader reader = new BufferedReader(this.isr); // To read user input
			try {
				System.out.println("Insert new Department's ID:");
				System.out.print(USER_INPUT);
				String depno = reader.readLine();
				if(!depno.matches("\\d+")) { // Check if the output is not numeric
					System.err.println("ERROR: Please provide a valid Department ID. Department's ID are Integer values");
					return;
				} else if (findDepartmentById(Integer.parseInt(depno)) != null) { // There is already an Employee with that ID
					System.err.println("ERROR: There is already an Employee with the same ID");
					return;
				}
				System.out.println("Insert new Department's NAME:");
				System.out.print(USER_INPUT);
				String name = reader.readLine();
				if(name.isEmpty()) {
					System.err.println("ERROR: You can't leave the information empty");
					return;
				}
				System.out.println("Insert new Department's LOCATION:");
				System.out.print(USER_INPUT);
				String location = reader.readLine();
				if(location.isEmpty()) {
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

	@Override
	public void executeUpdateDepartment() {
		if (this.connectionFlag) {
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public void executeDeleteDepartment() {
		if (this.connectionFlag) {
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}

	@Override
	public void executeFindEmployeesByDept() {
		if (this.connectionFlag) {
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
		}
	}
}
