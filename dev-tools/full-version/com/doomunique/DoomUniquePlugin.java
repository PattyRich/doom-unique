package com.doomunique;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Doom Unique Colors",
	description = "Customize the special-loot hole color at Doom of Mokhaiotl",
	tags = {"bosses", "combat", "pve", "loot", "doom"}
)
@Slf4j
public class DoomUniquePlugin extends Plugin implements RenderCallback
{
	private static final int SCAN_INTERVAL_TICKS = 50;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private RenderCallbackManager renderCallbackManager;


	@Inject
	private DoomUniqueConfig config;

	private final Map<TileObject, Set<Model>> objectModels = new IdentityHashMap<>();
	private final Map<TileObject, RuneLiteObject> testSwapObjects = new IdentityHashMap<>();
	private final Map<Model, ModelColors> originalColors = new IdentityHashMap<>();
	private final Set<String> statusMessages = new HashSet<>();

	private Set<Integer> uniqueHoleIds = Collections.emptySet();
	private Set<Integer> testObjectIds = Collections.emptySet();
	private Model uniqueHoleSwapModel;
	private ObjectModelDefinition uniqueHoleSwapDefinition;
	private int ticks;

	@Provides
	DoomUniqueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DoomUniqueConfig.class);
	}

	@Override
	protected void startUp()
	{
		refreshObjectIds();
		renderCallbackManager.register(this);
		clientThread.invokeLater(() ->
		{
			scanSceneForHoles();
			scanSceneForTestSwaps();
		});
	}

	@Override
	protected void shutDown()
	{
		clientThread.invokeLater(() ->
		{
			resetAllModels();
			clearTestSwaps();
			statusMessages.clear();
			uniqueHoleIds = Collections.emptySet();
			testObjectIds = Collections.emptySet();
			ticks = 0;
			renderCallbackManager.unregister(this);
		});
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		handleSceneObjectSafely(object);
		return !shouldHideForTestSwap(object);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			resetAllModels();
			clearTestSwaps();
			statusMessages.clear();
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			scanSceneForHoles();
			scanSceneForTestSwaps();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		ticks++;
		if (ticks % SCAN_INTERVAL_TICKS != 0)
		{
			return;
		}

		scanSceneForHoles();
		scanSceneForTestSwaps();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!DoomUniqueConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			refreshObjectIds();
			resetAllModels();
			clearTestSwaps();
			statusMessages.clear();
			scanSceneForHoles();
			scanSceneForTestSwaps();
		});
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		handleSceneObjectSafely(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		handleSceneObjectSafely(event.getGroundObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		handleSceneObjectSafely(event.getDecorativeObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		handleSceneObjectSafely(event.getWallObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		resetIfTracked(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		resetIfTracked(event.getGroundObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		resetIfTracked(event.getDecorativeObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		resetIfTracked(event.getWallObject());
	}

	private void refreshObjectIds()
	{
		uniqueHoleIds = ObjectIdParser.parse(config.uniqueHoleObjectIds());
		testObjectIds = ObjectIdParser.parse(config.testObjectIds());
	}

	private void scanSceneForHoles()
	{
		if (!isSceneReady() || !hasConfiguredSceneTargets())
		{
			return;
		}

		try
		{
			forEachSceneObject(this::handleSceneObjectSafely);
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.warn("Unable to scan Doom Unique scene targets", ex);
			messageOnce("scene-scan-error", "Scene scan failed once; skipping this scan. See client.log.");
		}
	}

	private void scanSceneForTestSwaps()
	{
		if (!isSceneReady() || !config.swapTestObjects() || testObjectIds.isEmpty())
		{
			clearTestSwaps();
			return;
		}

		try
		{
			Set<TileObject> activeTargets = Collections.newSetFromMap(new IdentityHashMap<>());
			int[] matchedTestObjectIds = new int[1];
			forEachSceneObject(object ->
			{
				if (isConfiguredTestObjectId(object))
				{
					matchedTestObjectIds[0]++;
				}
				if (shouldSwapTestObject(object))
				{
					activeTargets.add(object);
					swapTestObjectIfMatched(object);
				}
			});

			removeInactiveTestSwaps(activeTargets);
			if (activeTargets.isEmpty())
			{
				if (matchedTestObjectIds[0] > 0)
				{
					messageOnce("swap-type-no-match:" + testObjectIds + ":" + config.testObjectType(),
						"Swap IDs matched loaded objects, but none were " + config.testObjectType() + ".");
				}
				else
				{
					messageOnce("swap-no-match:" + testObjectIds,
						"No loaded game objects matched swap test IDs " + testObjectIds + ".");
				}
			}
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.warn("Unable to scan Doom Unique test swaps", ex);
			messageOnce("swap-scan-error", "Swap scan failed once; skipping this scan. See client.log.");
		}
	}

	private boolean hasConfiguredSceneTargets()
	{
		return config.recolorUniqueHole() && !uniqueHoleIds.isEmpty();
	}

	private boolean isSceneReady()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return false;
		}

		WorldView worldView = client.getTopLevelWorldView();
		return worldView != null && worldView.getScene() != null && worldView.getScene().getTiles() != null;
	}

	private void handleSceneObject(TileObject object)
	{
		recolorIfMatched(object);
	}

	private void handleSceneObjectSafely(TileObject object)
	{
		try
		{
			handleSceneObject(object);
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.warn("Unable to process Doom Unique scene object", ex);
			messageOnce("scene-object-error", "Scene object processing failed. See client.log.");
		}
	}

	private boolean isConfiguredTestObject(TileObject object)
	{
		return isConfiguredTestObjectId(object) && isConfiguredTestObjectType(object);
	}

	private boolean isConfiguredTestObjectId(TileObject object)
	{
		return object != null && testObjectIds.contains(object.getId());
	}

	private boolean isConfiguredTestObjectType(TileObject object)
	{
		switch (config.testObjectType())
		{
			case ANY:
				return true;
			case GAME:
				return object instanceof GameObject;
			case GROUND:
				return object instanceof GroundObject;
			case DECORATIVE:
				return object instanceof DecorativeObject;
			case WALL:
				return object instanceof WallObject;
			default:
				return false;
		}
	}



	private boolean swapTestObjectIfMatched(TileObject object)
	{
		if (!shouldSwapTestObject(object))
		{
			return false;
		}

		Model model = getUniqueHoleSwapModel();
		if (model == null)
		{
			return false;
		}

		RuneLiteObject runeLiteObject = testSwapObjects.get(object);
		if (runeLiteObject == null)
		{
			runeLiteObject = client.createRuneLiteObject();
			runeLiteObject.setModel(model);
			runeLiteObject.setShouldLoop(true);
			Animation animation = getSwapAnimation();
			if (animation != null)
			{
				runeLiteObject.setAnimation(animation);
			}
			client.registerRuneLiteObject(runeLiteObject);
			testSwapObjects.put(object, runeLiteObject);
			messageOnce("swap:" + object.getId() + ":" + objectType(object), "Swap active for "
				+ objectType(object) + " object ID " + object.getId()
				+ " -> " + config.swapTargetObjectId()
				+ " using animation " + swapAnimationId() + ".");
		}

		runeLiteObject.setLocation(object.getLocalLocation(), object.getPlane());
		runeLiteObject.setOrientation(modelOrientation(object));
		runeLiteObject.setActive(true);
		return true;
	}

	private boolean shouldSwapTestObject(TileObject object)
	{
		return config.swapTestObjects()
			&& object instanceof GameObject
			&& isConfiguredTestObject(object);
	}

	private boolean shouldHideForTestSwap(TileObject object)
	{
		return shouldSwapTestObject(object);
	}

	private int modelOrientation(TileObject object)
	{
		if (object instanceof GameObject)
		{
			return ((GameObject) object).getModelOrientation();
		}
		if (object instanceof WallObject)
		{
			return ((WallObject) object).getOrientationA();
		}
		return 0;
	}

	private Model getUniqueHoleSwapModel()
	{
		if (uniqueHoleSwapModel != null)
		{
			return uniqueHoleSwapModel;
		}

		try
		{
			uniqueHoleSwapDefinition = ObjectModelDefinition.load(client.getIndexConfig(), config.swapTargetObjectId());
			if (uniqueHoleSwapDefinition == null)
			{
				log.warn("Unable to load object definition {} for Doom Unique test swap", config.swapTargetObjectId());
				messageOnce("swap-definition-missing:" + config.swapTargetObjectId(),
					"Could not load object definition " + config.swapTargetObjectId() + ".");
				return null;
			}

			uniqueHoleSwapModel = buildObjectModel(config.swapTargetObjectId(), uniqueHoleSwapDefinition);
			if (uniqueHoleSwapModel != null && config.recolorSwapTestObjects())
			{
				int recoloredFaces = recolorModel(uniqueHoleSwapModel, config.uniqueHoleColor());
				if (recoloredFaces == 0)
				{
					messageOnce("swap-no-gold-faces:" + config.swapTargetObjectId(),
						"Swap target " + config.swapTargetObjectId()
							+ " had no gold/yellow faces to recolor.");
				}
				else
				{
					messageOnce("swap-recolored:" + config.swapTargetObjectId(),
						"Recolored " + recoloredFaces + " faces on swap target "
							+ config.swapTargetObjectId() + ".");
				}
			}
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("Unable to build object model {} for Doom Unique test swap", config.swapTargetObjectId(), ex);
			messageOnce("swap-build-failed:" + config.swapTargetObjectId(),
				"Could not build swap target " + config.swapTargetObjectId() + ". See client.log.");
		}
		catch (RuntimeException ex)
		{
			log.warn("Unable to build object model {} for Doom Unique test swap", config.swapTargetObjectId(), ex);
			messageOnce("swap-build-failed:" + config.swapTargetObjectId(),
				"Could not build swap target " + config.swapTargetObjectId() + ". See client.log.");
		}

		return uniqueHoleSwapModel;
	}

	private Animation getSwapAnimation()
	{
		int animationId = swapAnimationId();
		if (animationId < 0)
		{
			return null;
		}

		Animation animation = client.loadAnimation(animationId);
		if (animation == null)
		{
			messageOnce("swap-animation-missing:" + animationId,
				"Could not load animation " + animationId
					+ " for swap target " + config.swapTargetObjectId() + ".");
		}
		return animation;
	}

	private int swapAnimationId()
	{
		if (uniqueHoleSwapDefinition != null && uniqueHoleSwapDefinition.hasAnimation())
		{
			return uniqueHoleSwapDefinition.getAnimationId();
		}

		return -1;
	}

	private Model buildObjectModel(int objectId, ObjectModelDefinition definition)
	{
		int[] modelIds = definition.getModelIds();
		if (modelIds.length == 0)
		{
			log.warn("Object definition {} has no model IDs for Doom Unique test swap", objectId);
			messageOnce("swap-model-ids-missing:" + objectId, "Object definition " + objectId + " has no model IDs.");
			return null;
		}

		List<ModelData> modelData = new ArrayList<>();
		for (int modelId : modelIds)
		{
			ModelData data = client.loadModelData(modelId);
			if (data == null)
			{
				log.warn("Unable to load model {} for object {} test swap", modelId, objectId);
				messageOnce("swap-model-missing:" + objectId + ":" + modelId,
					"Could not load model " + modelId + " for object " + objectId + ".");
				continue;
			}

			data = data.cloneVertices().cloneColors();
			if (data.getFaceTextures() != null)
			{
				data = data.cloneTextures();
			}
			if (data.getFaceTransparencies() != null)
			{
				data = data.cloneTransparencies();
			}
			if (definition.isRotated())
			{
				data.rotateY90Ccw();
			}
			applyDefinitionRecolors(data, definition);
			modelData.add(data);
		}

		if (modelData.isEmpty())
		{
			messageOnce("swap-no-models-loaded:" + objectId, "No models loaded for swap target " + objectId + ".");
			return null;
		}

		ModelData merged = modelData.size() == 1
			? modelData.get(0)
			: client.mergeModels(modelData.toArray(new ModelData[0]), modelData.size());
		if (definition.isScaled())
		{
			merged.scale(definition.getModelSizeX(), definition.getModelHeight(), definition.getModelSizeY());
		}
		if (definition.isTranslated())
		{
			merged.translate(definition.getOffsetX(), definition.getOffsetHeight(), definition.getOffsetY());
		}

		return merged.light();
	}

	private void applyDefinitionRecolors(ModelData modelData, ObjectModelDefinition definition)
	{
		short[] recolorFrom = definition.getRecolorFrom();
		short[] recolorTo = definition.getRecolorTo();
		if (recolorFrom != null && recolorTo != null)
		{
			for (int i = 0; i < recolorFrom.length; i++)
			{
				modelData.recolor(recolorFrom[i], recolorTo[i]);
			}
		}

		short[] retextureFrom = definition.getRetextureFrom();
		short[] retextureTo = definition.getRetextureTo();
		if (retextureFrom != null && retextureTo != null && modelData.getFaceTextures() != null)
		{
			for (int i = 0; i < retextureFrom.length; i++)
			{
				modelData.retexture(retextureFrom[i], retextureTo[i]);
			}
		}
	}

	private void recolorIfMatched(TileObject object)
	{
		Color color = colorFor(object);
		if (color == null)
		{
			return;
		}

		Set<Model> models = collectModels(object);
		if (models.isEmpty())
		{
			return;
		}

		replaceTrackedObjectModels(object, models);
		int recoloredFaces = 0;
		for (Model model : models)
		{
			recoloredFaces += recolorModel(model, color);
		}
		if (recoloredFaces == 0)
		{
			messageOnce("no-gold-faces:" + object.getId() + ":" + objectType(object),
				"Matched " + objectType(object) + " object ID " + object.getId()
					+ ", but found no gold/yellow model faces to recolor.");
		}
	}

	private void replaceTrackedObjectModels(TileObject object, Set<Model> models)
	{
		Set<Model> previousModels = objectModels.put(object, models);
		resetReplacedModels(previousModels);
	}

	private void resetReplacedModels(Set<Model> previousModels)
	{
		if (previousModels == null)
		{
			return;
		}

		for (Model previousModel : previousModels)
		{
			if (!isModelStillTracked(previousModel))
			{
				resetModel(previousModel);
			}
		}
	}

	private Color colorFor(TileObject object)
	{
		if (object == null)
		{
			return null;
		}

		int objectId = object.getId();
		if (config.recolorUniqueHole() && uniqueHoleIds.contains(objectId))
		{
			return config.uniqueHoleColor();
		}



		return null;
	}

	private int recolorModel(Model model, Color color)
	{
		if (model == null || color == null)
		{
			return 0;
		}

		originalColors.computeIfAbsent(model, ModelColors::copy);
		int rs2hsb = colorToRs2hsb(color);

		boolean[] changedFaces = findGoldFaces(model);
		int recoloredFaces = countChangedFaces(changedFaces);
		recolorFaces(model.getFaceColors1(), rs2hsb, changedFaces);
		recolorFaces(model.getFaceColors2(), rs2hsb, changedFaces);
		recolorFaces(model.getFaceColors3(), rs2hsb, changedFaces);
		recolorFaces(model.getUnlitFaceColors(), (short) rs2hsb, changedFaces);
		clearRecoloredFaceTextures(model.getFaceTextures(), changedFaces);
		return recoloredFaces;
	}

	private boolean[] findGoldFaces(Model model)
	{
		boolean[] changedFaces = new boolean[maxFaceCount(model)];
		markGoldFaces(model.getFaceColors1(), changedFaces);
		markGoldFaces(model.getFaceColors2(), changedFaces);
		markGoldFaces(model.getFaceColors3(), changedFaces);
		markGoldFaces(model.getUnlitFaceColors(), changedFaces);
		markSharedTextureFaces(model.getFaceTextures(), changedFaces);
		markConnectedSoftGoldFaces(model, changedFaces);
		markCoolArtifactFaces(model.getFaceColors1(), changedFaces);
		markCoolArtifactFaces(model.getFaceColors2(), changedFaces);
		markCoolArtifactFaces(model.getFaceColors3(), changedFaces);
		markCoolArtifactFaces(model.getUnlitFaceColors(), changedFaces);
		markConnectedCoolArtifactFaces(model, changedFaces);
		markMutedInteriorFaces(model, changedFaces);
		markSharedTextureFaces(model.getFaceTextures(), changedFaces);
		return changedFaces;
	}

	private void markGoldFaces(int[] colors, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (isGoldColor(colors[i]))
			{
				changedFaces[i] = true;
			}
		}
	}

	private void markGoldFaces(short[] colors, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (isGoldColor(colors[i] & 0xFFFF))
			{
				changedFaces[i] = true;
			}
		}
	}

	private void markCoolArtifactFaces(int[] colors, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (isCoolArtifactColor(colors[i]))
			{
				changedFaces[i] = true;
			}
		}
	}

	private void markCoolArtifactFaces(short[] colors, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (isCoolArtifactColor(colors[i] & 0xFFFF))
			{
				changedFaces[i] = true;
			}
		}
	}

	private int countChangedFaces(boolean[] changedFaces)
	{
		int count = 0;
		for (boolean changed : changedFaces)
		{
			if (changed)
			{
				count++;
			}
		}
		return count;
	}

	private void markSharedTextureFaces(short[] textures, boolean[] changedFaces)
	{
		if (textures == null)
		{
			return;
		}

		Set<Short> changedTextures = new HashSet<>();
		for (int i = 0; i < textures.length && i < changedFaces.length; i++)
		{
			if (changedFaces[i] && textures[i] != -1)
			{
				changedTextures.add(textures[i]);
			}
		}

		if (changedTextures.isEmpty())
		{
			return;
		}

		for (int i = 0; i < textures.length && i < changedFaces.length; i++)
		{
			if (changedTextures.contains(textures[i]))
			{
				changedFaces[i] = true;
			}
		}
	}

	private void markConnectedSoftGoldFaces(Model model, boolean[] changedFaces)
	{
		int[] faceIndices1 = model.getFaceIndices1();
		int[] faceIndices2 = model.getFaceIndices2();
		int[] faceIndices3 = model.getFaceIndices3();
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (faceIndices1 == null || faceIndices2 == null || faceIndices3 == null)
		{
			return;
		}

		int faceCount = Math.min(model.getFaceCount(), changedFaces.length);
		faceCount = Math.min(faceCount, Math.min(faceIndices1.length, Math.min(faceIndices2.length, faceIndices3.length)));
		int verticesCount = geometryVertexCount(model, verticesX, verticesY, verticesZ);
		for (int pass = 0; pass < 3; pass++)
		{
			Set<Long> changedEdges = changedFaceEdges(faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			Set<String> changedGeometryEdges = changedGeometryEdges(verticesX, verticesY, verticesZ, verticesCount,
				faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			if (changedEdges.isEmpty() && changedGeometryEdges.isEmpty())
			{
				return;
			}

			boolean foundMore = false;
			for (int face = 0; face < faceCount; face++)
			{
				if (changedFaces[face] || !isSoftGoldFace(model, face))
				{
					continue;
				}

				if (hasChangedEdge(changedEdges, faceIndices1[face], faceIndices2[face], faceIndices3[face])
					|| hasChangedGeometryEdge(changedGeometryEdges, verticesX, verticesY, verticesZ, verticesCount,
						faceIndices1[face], faceIndices2[face], faceIndices3[face]))
				{
					changedFaces[face] = true;
					foundMore = true;
				}
			}

			if (!foundMore)
			{
				return;
			}
		}
	}

	private void markConnectedCoolArtifactFaces(Model model, boolean[] changedFaces)
	{
		int[] faceIndices1 = model.getFaceIndices1();
		int[] faceIndices2 = model.getFaceIndices2();
		int[] faceIndices3 = model.getFaceIndices3();
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (faceIndices1 == null || faceIndices2 == null || faceIndices3 == null)
		{
			return;
		}

		int faceCount = Math.min(model.getFaceCount(), changedFaces.length);
		faceCount = Math.min(faceCount, Math.min(faceIndices1.length, Math.min(faceIndices2.length, faceIndices3.length)));
		int verticesCount = geometryVertexCount(model, verticesX, verticesY, verticesZ);
		for (int pass = 0; pass < 2; pass++)
		{
			Set<Long> changedEdges = changedFaceEdges(faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			Set<String> changedGeometryEdges = changedGeometryEdges(verticesX, verticesY, verticesZ, verticesCount,
				faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			if (changedEdges.isEmpty() && changedGeometryEdges.isEmpty())
			{
				return;
			}

			boolean foundMore = false;
			for (int face = 0; face < faceCount; face++)
			{
				if (changedFaces[face] || !isCoolArtifactFace(model, face))
				{
					continue;
				}

				if (hasChangedEdge(changedEdges, faceIndices1[face], faceIndices2[face], faceIndices3[face])
					|| hasChangedGeometryEdge(changedGeometryEdges, verticesX, verticesY, verticesZ, verticesCount,
						faceIndices1[face], faceIndices2[face], faceIndices3[face]))
				{
					changedFaces[face] = true;
					foundMore = true;
				}
			}

			if (!foundMore)
			{
				return;
			}
		}
	}

	private void markMutedInteriorFaces(Model model, boolean[] changedFaces)
	{
		markRadialFaces(model, changedFaces, 0.52f, 1.0f, 1.0f, false, this::isMutedInteriorFace);
		markConnectedMutedInteriorFaces(model, changedFaces);
	}

	private void markConnectedMutedInteriorFaces(Model model, boolean[] changedFaces)
	{
		int[] faceIndices1 = model.getFaceIndices1();
		int[] faceIndices2 = model.getFaceIndices2();
		int[] faceIndices3 = model.getFaceIndices3();
		float[] verticesX = model.getVerticesX();
		float[] verticesY = model.getVerticesY();
		float[] verticesZ = model.getVerticesZ();
		if (faceIndices1 == null || faceIndices2 == null || faceIndices3 == null)
		{
			return;
		}

		int faceCount = Math.min(model.getFaceCount(), changedFaces.length);
		faceCount = Math.min(faceCount, Math.min(faceIndices1.length, Math.min(faceIndices2.length, faceIndices3.length)));
		int verticesCount = geometryVertexCount(model, verticesX, verticesY, verticesZ);
		for (int pass = 0; pass < 3; pass++)
		{
			Set<Long> changedEdges = changedFaceEdges(faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			Set<String> changedGeometryEdges = changedGeometryEdges(verticesX, verticesY, verticesZ, verticesCount,
				faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount);
			if (changedEdges.isEmpty() && changedGeometryEdges.isEmpty())
			{
				return;
			}

			boolean foundMore = false;
			for (int face = 0; face < faceCount; face++)
			{
				if (changedFaces[face] || !isSoftMutedInteriorFace(model, face))
				{
					continue;
				}

				if (hasChangedEdge(changedEdges, faceIndices1[face], faceIndices2[face], faceIndices3[face])
					|| hasChangedGeometryEdge(changedGeometryEdges, verticesX, verticesY, verticesZ, verticesCount,
						faceIndices1[face], faceIndices2[face], faceIndices3[face]))
				{
					changedFaces[face] = true;
					foundMore = true;
				}
			}

			if (!foundMore)
			{
				return;
			}
		}
	}

	private void markRadialFaces(Model model, boolean[] changedFaces, float centerRadiusScale,
		float maxVertexRadiusScale, float maxRadialSpanScale, boolean requireChangedEdge, FaceMatcher faceMatcher)
	{
		int[] faceIndices1 = model.getFaceIndices1();
		int[] faceIndices2 = model.getFaceIndices2();
		int[] faceIndices3 = model.getFaceIndices3();
		float[] verticesX = model.getVerticesX();
		float[] verticesZ = model.getVerticesZ();
		if (faceIndices1 == null || faceIndices2 == null || faceIndices3 == null
			|| verticesX == null || verticesZ == null)
		{
			return;
		}

		int verticesCount = Math.min(model.getVerticesCount(), Math.min(verticesX.length, verticesZ.length));
		if (verticesCount == 0)
		{
			return;
		}

		float minX = verticesX[0];
		float maxX = verticesX[0];
		float minZ = verticesZ[0];
		float maxZ = verticesZ[0];
		for (int vertex = 1; vertex < verticesCount; vertex++)
		{
			minX = Math.min(minX, verticesX[vertex]);
			maxX = Math.max(maxX, verticesX[vertex]);
			minZ = Math.min(minZ, verticesZ[vertex]);
			maxZ = Math.max(maxZ, verticesZ[vertex]);
		}

		float centerX = (minX + maxX) / 2.0f;
		float centerZ = (minZ + maxZ) / 2.0f;
		float maxRadius = Math.max(maxX - minX, maxZ - minZ) / 2.0f;
		if (maxRadius <= 0.0f)
		{
			return;
		}

		float innerRadius = maxRadius * centerRadiusScale;
		float innerRadiusSquared = innerRadius * innerRadius;
		float maxVertexRadius = maxRadius * maxVertexRadiusScale;
		float maxVertexRadiusSquared = maxVertexRadius * maxVertexRadius;
		float maxRadialSpan = maxRadius * maxRadialSpanScale;
		int faceCount = Math.min(model.getFaceCount(), changedFaces.length);
		faceCount = Math.min(faceCount, Math.min(faceIndices1.length, Math.min(faceIndices2.length, faceIndices3.length)));
		Set<Long> changedEdges = requireChangedEdge
			? changedFaceEdges(faceIndices1, faceIndices2, faceIndices3, changedFaces, faceCount)
			: Collections.emptySet();
		for (int face = 0; face < faceCount; face++)
		{
			if (changedFaces[face] || !faceMatcher.matches(model, face)
				|| !hasValidVertices(faceIndices1[face], faceIndices2[face], faceIndices3[face], verticesCount))
			{
				continue;
			}
			if (requireChangedEdge && !hasChangedEdge(changedEdges, faceIndices1[face], faceIndices2[face],
				faceIndices3[face]))
			{
				continue;
			}

			float faceCenterX = (verticesX[faceIndices1[face]] + verticesX[faceIndices2[face]]
				+ verticesX[faceIndices3[face]]) / 3.0f;
			float faceCenterZ = (verticesZ[faceIndices1[face]] + verticesZ[faceIndices2[face]]
				+ verticesZ[faceIndices3[face]]) / 3.0f;
			float deltaX = faceCenterX - centerX;
			float deltaZ = faceCenterZ - centerZ;
			if (deltaX * deltaX + deltaZ * deltaZ <= innerRadiusSquared
				&& isFaceInsideRadius(verticesX, verticesZ, faceIndices1[face], faceIndices2[face],
					faceIndices3[face], centerX, centerZ, maxVertexRadiusSquared)
				&& isFaceRadialSpanInside(verticesX, verticesZ, faceIndices1[face], faceIndices2[face],
					faceIndices3[face], centerX, centerZ, maxRadialSpan))
			{
				changedFaces[face] = true;
			}
		}
	}

	private boolean isFaceRadialSpanInside(float[] verticesX, float[] verticesZ, int vertex1, int vertex2, int vertex3,
		float centerX, float centerZ, float maxRadialSpan)
	{
		float radius1 = vertexRadius(verticesX, verticesZ, vertex1, centerX, centerZ);
		float radius2 = vertexRadius(verticesX, verticesZ, vertex2, centerX, centerZ);
		float radius3 = vertexRadius(verticesX, verticesZ, vertex3, centerX, centerZ);
		float minRadius = Math.min(radius1, Math.min(radius2, radius3));
		float maxRadius = Math.max(radius1, Math.max(radius2, radius3));
		return maxRadius - minRadius <= maxRadialSpan;
	}

	private boolean isFaceInsideRadius(float[] verticesX, float[] verticesZ, int vertex1, int vertex2, int vertex3,
		float centerX, float centerZ, float maxRadiusSquared)
	{
		return isVertexInsideRadius(verticesX, verticesZ, vertex1, centerX, centerZ, maxRadiusSquared)
			&& isVertexInsideRadius(verticesX, verticesZ, vertex2, centerX, centerZ, maxRadiusSquared)
			&& isVertexInsideRadius(verticesX, verticesZ, vertex3, centerX, centerZ, maxRadiusSquared);
	}

	private boolean isVertexInsideRadius(float[] verticesX, float[] verticesZ, int vertex, float centerX,
		float centerZ, float maxRadiusSquared)
	{
		float deltaX = verticesX[vertex] - centerX;
		float deltaZ = verticesZ[vertex] - centerZ;
		return deltaX * deltaX + deltaZ * deltaZ <= maxRadiusSquared;
	}

	private float vertexRadius(float[] verticesX, float[] verticesZ, int vertex, float centerX, float centerZ)
	{
		float deltaX = verticesX[vertex] - centerX;
		float deltaZ = verticesZ[vertex] - centerZ;
		return (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
	}

	private boolean hasValidVertices(int vertex1, int vertex2, int vertex3, int verticesCount)
	{
		return vertex1 >= 0 && vertex1 < verticesCount
			&& vertex2 >= 0 && vertex2 < verticesCount
			&& vertex3 >= 0 && vertex3 < verticesCount;
	}

	private Set<Long> changedFaceEdges(int[] faceIndices1, int[] faceIndices2, int[] faceIndices3,
		boolean[] changedFaces, int faceCount)
	{
		Set<Long> edges = new HashSet<>();
		for (int face = 0; face < faceCount; face++)
		{
			if (!changedFaces[face])
			{
				continue;
			}

			edges.add(edgeKey(faceIndices1[face], faceIndices2[face]));
			edges.add(edgeKey(faceIndices2[face], faceIndices3[face]));
			edges.add(edgeKey(faceIndices3[face], faceIndices1[face]));
		}
		return edges;
	}

	private boolean hasChangedEdge(Set<Long> changedEdges, int vertex1, int vertex2, int vertex3)
	{
		return changedEdges.contains(edgeKey(vertex1, vertex2))
			|| changedEdges.contains(edgeKey(vertex2, vertex3))
			|| changedEdges.contains(edgeKey(vertex3, vertex1));
	}

	private int geometryVertexCount(Model model, float[] verticesX, float[] verticesY, float[] verticesZ)
	{
		if (verticesX == null || verticesY == null || verticesZ == null)
		{
			return 0;
		}

		return Math.min(model.getVerticesCount(), Math.min(verticesX.length, Math.min(verticesY.length, verticesZ.length)));
	}

	private Set<String> changedGeometryEdges(float[] verticesX, float[] verticesY, float[] verticesZ, int verticesCount,
		int[] faceIndices1, int[] faceIndices2, int[] faceIndices3, boolean[] changedFaces, int faceCount)
	{
		if (verticesCount == 0)
		{
			return Collections.emptySet();
		}

		Set<String> edges = new HashSet<>();
		for (int face = 0; face < faceCount; face++)
		{
			if (!changedFaces[face] || !hasValidVertices(faceIndices1[face], faceIndices2[face], faceIndices3[face],
				verticesCount))
			{
				continue;
			}

			edges.add(geometryEdgeKey(verticesX, verticesY, verticesZ, faceIndices1[face], faceIndices2[face]));
			edges.add(geometryEdgeKey(verticesX, verticesY, verticesZ, faceIndices2[face], faceIndices3[face]));
			edges.add(geometryEdgeKey(verticesX, verticesY, verticesZ, faceIndices3[face], faceIndices1[face]));
		}
		return edges;
	}

	private boolean hasChangedGeometryEdge(Set<String> changedEdges, float[] verticesX, float[] verticesY,
		float[] verticesZ, int verticesCount, int vertex1, int vertex2, int vertex3)
	{
		if (changedEdges.isEmpty() || !hasValidVertices(vertex1, vertex2, vertex3, verticesCount))
		{
			return false;
		}

		return changedEdges.contains(geometryEdgeKey(verticesX, verticesY, verticesZ, vertex1, vertex2))
			|| changedEdges.contains(geometryEdgeKey(verticesX, verticesY, verticesZ, vertex2, vertex3))
			|| changedEdges.contains(geometryEdgeKey(verticesX, verticesY, verticesZ, vertex3, vertex1));
	}

	private String geometryEdgeKey(float[] verticesX, float[] verticesY, float[] verticesZ, int vertex1, int vertex2)
	{
		String first = geometryVertexKey(verticesX, verticesY, verticesZ, vertex1);
		String second = geometryVertexKey(verticesX, verticesY, verticesZ, vertex2);
		return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
	}

	private String geometryVertexKey(float[] verticesX, float[] verticesY, float[] verticesZ, int vertex)
	{
		return Float.floatToIntBits(verticesX[vertex]) + ":"
			+ Float.floatToIntBits(verticesY[vertex]) + ":"
			+ Float.floatToIntBits(verticesZ[vertex]);
	}

	private long edgeKey(int vertex1, int vertex2)
	{
		int min = Math.min(vertex1, vertex2);
		int max = Math.max(vertex1, vertex2);
		return ((long) min << 32) | (max & 0xFFFFFFFFL);
	}

	private boolean isSoftGoldFace(Model model, int face)
	{
		return hasSoftGoldColor(model.getFaceColors1(), face)
			|| hasSoftGoldColor(model.getFaceColors2(), face)
			|| hasSoftGoldColor(model.getFaceColors3(), face)
			|| hasSoftGoldColor(model.getUnlitFaceColors(), face);
	}

	private boolean hasSoftGoldColor(int[] colors, int face)
	{
		return colors != null && face < colors.length && isSoftGoldColor(colors[face]);
	}

	private boolean hasSoftGoldColor(short[] colors, int face)
	{
		return colors != null && face < colors.length && isSoftGoldColor(colors[face] & 0xFFFF);
	}

	private boolean isCoolArtifactFace(Model model, int face)
	{
		return hasCoolArtifactColor(model.getFaceColors1(), face)
			|| hasCoolArtifactColor(model.getFaceColors2(), face)
			|| hasCoolArtifactColor(model.getFaceColors3(), face)
			|| hasCoolArtifactColor(model.getUnlitFaceColors(), face);
	}

	private boolean hasCoolArtifactColor(int[] colors, int face)
	{
		return colors != null && face < colors.length && isCoolArtifactColor(colors[face]);
	}

	private boolean hasCoolArtifactColor(short[] colors, int face)
	{
		return colors != null && face < colors.length && isCoolArtifactColor(colors[face] & 0xFFFF);
	}

	private boolean isMutedInteriorFace(Model model, int face)
	{
		return hasMutedInteriorColor(model.getFaceColors1(), face)
			|| hasMutedInteriorColor(model.getFaceColors2(), face)
			|| hasMutedInteriorColor(model.getFaceColors3(), face)
			|| hasMutedInteriorColor(model.getUnlitFaceColors(), face);
	}

	private boolean hasMutedInteriorColor(int[] colors, int face)
	{
		return colors != null && face < colors.length && isMutedInteriorColor(colors[face]);
	}

	private boolean hasMutedInteriorColor(short[] colors, int face)
	{
		return colors != null && face < colors.length && isMutedInteriorColor(colors[face] & 0xFFFF);
	}

	private boolean isSoftMutedInteriorFace(Model model, int face)
	{
		return hasSoftMutedInteriorColor(model.getFaceColors1(), face)
			|| hasSoftMutedInteriorColor(model.getFaceColors2(), face)
			|| hasSoftMutedInteriorColor(model.getFaceColors3(), face)
			|| hasSoftMutedInteriorColor(model.getUnlitFaceColors(), face);
	}

	private boolean hasSoftMutedInteriorColor(int[] colors, int face)
	{
		return colors != null && face < colors.length && isSoftMutedInteriorColor(colors[face]);
	}

	private boolean hasSoftMutedInteriorColor(short[] colors, int face)
	{
		return colors != null && face < colors.length && isSoftMutedInteriorColor(colors[face] & 0xFFFF);
	}

	private void recolorFaces(int[] colors, int targetColor, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (changedFaces[i])
			{
				colors[i] = targetColor;
			}
		}
	}

	private void recolorFaces(short[] colors, short targetColor, boolean[] changedFaces)
	{
		if (colors == null)
		{
			return;
		}

		for (int i = 0; i < colors.length && i < changedFaces.length; i++)
		{
			if (changedFaces[i])
			{
				colors[i] = targetColor;
			}
		}
	}

	private boolean isGoldColor(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;

		boolean richGold = hue >= 6 && hue <= 17 && saturation >= 3 && brightness >= 38;
		boolean brightPaleGold = hue >= 8 && hue <= 18 && saturation >= 2 && brightness >= 50;
		return richGold || brightPaleGold;
	}

	private boolean isSoftGoldColor(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;

		boolean darkerGold = hue >= 4 && hue <= 19 && saturation >= 2 && brightness >= 24;
		boolean mutedGold = hue >= 7 && hue <= 18 && saturation >= 1 && brightness >= 42;
		return darkerGold || mutedGold;
	}

	private boolean isCoolArtifactColor(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;

		return hue >= 18 && hue <= 42 && saturation >= 1 && brightness >= 22;
	}

	private boolean isMutedInteriorColor(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;

		boolean mutedBlueGreen = hue >= 16 && hue <= 46 && brightness >= 14 && brightness <= 82;
		boolean neutralFloor = saturation <= 2 && brightness >= 18 && brightness <= 82;
		return mutedBlueGreen || neutralFloor;
	}

	private boolean isSoftMutedInteriorColor(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;

		boolean mutedBlueGreen = hue >= 12 && hue <= 50 && brightness >= 10 && brightness <= 92;
		boolean neutralFloor = saturation <= 3 && brightness >= 12 && brightness <= 92;
		boolean darkShadow = brightness >= 6 && brightness <= 30;
		return mutedBlueGreen || neutralFloor || darkShadow;
	}

	private int maxFaceCount(Model model)
	{
		return Math.max(Math.max(length(model.getFaceColors1()), length(model.getFaceColors2())),
			Math.max(Math.max(length(model.getFaceColors3()), length(model.getUnlitFaceColors())),
				length(model.getFaceTextures())));
	}

	private int length(int[] values)
	{
		return values == null ? 0 : values.length;
	}

	private int length(short[] values)
	{
		return values == null ? 0 : values.length;
	}

	private void clearRecoloredFaceTextures(short[] textures, boolean[] changedFaces)
	{
		if (textures == null)
		{
			return;
		}

		for (int i = 0; i < textures.length && i < changedFaces.length; i++)
		{
			if (changedFaces[i])
			{
				textures[i] = -1;
			}
		}
	}

	private void resetIfTracked(TileObject object)
	{

		removeTestSwap(object);

		Set<Model> models = objectModels.remove(object);
		if (models == null)
		{
			return;
		}

		for (Model model : models)
		{
			if (!isModelStillTracked(model))
			{
				resetModel(model);
			}
		}
	}

	private boolean isModelStillTracked(Model model)
	{
		for (Set<Model> models : objectModels.values())
		{
			if (models.contains(model))
			{
				return true;
			}
		}
		return false;
	}

	private void resetAllModels()
	{
		for (Map.Entry<Model, ModelColors> entry : originalColors.entrySet())
		{
			try
			{
				entry.getValue().restore(entry.getKey());
			}
			catch (RuntimeException | AssertionError ex)
			{
				log.debug("Unable to restore Doom Unique model colors", ex);
			}
		}
		originalColors.clear();
		objectModels.clear();
		uniqueHoleSwapModel = null;
		uniqueHoleSwapDefinition = null;
	}

	private void clearTestSwaps()
	{
		for (RuneLiteObject runeLiteObject : testSwapObjects.values())
		{
			removeRuneLiteObjectSafely(runeLiteObject);
		}
		testSwapObjects.clear();
	}

	private void removeInactiveTestSwaps(Set<TileObject> activeTargets)
	{
		Iterator<Map.Entry<TileObject, RuneLiteObject>> iterator = testSwapObjects.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<TileObject, RuneLiteObject> entry = iterator.next();
			if (!activeTargets.contains(entry.getKey()))
			{
				removeRuneLiteObjectSafely(entry.getValue());
				iterator.remove();
			}
		}
	}

	private void removeTestSwap(TileObject object)
	{
		RuneLiteObject runeLiteObject = testSwapObjects.remove(object);
		if (runeLiteObject == null)
		{
			return;
		}

		removeRuneLiteObjectSafely(runeLiteObject);
	}

	private void removeRuneLiteObjectSafely(RuneLiteObject runeLiteObject)
	{
		if (runeLiteObject == null)
		{
			return;
		}

		try
		{
			runeLiteObject.setActive(false);
			if (client.isRuneLiteObjectRegistered(runeLiteObject))
			{
				client.removeRuneLiteObject(runeLiteObject);
			}
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.debug("Unable to remove Doom Unique RuneLiteObject", ex);
		}
	}

	private void resetModel(Model model)
	{
		ModelColors colors = originalColors.remove(model);
		if (colors != null)
		{
			colors.restore(model);
		}
	}

	private Set<Model> collectModels(TileObject object)
	{
		Set<Model> models = Collections.newSetFromMap(new IdentityHashMap<>());

		if (object instanceof GameObject)
		{
			addRenderableModel(models, ((GameObject) object).getRenderable());
		}
		else if (object instanceof GroundObject)
		{
			addRenderableModel(models, ((GroundObject) object).getRenderable());
		}
		else if (object instanceof DecorativeObject)
		{
			DecorativeObject decorativeObject = (DecorativeObject) object;
			addRenderableModel(models, decorativeObject.getRenderable());
			addRenderableModel(models, decorativeObject.getRenderable2());
		}
		else if (object instanceof WallObject)
		{
			WallObject wallObject = (WallObject) object;
			addRenderableModel(models, wallObject.getRenderable1());
			addRenderableModel(models, wallObject.getRenderable2());
		}

		return models;
	}

	private void addRenderableModel(Set<Model> models, Renderable renderable)
	{
		if (renderable == null)
		{
			return;
		}

		Model model;
		try
		{
			model = renderable.getModel();
		}
		catch (RuntimeException | AssertionError ex)
		{
			log.debug("Skipping Doom Unique renderable model", ex);
			return;
		}
		if (model != null)
		{
			models.add(model);
		}
	}



	private String objectAnimationInfo(TileObject object)
	{
		if (!(object instanceof GameObject))
		{
			return "";
		}

		Renderable renderable = ((GameObject) object).getRenderable();
		if (!(renderable instanceof DynamicObject))
		{
			return "";
		}

		DynamicObject dynamicObject = (DynamicObject) renderable;
		Animation animation = dynamicObject.getAnimation();
		if (animation == null)
		{
			return "";
		}

		return " animation=" + animation.getId()
			+ " frame=" + dynamicObject.getAnimFrame()
			+ " cycle=" + dynamicObject.getAnimCycle();
	}

	private void messageOnce(String key, String message)
	{
		if (!statusMessages.add(key))
		{
			return;
		}

		String formatted = "Doom Unique: " + message;
		log.info(formatted);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", formatted, null);
		}
	}

	private boolean isWithinRadius(WorldPoint center, WorldPoint point, int radius)
	{
		return center.getPlane() == point.getPlane()
			&& Math.abs(center.getX() - point.getX()) <= radius
			&& Math.abs(center.getY() - point.getY()) <= radius;
	}

	private String getObjectName(int objectId)
	{
		ObjectComposition definition = client.getObjectDefinition(objectId);
		if (definition == null || definition.getName() == null || "null".equals(definition.getName()))
		{
			return "unknown";
		}
		return definition.getName();
	}

	private String objectType(TileObject object)
	{
		if (object instanceof GameObject)
		{
			return "game";
		}
		if (object instanceof GroundObject)
		{
			return "ground";
		}
		if (object instanceof DecorativeObject)
		{
			return "decorative";
		}
		if (object instanceof WallObject)
		{
			return "wall";
		}
		return "tile";
	}

	private void forEachSceneObject(SceneObjectConsumer consumer)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return;
		}

		Tile[][][] tiles = scene.getTiles();
		if (tiles == null)
		{
			return;
		}

		for (Tile[][] plane : tiles)
		{
			if (plane == null)
			{
				continue;
			}
			for (Tile[] column : plane)
			{
				if (column == null)
				{
					continue;
				}
				for (Tile tile : column)
				{
					visitTileObjects(tile, consumer);
				}
			}
		}
	}

	private void visitTileObjects(Tile tile, SceneObjectConsumer consumer)
	{
		if (tile == null)
		{
			return;
		}

		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null)
				{
					consumer.accept(gameObject);
				}
			}
		}

		if (tile.getGroundObject() != null)
		{
			consumer.accept(tile.getGroundObject());
		}
		if (tile.getDecorativeObject() != null)
		{
			consumer.accept(tile.getDecorativeObject());
		}
		if (tile.getWallObject() != null)
		{
			consumer.accept(tile.getWallObject());
		}
	}

	private int colorToRs2hsb(Color color)
	{
		float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		hsbVals[2] -= Math.min(hsbVals[1], hsbVals[2] / 2);

		int encodedHue = (int) (hsbVals[0] * 63);
		int encodedSaturation = (int) (hsbVals[1] * 7);
		int encodedBrightness = (int) (hsbVals[2] * 127);
		return (encodedHue << 10) + (encodedSaturation << 7) + encodedBrightness;
	}

	private interface SceneObjectConsumer
	{
		void accept(TileObject object);
	}

	private interface FaceMatcher
	{
		boolean matches(Model model, int face);
	}

	private static final class ModelColors
	{
		private final int[] faceColors1;
		private final int[] faceColors2;
		private final int[] faceColors3;
		private final short[] unlitFaceColors;
		private final short[] faceTextures;

		private ModelColors(int[] faceColors1, int[] faceColors2, int[] faceColors3, short[] unlitFaceColors,
			short[] faceTextures)
		{
			this.faceColors1 = faceColors1;
			this.faceColors2 = faceColors2;
			this.faceColors3 = faceColors3;
			this.unlitFaceColors = unlitFaceColors;
			this.faceTextures = faceTextures;
		}

		private static ModelColors copy(Model model)
		{
			return new ModelColors(
				copy(model.getFaceColors1()),
				copy(model.getFaceColors2()),
				copy(model.getFaceColors3()),
				copy(model.getUnlitFaceColors()),
				copy(model.getFaceTextures())
			);
		}

		private static int[] copy(int[] values)
		{
			return values == null ? null : values.clone();
		}

		private static short[] copy(short[] values)
		{
			return values == null ? null : values.clone();
		}

		private void restore(Model model)
		{
			restore(model.getFaceColors1(), faceColors1);
			restore(model.getFaceColors2(), faceColors2);
			restore(model.getFaceColors3(), faceColors3);
			restore(model.getUnlitFaceColors(), unlitFaceColors);
			restore(model.getFaceTextures(), faceTextures);
		}

		private void restore(int[] destination, int[] source)
		{
			if (destination != null && source != null && destination.length == source.length)
			{
				System.arraycopy(source, 0, destination, 0, source.length);
			}
		}

		private void restore(short[] destination, short[] source)
		{
			if (destination != null && source != null && destination.length == source.length)
			{
				System.arraycopy(source, 0, destination, 0, source.length);
			}
		}
	}
}
