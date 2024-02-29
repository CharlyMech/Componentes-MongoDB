package cesur.accesodatos.mongodb;

import org.bson.Document;

public class Department {
	// Class variables
	private Integer depno;
	private String name;
	private String location;

	// Constructors
	public Department(Integer depno, String name, String location) {
		this.depno = depno;
		this.name = name;
		this.location = location;
	}

	public Department() {
	}

	// GETTERS //
	public int getDepno() {
		return depno;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	// SETTERS //
	public void setDepno(int depno) {
		this.depno = depno;
	}

	public void setName(String name) {
		this.name = name;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	// TO STRING //
	@Override
	public String toString() {
		return "Department{" +
				"depno=" + depno +
				", name='" + name + '\'' +
				", location='" + location + '\'' +
				'}';
	}
}
