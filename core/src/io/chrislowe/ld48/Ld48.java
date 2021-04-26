package io.chrislowe.ld48;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.*;
import io.chrislowe.ld48.ui.LaunchUi;
import io.chrislowe.ld48.ui.UnitUi;

import java.util.*;
import java.util.List;
import java.util.Queue;

public class Ld48 extends Game {
	public final static int screenWidth = 1024;
	public final static int screenHeight = 768;

	GameState gameState = GameState.Launching;
	int gridHeight, gridWidth;
	int gridPixelsWidth, gridPixelsHeight;
	int startX, startY;
	int attackMode;
	long nextAiMoveTime;
	long nextNonDmgBgTime;
	SpriteBatch batch;
	BitmapFont font;
	Map<String, Texture> textureMap = new HashMap<>();
	GridPiece[][] gridPieces;
	GridPiece selectedPiece;
	Set<TileType> selectablePieceTypes = setOf(TileType.Launch, TileType.Blue, TileType.Red);
	Set<TileType> movablePieceTypes = setOf(TileType.Launch, TileType.Empty1);
	List<TileUnit> unitTypes = new ArrayList<>();
	Map<String, Attack> attacks = new HashMap<>();
	List<TileUnit> playerUnitInstances = new ArrayList<>();
	List<TileUnit> aiUnitInstances = new ArrayList<>();
	Color bgColor = new Color(.3f, .3f, .3f, 1);
	Color flashColor = new Color(.3f, 0, 0, 1);

	LaunchUi launchUi;
	UnitUi unitUi;

	@Override
	public void create() {
		Random random = new Random();
		batch = new SpriteBatch();
		font = new BitmapFont();

		// TODO: HTML target hack
		String[] fileNames = new String[]{"tileAttackable","tileBlue","tileEmpty1","tileLaunch","tileMovable","tileRed","tileSelector","unitBug","unitRat","unitSnek","unitWorm"};
		for (String filename : fileNames) {
			textureMap.put(filename, new Texture("textures/" + filename + ".png"));
		}
//		FileHandle textureDir = Gdx.files.internal("textures");
//		for (FileHandle textureFile : textureDir.list("png")) {
//			textureMap.put(textureFile.nameWithoutExtension(), new Texture(textureFile.path()));
//		}

		Json json = new Json();
		JsonReader jsonReader = new JsonReader();
		JsonValue unitsJson = jsonReader.parse(Gdx.files.internal("data/units.json"));
		unitsJson.forEach(unit -> {
			TileUnit tileUnit = json.fromJson(TileUnit.class, unit.toJson(JsonWriter.OutputType.json));
			tileUnit.loadTexture(textureMap, unit.name);
			tileUnit.attacks = new ArrayList<>();
			// TODO html target hack
			if (tileUnit.name.equals("Bug")) {
				tileUnit.attacks.add("nibble");
			} else if (tileUnit.name.equals("Rat")) {
				tileUnit.attacks.add("byte");
			} else if (tileUnit.name.equals("Snake")) {
				tileUnit.attacks.add("byte");
				tileUnit.attacks.add("swallow");
			} else if (tileUnit.name.equals("Worm")) {
				tileUnit.attacks.add("nibble");
				tileUnit.attacks.add("wormhole");
			}
			unitTypes.add(tileUnit);
		});

		JsonValue attacksJson = jsonReader.parse(Gdx.files.internal("data/attacks.json"));
		attacksJson.forEach(attackJson -> {
			Attack attack = json.fromJson(Attack.class, attackJson.toJson(JsonWriter.OutputType.json));
			attacks.put(attackJson.name, attack);
		});

		String[] fileText = Gdx.files.internal("levels/grid.txt").readString().split("\\r?\\n");
		gridHeight = fileText.length;
		gridWidth = fileText[0].length();
		gridPixelsHeight = gridHeight * 32;
		gridPixelsWidth = gridWidth * 32;
		startX = (screenWidth / 2) - (gridPixelsWidth / 2);
		startY = (screenHeight / 2) - (gridPixelsHeight / 2);

		gridPieces = new GridPiece[gridWidth][gridHeight];
		for (int y = 0; y < fileText.length; y++) {
			for (int x = 0; x < fileText[0].length(); x++) {
				Rectangle r = new Rectangle();
				r.x = startX + (x * 32);
				r.y = startY + (y * 32);
				r.width = 32;
				r.height = 32;
				GridPiece gridPiece = new GridPiece(r, x, y);

				char pieceType = fileText[y].charAt(x);
				if (pieceType == 'E') {
					TileUnit cloneUnit = unitTypes.get(random.nextInt(unitTypes.size())).copy(textureMap);
					cloneUnit.currentHealth = cloneUnit.maxHealth;
					cloneUnit.gridX = x;
					cloneUnit.gridY = y;
					gridPiece.tileType = TileType.Red;
					gridPiece.tileUnit = cloneUnit;
					aiUnitInstances.add(cloneUnit);
				} else if (pieceType == 'X') {
					gridPiece.tileType = TileType.Launch;
				} else if (pieceType == '1') {
					gridPiece.tileType = TileType.Empty1;
				}

				gridPieces[x][y] = gridPiece;
			}
		}

		launchUi = new LaunchUi();
		launchUi.tileUnits.addAll(unitTypes);
		launchUi.unitBackgroundTexture = textureMap.get("tileBlue");
		launchUi.isActive = false;

		unitUi = new UnitUi(unitTypes);
	}

	@Override
	public void render() {
		long currentTime = System.currentTimeMillis();
		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
			if (gameState == GameState.Launching) {
				startPlayerTurn();
			} else if (gameState == GameState.PlayerTurn && !aiUnitInstances.isEmpty()) {
				startAiTurn();
			}
		}

		if (gameState == GameState.PlayerTurn) {
			if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
				attackMode = 1;
			} else if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
				attackMode = 2;
			}
		} else if (gameState == GameState.AiTurn) {
			if (currentTime > nextAiMoveTime) {
				TileUnit unit = aiUnitInstances.stream().filter(u -> u.moves > 0 || u.canAttack).findFirst().orElse(null);
				if (unit == null) {
					if (!playerUnitInstances.isEmpty()) {
						startPlayerTurn();
					}
				} else {
					performAiAction(unit);
					nextAiMoveTime = currentTime + 250;
				}
			}
		}

		if (currentTime > nextNonDmgBgTime) {
			ScreenUtils.clear(bgColor);
		} else {
			ScreenUtils.clear(flashColor);
		}

		if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
			Integer launchedUnitIdx = launchUi.handleInput();
			if (launchedUnitIdx != null && selectedPiece != null) {
				TileUnit cloneUnit = unitTypes.get(launchedUnitIdx).copy(textureMap);
				cloneUnit.currentHealth = cloneUnit.maxHealth;
				cloneUnit.gridX = selectedPiece.x;
				cloneUnit.gridY = selectedPiece.y;
				selectedPiece.tileType = TileType.Blue;
				selectedPiece.tileUnit = cloneUnit;
				playerUnitInstances.add(cloneUnit);
			}

			GridPiece oldSelectedPiece = selectedPiece;
			selectedPiece = null;
			int x = Gdx.input.getX(), y = Gdx.input.getY();
			if (x > startX && x < startX + gridPixelsWidth && y > startY && y < startY + gridPixelsHeight) {
				int gridX = (x - startX) / 32;
				int gridY = gridHeight - ((y - startY) / 32) - 1;

				if (oldSelectedPiece != null) {
					boolean vertMove = oldSelectedPiece.x == gridX && (oldSelectedPiece.y + 1 == gridY || oldSelectedPiece.y - 1 == gridY);
					boolean horzMove = oldSelectedPiece.y == gridY && (oldSelectedPiece.x + 1 == gridX || oldSelectedPiece.x - 1 == gridX);
					TileUnit unit = oldSelectedPiece.tileUnit;
					if ((vertMove || horzMove) && unit != null) {
						if (attackMode == 0 && unit.moves > 0) {
							selectedPiece = gridPieces[gridX][gridY];

							if (selectedPiece != null && selectedPiece.tileType == TileType.Empty1) {
								unit.moves--;
								GridPiece old = gridPieces[unit.gridX][unit.gridY];
								old.tileType = TileType.Empty1;
								old.tileUnit = null;
								unit.gridX = gridX;
								unit.gridY = gridY;
								selectedPiece.tileUnit = unit;
								selectedPiece.tileType = TileType.Blue;
							}
						} else if (attackMode != 0 && unit.canAttack) {
							if (isAttackable(gridX, gridY, TileType.Blue)) {
								int attIdx = attackMode - 1;
								String attackName = attIdx < unit.attacks.size() ? unit.attacks.get(attIdx) : null;
								if (attackName != null) {
									selectedPiece = gridPieces[gridX][gridY];
									Attack attack = attacks.get(attackName);
									selectedPiece.tileUnit.currentHealth -= attack.damage;
									if (selectedPiece.tileUnit.currentHealth <= 0) {
										aiUnitInstances.remove(selectedPiece.tileUnit);
										selectedPiece.tileType = TileType.Empty1;
										selectedPiece.tileUnit = null;
									}
									oldSelectedPiece.tileUnit.moves = 0;
									oldSelectedPiece.tileUnit.canAttack = false;
									oldSelectedPiece.tileUnit.currentHealth -= attack.recoil;
									if (oldSelectedPiece.tileUnit.currentHealth <= 0) {
										playerUnitInstances.remove(oldSelectedPiece.tileUnit);
										oldSelectedPiece.tileType = TileType.Empty1;
										oldSelectedPiece.tileUnit = null;
									}
									nextNonDmgBgTime = System.currentTimeMillis() + 33;
								}
							}
						}
					}
				}

				attackMode = 0;
				selectedPiece = gridPieces[gridX][gridY];
				launchUi.isActive = selectedPiece.tileType == TileType.Launch;
				if (selectedPiece.tileType == null || !selectablePieceTypes.contains(selectedPiece.tileType)) {
					selectedPiece = null;
				}
			} else {
				launchUi.isActive = false;
			}

			if (selectedPiece == null) {
				attackMode = 0;
				unitUi.focusUnit = null;
			} else {
				unitUi.focusUnit = selectedPiece.tileUnit;
			}
		}

		batch.begin();
		for (int a = 0; a < gridPieces.length; a++) {
			for (int b = 0; b < gridPieces[0].length; b++) {
				GridPiece g = gridPieces[a][b];
				String tileTextureName = g.tileType != null ? g.tileType.textureName : null;
				Texture tt = textureMap.get(tileTextureName);
				if (tt != null) {
					float x = g.rectangle.x;
					float y = g.rectangle.y;
					batch.draw(tt, x, y);
					if (g == selectedPiece) {
						batch.draw(textureMap.get("tileSelector"), x, y);
						if (gameState == GameState.PlayerTurn && selectedPiece.tileUnit != null) {
							boolean canMove = attackMode == 0 && selectedPiece.tileUnit.moves > 0;
							boolean canAttack = attackMode != 0 && selectedPiece.tileUnit.canAttack;
							if (canMove || canAttack) {
								if (attackMode != 0) {
									Texture texture = textureMap.get("tileAttackable");
									if (isAttackable(g.x - 1, g.y, TileType.Blue)) batch.draw(texture, x - 32, y);
									if (isAttackable(g.x + 1, g.y, TileType.Blue)) batch.draw(texture, x + 32, y);
									if (isAttackable(g.x, g.y - 1, TileType.Blue)) batch.draw(texture, x, y - 32);
									if (isAttackable(g.x, g.y + 1, TileType.Blue)) batch.draw(texture, x, y + 32);
								} else {
									Texture texture = textureMap.get("tileMovable");
									if (isMovableTo(g.x, g.y, -1, 0)) batch.draw(texture, x - 32, y);
									if (isMovableTo(g.x, g.y, +1, 0)) batch.draw(texture, x + 32, y);
									if (isMovableTo(g.x, g.y, 0, -1)) batch.draw(texture, x, y - 32);
									if (isMovableTo(g.x, g.y, 0, +1)) batch.draw(texture, x, y + 32);
								}
							}
						}
					}
				}
				Texture ut = g.tileUnit != null ? g.tileUnit.texture : null;
				if (ut != null) {
					float x = g.rectangle.x;
					float y = g.rectangle.y;
					batch.draw(ut, x, y);
				}
			}
		}
		launchUi.render(batch);
		unitUi.render(batch, font);
		batch.end();
	}

	public void startPlayerTurn() {
		for (int y = 0; y < gridPieces.length; y++) {
			for (int x = 0; x < gridPieces[0].length; x++) {
				GridPiece gp = gridPieces[y][x];
				if (gp.tileType == TileType.Launch) {
					gp.tileType = TileType.Empty1;
				}
			}
		}
		playerUnitInstances.forEach(unit -> {
			unit.moves = unit.speed;
			unit.canAttack = true;
		});
		bgColor = new Color(.3f, .3f, .4f, 1);
		gameState = GameState.PlayerTurn;
	}

	public void startAiTurn() {
		playerUnitInstances.forEach(unit -> {
			unit.moves = 0;
			unit.canAttack = false;
		});
		aiUnitInstances.forEach(unit -> {
			unit.moves = unit.speed;
			unit.canAttack = true;
		});
		bgColor = new Color(.4f, .3f, .3f, 1);
		gameState = GameState.AiTurn;
	}

	private void performAiAction(TileUnit unit) {
		Point move = getAiMove(unit);
		if (move == null) {
			unit.moves = 0;
			unit.canAttack = false;
		} else {
			if (unit.canAttack && isAttackable(unit.gridX + move.x, unit.gridY + move.y, TileType.Red)) {
				GridPiece enemyPiece = gridPieces[unit.gridX + move.x][unit.gridY + move.y];
				int attIdx = 0;
				String attackName = attIdx < unit.attacks.size() ? unit.attacks.get(attIdx) : null;
				if (attackName != null) {
					Attack attack = attacks.get(attackName);
					enemyPiece.tileUnit.currentHealth -= attack.damage;
					if (enemyPiece.tileUnit.currentHealth <= 0) {
						playerUnitInstances.remove(enemyPiece.tileUnit);
						enemyPiece.tileType = TileType.Empty1;
						enemyPiece.tileUnit = null;
					}
					unit.moves = 0;
					unit.canAttack = false;
					nextNonDmgBgTime = System.currentTimeMillis() + 33;
				}
			} else {
				if (unit.moves > 0) {
					unit.moves--;
					GridPiece oldPiece = gridPieces[unit.gridX][unit.gridY];
					oldPiece.tileUnit = null;
					oldPiece.tileType = TileType.Empty1;
					if (selectedPiece == oldPiece) selectedPiece = null;
					unit.gridX += move.x;
					unit.gridY += move.y;
					GridPiece newPiece = gridPieces[unit.gridX][unit.gridY];
					newPiece.tileUnit = unit;
					newPiece.tileType = TileType.Red;
				} else {
					unit.canAttack = false;
				}
			}
		}
	}

	private Point getAiMove(TileUnit unit) {
		Set<TileType> allowableTiles = setOf(TileType.Empty1, TileType.Blue);
		boolean[][] visited = new boolean[gridWidth][gridHeight];
		Queue<MPoint> visitQ = new LinkedList<>();
		visitQ.add(new MPoint(unit.gridX - 1, unit.gridY, new Point(-1,0)));
		visitQ.add(new MPoint(unit.gridX + 1, unit.gridY, new Point(+1,0)));
		visitQ.add(new MPoint(unit.gridX, unit.gridY - 1, new Point(0,-1)));
		visitQ.add(new MPoint(unit.gridX, unit.gridY + 1, new Point(0,+1)));
		while (!visitQ.isEmpty()) {
			MPoint visitPoint = visitQ.poll();
			if (visited[visitPoint.x][visitPoint.y]) continue;
			visited[visitPoint.x][visitPoint.y] = true;
			GridPiece gp = gridPieces[visitPoint.x][visitPoint.y];
			if ((gp.tileType != TileType.Empty1 && gp.tileType != TileType.Blue)) continue;
			if (gp.tileType == TileType.Blue) return visitPoint.moveDir;
			if (isMovableTo(visitPoint.x, visitPoint.y, -1, 0, allowableTiles)) visitQ.add(new MPoint(visitPoint.x-1, visitPoint.y, visitPoint));
			if (isMovableTo(visitPoint.x, visitPoint.y, +1, 0, allowableTiles)) visitQ.add(new MPoint(visitPoint.x+1, visitPoint.y, visitPoint));
			if (isMovableTo(visitPoint.x, visitPoint.y, 0, -1, allowableTiles)) visitQ.add(new MPoint(visitPoint.x, visitPoint.y-1, visitPoint));
			if (isMovableTo(visitPoint.x, visitPoint.y, 0, +1, allowableTiles)) visitQ.add(new MPoint(visitPoint.x, visitPoint.y+1, visitPoint));
		}
		return null;
	}

	boolean isMovableTo(int fromGridX, int fromGridY, int relX, int relY) {
		int newGridX = fromGridX + relX;
		int newGridY = fromGridY + relY;
		boolean inBoundsX = newGridX >= 0 && newGridX < gridWidth;
		boolean inBoundsY = newGridY >= 0 && newGridY < gridHeight;
		if (!inBoundsX || !inBoundsY) return false;
		TileType tileType = gridPieces[newGridX][newGridY].tileType;
		return tileType != null && movablePieceTypes.contains(tileType);
	}

	boolean isMovableTo(int fromGridX, int fromGridY, int relX, int relY, Set<TileType> allowableTiles) {
		int newGridX = fromGridX + relX;
		int newGridY = fromGridY + relY;
		boolean inBoundsX = newGridX >= 0 && newGridX < gridWidth;
		boolean inBoundsY = newGridY >= 0 && newGridY < gridHeight;
		if (!inBoundsX || !inBoundsY) return false;
		TileType tileType = gridPieces[newGridX][newGridY].tileType;
		return tileType != null && allowableTiles.contains(tileType);
	}

	boolean isAttackable(int gridX, int gridY, TileType attackingUnitType) {
		boolean inBoundsX = gridX >= 0 && gridX < gridWidth;
		boolean inBoundsY = gridY >= 0 && gridY < gridHeight;
		if (!inBoundsX || !inBoundsY) return false;
		if (attackingUnitType == TileType.Blue) {
			return gridPieces[gridX][gridY].tileType == TileType.Red;
		} else {
			return gridPieces[gridX][gridY].tileType == TileType.Blue;
		}
	}

	@SafeVarargs
	public final <T> Set<T> setOf(T... elements) {
		return new HashSet<>(Arrays.asList(elements));
	}
	
	@Override
	public void dispose() {
		batch.dispose();
		textureMap.values().forEach(Texture::dispose);
	}
}
