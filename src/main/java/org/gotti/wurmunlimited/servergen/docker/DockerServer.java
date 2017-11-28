package org.gotti.wurmunlimited.servergen.docker;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DockerServer {

	private Path mapArchive;

	private Path modsFolder;

	private String dockerArgs;

	private String baseImage;

	private String imageName;

	private String serverName;

	private String containerId;

	private String containerArgs;

	public DockerServer(Path mapArchive, Path modsFolder, String dockerArgs, String baseImage, String imageName, String serverName, String containerArgs) {
		this.mapArchive = mapArchive;
		this.modsFolder = modsFolder;
		this.dockerArgs = dockerArgs;
		this.baseImage = baseImage;
		this.imageName = imageName;
		this.serverName = serverName;
		this.containerArgs = containerArgs;
	}
	
	private static class StreamCopy implements Runnable {
		private InputStream source;
		private OutputStream dest;

		public StreamCopy(InputStream source, OutputStream dest) {
			this.source = source;
			this.dest = dest;
		}

		@Override
		public void run() {
			try {
				if (source != null) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = source.read(buffer)) >= 0) {
						dest.write(buffer, 0, len);
						dest.flush();
					}
					source.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void build() {
		try {
			Path tempDir = Files.createTempDirectory("servergen");
			try {
				
				unzipArchive(mapArchive, tempDir);
				copyFiles(modsFolder, tempDir.resolve("mods"));
				copyResource("Dockerfile", tempDir.resolve("Dockerfile"));
				
				final String cmd = String.format("docker %s build --build-arg baseimage=\"%s\" --build-arg servername=\"%s\" -t \"%s\" \"%s\"", dockerArgs, baseImage, serverName, imageName, tempDir.toAbsolutePath().toString());
				System.out.println(cmd);
				Process build = Runtime.getRuntime().exec(cmd);
				
				new Thread(new StreamCopy(build.getInputStream(), System.out)).start();
				new Thread(new StreamCopy(build.getErrorStream(), System.out)).start();
				build.waitFor();
			} finally {
				deleteDirectory(tempDir);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		try {
			final String cmd = String.format("docker %1$s run -d %3$s %2$s", dockerArgs, imageName, containerArgs);
			System.out.println(cmd);
			Process build = Runtime.getRuntime().exec(cmd);
			
			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			
			Thread reader = new Thread(new StreamCopy(build.getInputStream(), stdout));
			reader.start();
			new Thread(new StreamCopy(build.getErrorStream(), System.out)).start();
			build.waitFor();
			reader.join();
			
			this.containerId = new String(stdout.toByteArray(), StandardCharsets.UTF_8).trim();
			System.out.println(this.containerId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		try {
			if (containerId != null) {
				final String cmd = String.format("docker %1$s stop %2$s", dockerArgs, containerId);
				System.out.println(cmd);
				Process build = Runtime.getRuntime().exec(cmd);
				
				new Thread(new StreamCopy(build.getInputStream(), System.out)).start();
				new Thread(new StreamCopy(build.getErrorStream(), System.out)).start();
				build.waitFor();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void unzipArchive(Path archive, Path destDir) throws IOException {
		try (InputStream is = Files.newInputStream(archive); ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry entry; 
			while ((entry = zis.getNextEntry()) != null) {
				Path target = destDir.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(target);
				} else {
					Files.createDirectories(target.getParent());
					Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
				}
			};
		}
	}
	
	private static void copyFiles(Path source, Path destDir) throws IOException {
		if (Files.exists(source)) {
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
					Files.copy(path, destDir.resolve(source.relativize(path)));
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
					Files.createDirectories(destDir.resolve(source.relativize(path)));
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
					Files.delete(path);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
					Files.delete(directory);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
	private void copyResource(String resourceName, Path destFile) throws IOException {
		URL resource = getClass().getResource(resourceName);
		if (resource == null) {
			throw new FileNotFoundException(resourceName);
		}
		try (InputStream is = resource.openStream()) {
			Files.copy(is, destFile);
		}
	}
}
