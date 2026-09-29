package rodoco.iachess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nd4j.shade.jackson.core.type.TypeReference;
import org.nd4j.shade.jackson.databind.ObjectMapper;

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
	
	public static MoveIndexer loadFromJson(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        MoveIndexer indexer = new MoveIndexer();

        try {
            // Tentative 1 : Format Dictionnaire {"e4": 0, "e5": 1} ou {"0": "e4", "1": "e5"}
            Map<String, Object> map = mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Number) {
                    // {"e4": 0}
                    String move = key;
                    int index = ((Number) value).intValue();
                    indexer.moveToIndex.put(move, index);
                    indexer.indexToMove.put(index, move);
                } else if (value instanceof String) {
                    // {"0": "e4"}
                    int index = Integer.parseInt(key);
                    String move = (String) value;
                    indexer.moveToIndex.put(move, index);
                    indexer.indexToMove.put(index, move);
                }
            }
        } catch (Exception e) {
            // Tentative 2 : Format Liste ["e4", "e5", "Nf3", ...]
            List<String> moves = mapper.readValue(file, new TypeReference<List<String>>() {});
            for (int i = 0; i < moves.size(); i++) {
                indexer.moveToIndex.put(moves.get(i), i);
                indexer.indexToMove.put(i, moves.get(i));
            }
        }

        return indexer;
    }
}