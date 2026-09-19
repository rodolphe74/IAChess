package rodoco.iachess;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MoveIndexer implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Map<String, Integer> moveToIndex = new HashMap<>();
	private final Map<Integer, String> indexToMove = new HashMap<>();
	private int nextIndex = 0;

	public int getOrCreateIndex(String move) {
		String cleanMove = move.trim();
		if (!moveToIndex.containsKey(cleanMove)) {
			moveToIndex.put(cleanMove, nextIndex);
			indexToMove.put(nextIndex, cleanMove);
			nextIndex++;
		}
		return moveToIndex.get(cleanMove);
	}

	public String getMoveFromIndex(int index) {
		return indexToMove.get(index);
	}

	public int getSize() {
		return nextIndex;
	}

	// Sauvegarde de l'indexeur sur disque
	public void saveToFile(File file) throws IOException {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
			oos.writeObject(this);
		}
	}

	// Chargement de l'indexeur depuis le disque
	public static MoveIndexer loadFromFile(File file) throws IOException, ClassNotFoundException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
			return (MoveIndexer) ois.readObject();
		}
	}
}