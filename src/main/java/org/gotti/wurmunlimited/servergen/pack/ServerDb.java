package org.gotti.wurmunlimited.servergen.pack;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ServerDb implements AutoCloseable {

	private Connection connection;

	private ServerDb() {
	}

	public Connection getConnection() {
		return connection;
	}

	@Override
	public void close() throws SQLException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	private static String getDbUrl(final Path dbFile) {
		return String.format("jdbc:sqlite:%s", dbFile.toAbsolutePath().toString().replace('\\', '/'));
	}

	public void importSQL(Reader reader) throws SQLException {
		try (Scanner s = new Scanner(reader); Statement st = connection.createStatement()) {
			s.useDelimiter("(;(\r)?\n)|(--\n)");
			while (s.hasNext()) {
				String line = s.next();
				if (line.startsWith("/*!") && line.endsWith("*/")) {
					int i = line.indexOf(' ');
					line = line.substring(i + 1, line.length() - " */".length());
				}

				if (line.trim().length() > 0) {
					st.execute(line);
				}
			}
		}
	}

	public static ServerDb create(ServerDbs dbType, Path dbFile) throws IOException, SQLException {
		ServerDb serverDb = new ServerDb();
		String url = getDbUrl(dbFile);
		serverDb.connection = DriverManager.getConnection(url);
		return serverDb;
	}

}
