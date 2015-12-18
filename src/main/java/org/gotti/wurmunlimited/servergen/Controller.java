package org.gotti.wurmunlimited.servergen;


import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Random;

import org.gotti.wurmunlimited.servergen.namegen.NameGenerator;
import org.gotti.wurmunlimited.servergen.pack.ServerBuilder;

import com.wyverngame.terraingenerator.Map;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Controller {
	
	@FXML
	private ComboBox<Integer> mapSize;
	
	@FXML
	private TextField mapSeed;
	
	@FXML
	private TextField mapName;
	
	@FXML
	private Button generate;
	
	@FXML
	private Button reroll;
	
	@FXML
	private ProgressBar progressBar;
	
	@FXML
	private Label progressMessage;
	
	@FXML
	private ImageView mapRender;
	
	@FXML
	private TextField zipFolder;
	
	@FXML
	private TextField zipName;
	
	@FXML
	private Button createZip;
	
	@FXML
	private Pane root;
	
	private static final Integer[] mapSizes = { 512, 1024, 2048, 4096, 8192 };

	private ObjectProperty<Map> currentMap = new SimpleObjectProperty<>();
	
	private ObjectProperty<Path> currentMapArchive = new SimpleObjectProperty<>();

	private NameGenerator nameGen;

	private Random random = new Random();
	
	@FXML
	public void initialize() {
		mapSize.getItems().addAll(mapSizes);
		mapSize.getSelectionModel().select(Integer.valueOf(1024));
		
		nameGen = new NameGenerator(this::getFantasyNameRules);
		
		zipName.textProperty().bind(Bindings.concat(zipFolder.textProperty(), "/", mapName.textProperty(), ".zip"));
		
		createZip.disableProperty().bind(currentMap.isNull());
		createZip.textProperty().bind(Bindings.when(currentMapArchive.isNotNull()).then("Created").otherwise("Create"));
		
		rollSeed();
		rollName();
	}
	
	@FXML
	protected void generate(ActionEvent event) {
		generate();
	}
	
	@FXML
	protected void reroll(ActionEvent event) {
		rollSeed();
		rollName();
		generate();
	}
	
	@FXML
	protected void rollSeed(ActionEvent event) {
		rollSeed();
		generate();
	}
	
	private void rollSeed() {
		mapSeed.setText(String.valueOf(random.nextInt()));
	}
	
	@FXML
	protected void rollName(ActionEvent event) {
		rollName();
	}
	
	private void rollName() {
		mapName.setText(nameGen.compose(2 + random.nextInt(3)));
	}

	
	private void generate() {
		currentMap.set(null);
		currentMapArchive.set(null);
		setMapRender(null);
		
		int size = mapSize.getSelectionModel().getSelectedItem();
		int seed = Integer.parseInt(mapSeed.getText());
		
		Task<Map> task = new Task<Map>() {
			private void progress(int progress, String message) {
				updateProgress(progress, 100);
				updateMessage(message);
				Thread.yield();
			}
			
			@Override
			protected Map call() throws Exception {
				try {
					root.disableProperty().set(true);
					Terragen terragen = new Terragen();
					terragen.addListener(this::progress);
					Map map = terragen.generate(size, seed);
					
					currentMap.set(map);
					setMapRender(map);
					
					progressBar.progressProperty().unbind();
					progressMessage.textProperty().unbind();
					
					return map;
				} finally {
					root.disableProperty().set(false);
				}
			}
		};
		
		progressBar.progressProperty().bind(task.progressProperty());
		progressMessage.textProperty().bind(task.messageProperty());
		
		new Thread(task).start();
	}
	
	
	private void setMapRender(Map map) {
		if (map != null) {
			BufferedImage surfaceRender = map.getSurface().render();
			mapRender.setImage(SwingFXUtils.toFXImage(surfaceRender, null));
		} else {
			BufferedImage bufferedImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
			mapRender.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
		}
	}
	
	private BufferedReader getFantasyNameRules() {
		try {
			URL resource = getClass().getResource("fantasy.namegen");
			return new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@FXML
	protected void build(ActionEvent event) {
		if (currentMap.get() != null) {
			try {
				Path zipFile = Paths.get(zipName.getText());
				Files.createDirectories(zipFile.getParent());
				URI uri = URI.create("jar:" + zipFile.toUri());
	
				HashMap<String, String> env = new HashMap<>();
				env.put("create", "true");
	
				try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
					new ServerBuilder(zipfs.getPath("/")).prepareServer(mapName.getText(), currentMap.get());
				}
				
				currentMap.set(null);
				currentMapArchive.set(zipFile);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} 
		}
	}
}
