package com.amberclient.modules.render.xray;

import com.amberclient.mixins.accessors.CountPlacementModifierAccessor;
import com.amberclient.mixins.accessors.HeightRangePlacementModifierAccessor;
import com.amberclient.mixins.accessors.RarityFilterPlacementModifierAccessor;
import com.amberclient.utils.general.Dimension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Ore {

    private static final Map<OreType, Setting<Boolean>> oreSettings = new HashMap<>();

    static {
        for (OreType oreType : OreType.values()) {
            oreSettings.put(oreType, new BoolSetting.Builder().name(oreType.name).build());
        }
    }

    public static Map<RegistryKey<Biome>, List<Ore>> getRegistry(Dimension dimension) {
        RegistryWrapper.WrapperLookup registry = BuiltinRegistries.createWrapperLookup();
        RegistryWrapper.Impl<PlacedFeature> features = registry.getOrThrow(RegistryKeys.PLACED_FEATURE);
        var reg = registry.getOrThrow(RegistryKeys.WORLD_PRESET).getOrThrow(WorldPresets.DEFAULT).value().createDimensionsRegistryHolder().dimensions();

        var dim = switch (dimension) {
            case Overworld -> reg.get(DimensionOptions.OVERWORLD);
            case Nether -> reg.get(DimensionOptions.NETHER);
            case End -> reg.get(DimensionOptions.END);
        };

        var biomes = dim.chunkGenerator().getBiomeSource().getBiomes();
        var biomes1 = biomes.stream().toList();

        List<PlacedFeatureIndexer.IndexedFeatures> indexer = PlacedFeatureIndexer.collectIndexedFeatures(
                biomes1, biomeEntry -> biomeEntry.value().getGenerationSettings().getFeatures(), true
        );

        Map<PlacedFeature, OreType> featureToOreType = new HashMap<>();
        for (OreType oreType : OreType.values()) {
            for (RegistryKey<PlacedFeature> key : oreType.placedFeatures) {
                PlacedFeature feature = features.getOrThrow(key).value();
                featureToOreType.put(feature, oreType);
            }
        }

        Map<PlacedFeature, Ore> featureToOre = new HashMap<>();
        for (OreType oreType : OreType.values()) {
            Setting<Boolean> active = oreSettings.get(oreType);
            int genStep = oreType.name.equals("Quartz") || oreType.name.equals("Ancient Debris") ||
                    (oreType.name.equals("Gold") && Arrays.stream(oreType.placedFeatures)
                            .anyMatch(key -> key == OrePlacedFeatures.ORE_GOLD_NETHER || key == OrePlacedFeatures.ORE_GOLD_DELTAS)) ? 7 : 6;
            for (RegistryKey<PlacedFeature> oreKey : oreType.placedFeatures) {
                registerOre(featureToOre, indexer, features, oreKey, genStep, active, new Color(oreType.color.getRed(), oreType.color.getGreen(), oreType.color.getBlue()));
            }
        }

        Map<RegistryKey<Biome>, List<Ore>> biomeOreMap = new HashMap<>();
        biomes1.forEach(biome -> {
            biomeOreMap.put(biome.getKey().get(), new ArrayList<>());
            biome.value().getGenerationSettings().getFeatures().stream()
                    .flatMap(RegistryEntryList::stream)
                    .map(RegistryEntry::value)
                    .filter(featureToOre::containsKey)
                    .forEach(feature -> biomeOreMap.get(biome.getKey().get()).add(featureToOre.get(feature)));
        });
        return biomeOreMap;
    }

    private static void registerOre(
            Map<PlacedFeature, Ore> map,
            List<PlacedFeatureIndexer.IndexedFeatures> indexer,
            RegistryWrapper.Impl<PlacedFeature> oreRegistry,
            RegistryKey<PlacedFeature> oreKey,
            int genStep,
            Setting<Boolean> active,
            Color color
    ) {
        var orePlacement = oreRegistry.getOrThrow(oreKey).value();
        int index = indexer.get(genStep).indexMapping().applyAsInt(orePlacement);
        Ore ore = new Ore(orePlacement, genStep, index, active, color);
        map.put(orePlacement, ore);
    }

    public int step;
    public int index;
    public Setting<Boolean> active;
    public IntProvider count = ConstantIntProvider.create(1);
    public HeightProvider heightProvider;
    public HeightContext heightContext;
    public float rarity = 1;
    public float discardOnAirChance;
    public int size;
    public Color color;
    public boolean scattered;

    private Ore(PlacedFeature feature, int step, int index, Setting<Boolean> active, Color color) {
        this.step = step;
        this.index = index;
        this.active = active;
        this.color = color;
        int bottom = MinecraftClient.getInstance().world.getBottomY();
        int height = MinecraftClient.getInstance().world.getDimension().logicalHeight();
        this.heightContext = new HeightContext(null, HeightLimitView.create(bottom, height));

        for (PlacementModifier modifier : feature.placementModifiers()) {
            if (modifier instanceof CountPlacementModifier) {
                this.count = ((CountPlacementModifierAccessor) modifier).getCount();
            } else if (modifier instanceof HeightRangePlacementModifier) {
                this.heightProvider = ((HeightRangePlacementModifierAccessor) modifier).getHeight();
            } else if (modifier instanceof RarityFilterPlacementModifier) {
                this.rarity = ((RarityFilterPlacementModifierAccessor) modifier).getChance();
            }
        }

        FeatureConfig featureConfig = feature.feature().value().config();
        if (featureConfig instanceof OreFeatureConfig oreFeatureConfig) {
            this.discardOnAirChance = oreFeatureConfig.discardOnAirChance;
            this.size = oreFeatureConfig.size;
        } else {
            throw new IllegalStateException("config for " + feature + "is not OreFeatureConfig.class");
        }

        if (feature.feature().value().feature() instanceof ScatteredOreFeature) {
            this.scattered = true;
        }
    }
}