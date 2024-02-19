package cesur.accesodatos.mongodb;

public interface ConnectionInterface {
	public boolean connectDB();
	public void closeConnection();
}
