package org.gotti.wurmunlimited.servergen.pack;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.wyverngame.terraingenerator.Map;

public class ServerBuilder {

	private HashMap<ServerDbs, ServerDb> serverDbs = new HashMap<>();
	private HashMap<ServerDbs, Path> dbFiles = new HashMap<>();

	private Path rootDir;

	public ServerBuilder(Path rootDir) {
		this.rootDir = rootDir;
	}

	public void prepareServer(String name, Map map) {

		Path serverDir = rootDir.resolve(name);
		Path sqliteDir = serverDir.resolve("sqlite");

		try {

			Files.createDirectories(serverDir);
			Files.createDirectories(sqliteDir);

			map.save(serverDir);

			try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(serverDir.resolve(name + ".png")), 65536)) {
				ImageIO.write(map.getSurface().render(), "PNG", out);
			}

			copy("ini/wurm.ini", serverDir.resolve("wurm.ini"));
			copy("ini/gamedir", serverDir.resolve("gamedir"));

			try {
				for (ServerDbs serverDb : ServerDbs.values()) {
					String dbName = serverDb.getDbName();
					dbFiles.put(serverDb, Files.createTempFile(dbName, ".db"));
					serverDbs.put(serverDb ,ServerDb.create(serverDb, dbFiles.get(serverDb)));

					try (InputStream script = getResource("db/" + dbName + ".sql").openStream()) {
						serverDbs.get(serverDb).importSQL(new InputStreamReader(script, StandardCharsets.UTF_8));
					}
					try (InputStream script = getResource("db/insert" + dbName + ".sql").openStream()) {
						serverDbs.get(serverDb).importSQL(new InputStreamReader(script, StandardCharsets.UTF_8));
					}
				}

				try (PreparedStatement prep = serverDbs.get(ServerDbs.LOGIN).getConnection().prepareStatement("update servers set name=?, mapname=?, tilerest=0, polltile=0, pollmod=0, externalip='0.0.0.0' where local=1")) {
					prep.setString(1, name);
					prep.setString(2, name);
					prep.execute();
				}
			} finally {
				for (ServerDbs serverDb : ServerDbs.values()) {
					ServerDb db = serverDbs.get(serverDb);
					Path dbFile = dbFiles.get(serverDb);
					if (db != null) {
						db.close();
					}
					if (db != null && dbFile != null && Files.exists(dbFile)) {
						Files.copy(dbFile, sqliteDir.resolve(serverDb.getDbName() + ".db"));
					}
					if (dbFile != null) {
						Files.delete(dbFile);
					}
				}
			}
		} catch (IOException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private URL getResource(String resourceName) throws IOException {
		URL resource = getClass().getResource(resourceName);
		if (resource == null) {
			throw new FileNotFoundException(resourceName);
		}
		return resource;
	}

	private void copy(String sourceName, Path target) throws IOException {
		URL resource = getResource(sourceName);
		try (InputStream is = resource.openStream()) {
			Files.copy(is, target);
		}
	}
}
