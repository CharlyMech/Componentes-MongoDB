package cesur.accesodatos.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class MongoDBDAO implements IDAO, Connection {
	// Terminal outputs and colors
	static final String BLACK_FONT = "\u001B[30m";
	static final String GREEN_FONT = "\u001B[32m";
	static final String WHITE_BG = "\u001B[47m";
	static final String RESET = "\u001B[0m";
	static final String USER_INPUT = String.format("%s%s>%s ", BLACK_FONT, WHITE_BG, RESET);
	// Class variables
	private final InputStreamReader isr = new InputStreamReader(System.in);
	private boolean flag = false;
	private final Pattern ipPattern = Pattern.compile("(localhost)|(\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b)");
	private MongoDatabase db;

	// Constructor
	public MongoDBDAO() {
	}

	@Override
	public List<Employee> findAllEmployees() {
		if(this.flag) {
			return null;
		} else {
			System.err.println("ERROR: You must first try to connect to the database with the method .connectDB()");
			return null;
		}
	}

	@Override
	public Employee findEmployeeById(Object id) {
		return null;
	}

	@Override
	public void addEmployee(Employee employee) {
	}

	@Override
	public Employee updateEmployee(Object id) {
		return null;
	}

	@Override
	public Employee deleteEmployee(Object id) {
		return null;
	}

	@Override
	public List<Department> findAllDepartments() {
		return null;
	}

	@Override
	public Department findDepartmentById(Object id) {
		return null;
	}

	@Override
	public void addDepartment(Department department) {
	}

	@Override
	public Department updateDepartment(Object id) {
		return null;
	}

	@Override
	public Department deleteDepartment(Object id) {
		return null;
	}

	@Override
	public List<Employee> findEmployeesByDept(Object idDept) {
		return null;
	}

	@Override
	public boolean connectDB() {
		Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
		mongoLogger.setLevel(Level.SEVERE);
		try (BufferedReader reader = new BufferedReader(this.isr)) {
			// StringBuilder for String connection
			StringBuilder connectionBuilder = new StringBuilder();
			connectionBuilder.append("mongodb://"); // Append DBM

			System.out.println("Insert the Database server IP:"); // DB Server IP
			System.out.print(USER_INPUT);
			String serverIp = reader.readLine();
			if (!ipPattern.matcher(serverIp).matches()) {
				System.err.println("ERROR: The provided IP address is not valid");
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

			// Build the Connection String
			connectionBuilder
					.append(username).append(":").append(passwd)
					.append("@")
					.append(serverIp).append(":").append(serverPort);
			ConnectionString connectionString = new ConnectionString(connectionBuilder.toString());

			// Try to connect the DB and create the client and database Objects
			MongoClientSettings settings = MongoClientSettings.builder()
					.applyConnectionString(connectionString)
					.build();
			MongoClient client = MongoClients.create(settings);
			this.db = client.getDatabase(dbName);
			// Test the connection (not necessary but recommended)
			Bson cmd = new BsonDocument("ping", new BsonInt64(1));
			Document cmdResult = db.runCommand(cmd);
			if(cmdResult != null) {
				this.flag = true;
				return true;
			}
		} catch (MongoException me) { // Catch MongoException
			System.err.println("ERROR: MongoException error reported: " + me.getMessage());
		} catch (IOException ioe) {
			System.err.println("ERROR: IOException error reported: " + ioe.getMessage());
		}
		return false;
	}

	@Override
	public void closeConnection() {

	}
}
