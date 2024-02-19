package cesur.accesodatos.mongodb;

public interface Connection {
	public boolean connectDB();
	public void closeConnection();
}
