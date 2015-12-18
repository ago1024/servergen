package org.gotti.wurmunlimited.servergen;

import java.util.Collection;
import java.util.LinkedList;

import com.wyverngame.terraingenerator.Map;
import com.wyverngame.terraingenerator.filter.AverageFilter;
import com.wyverngame.terraingenerator.filter.BiomeFilter;
import com.wyverngame.terraingenerator.filter.DeltaFilter;
import com.wyverngame.terraingenerator.filter.EdgeFilter;
import com.wyverngame.terraingenerator.filter.FeatureMountainFilter;
import com.wyverngame.terraingenerator.filter.ForestFilter;
import com.wyverngame.terraingenerator.filter.GrassDecoratorFilter;
import com.wyverngame.terraingenerator.filter.MountainFilter;
import com.wyverngame.terraingenerator.filter.NoiseFilter;
import com.wyverngame.terraingenerator.filter.OceanFilter;
import com.wyverngame.terraingenerator.filter.OreFilter;
import com.wyverngame.terraingenerator.filter.ResourceFilter;
import com.wyverngame.terraingenerator.filter.RidgeFilter;
import com.wyverngame.terraingenerator.filter.RiverFilter;
import com.wyverngame.terraingenerator.filter.RockDecoratorFilter;
import com.wyverngame.terraingenerator.filter.RockLayerFilter;
import com.wyverngame.terraingenerator.filter.SmoothCoastFilter;
import com.wyverngame.terraingenerator.filter.TarPitFilter;

public class Terragen {
	
	@FunctionalInterface
	public interface ProgressListener {
		public void notify(int progress, String message);
	}
	
	private Collection<ProgressListener> listeners = new LinkedList<>();
	
	public Terragen() {
		
	}
	
	public void addListener(ProgressListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(ProgressListener listener) {
		listeners.remove(listener);
	}
	
	private void fireProgress(Map map, int progress, String message) {
		listeners.forEach(listener -> listener.notify(progress, message));
	}
	
	public Map generate(int size, long seed) {
		fireProgress(null, 1, "Generating " + size + "x" + size + " map with seed " + seed);
		Map map = new Map(size, seed);
		
		new DeltaFilter().apply(map);
		
		fireProgress(map, 5, " Generating mountains...");
		new MountainFilter(size >= 4096).apply(map);
		
		fireProgress(map, 22, " Generating ridged mountains...");
		new RidgeFilter(size >= 4096).apply(map);
		
		fireProgress(map, 42, " Generating noise...");
		new NoiseFilter().apply(map);
		
		fireProgress(map, 52, " Generating oceans...");
		new OceanFilter(size <= 2048).apply(map);
		
		fireProgress(map, 55, " Smoothing terrain...");
		new AverageFilter().apply(map);
		
		fireProgress(map, 60, " Generating rives and lakes...");
		new RiverFilter().apply(map);
		
		if (size >= 4096) {
			fireProgress(map, 65, " Generating peaks...");
			new FeatureMountainFilter().apply(map);
		}
		
		fireProgress(map, 70, " Lowering coastlines...");
		if (size == 512) {
			new EdgeFilter(96).apply(map);	
		} else if (size <= 2048) {
			new EdgeFilter(128).apply(map);
		} else {
			new EdgeFilter(256).apply(map);
		}
		
		fireProgress(map, 75, " Smoothing coastlines...");
		new SmoothCoastFilter().apply(map);
		
		fireProgress(map, 80, " Generating rock layer...");
		new RockLayerFilter().apply(map);
		
		fireProgress(map, 82, " Generating rocks and cliffs...");
		new RockDecoratorFilter().apply(map);
		
		fireProgress(map, 84, " Generating deserts...");
		new BiomeFilter(1, 4639).apply(map);
		
		fireProgress(map, 86, " Generating tundra...");
		new BiomeFilter(19, 3748).apply(map);
		
		fireProgress(map, 88, " Generating steppe...");
		new BiomeFilter(22, 8178).apply(map);
		
		fireProgress(map, 90, " Generating clay...");
		new ResourceFilter(6, 6578).apply(map);
		
		fireProgress(map, 91, " Generating peat...");
		new ResourceFilter(18, 8739).apply(map);
		
		fireProgress(map, 92, " Generating moss...");
		new ResourceFilter(20, 1043).apply(map);
		
		fireProgress(map, 93, " Generating grass and flowers...");
		new GrassDecoratorFilter().apply(map);
		
		fireProgress(map, 94, " Generating tar pits...");
		new TarPitFilter().apply(map);
		
		fireProgress(map, 95, " Generating forests...");
		new ForestFilter().apply(map);
		
		fireProgress(map, 96, " Generating ores...");
		new OreFilter().apply(map);
		
		fireProgress(map, 100, " Done...");
		return map;
	}

}
