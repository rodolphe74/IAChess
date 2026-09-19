package rodoco.iachess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

public class TrainFullCsv {

	public static void main(String[] args) {
		
		// Ajuste selon le nombre de cœurs physiques de ton CPU (ex: 8)
		System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "16G");
		
		// Nom du fichier situe dans src/main/resources/
		String resourceName = "games.csv";
		int batchSize = 128; // Taille du batch
		int epochs = 5; // Nombre de passes sur le jeu de donnees

		MoveIndexer moveIndexer = new MoveIndexer();
		ChessBoardSimulator simulator = new ChessBoardSimulator();

		try {
			// STEP 1 : Premier passage pour recenser tous les coups uniques du CSV dans le
			// Classpath
			System.out.println("1. Scan du fichier CSV dans le Classpath...");
			scanUniqueMoves(resourceName, moveIndexer);
			int numClasses = moveIndexer.getSize();
			System.out.printf("Dictionnaire cree : %d coups uniques identifies.%n", numClasses);

			// STEP 2 : Initialisation du reseau ResNet DL4J
			System.out.println("2. Initialisation du reseau de neurones DL4J...");
			ComputationGraph model = ChessResNetDL4J.createChessModel(14, numClasses);
			
			
			// -----------------------------------------------------------------
		    // STEP 2.5 : INITIALISATION DU SERVEUR DL4J UI
		    // -----------------------------------------------------------------
			UIServer uiServer = UIServer.getInstance();
			if (uiServer != null) {
			    StatsStorage statsStorage = new InMemoryStatsStorage();
			    uiServer.attach(statsStorage);
			    model.setListeners(new StatsListener(statsStorage));
			    System.out.println("Interface disponible sur http://localhost:9000");
			} else {
			    System.err.println("Impossible de démarrer le serveur UI.");
			}
		    // -----------------------------------------------------------------
			
			

			// STEP 3 : Boucle d'entrainement
			System.out.println("3. Debut de l'entrainement...");
			for (int epoch = 1; epoch <= epochs; epoch++) {
				System.out.printf("--- Epoque %d / %d ---%n", epoch, epochs);

				long startTime = System.currentTimeMillis();
				int totalSamples = trainOneEpoch(model, resourceName, moveIndexer, simulator, batchSize, numClasses);
				long duration = (System.currentTimeMillis() - startTime) / 1000;

				System.out.printf("Epoque %d terminee : %d exemples entraines en %d s.%n", epoch, totalSamples,
						duration);
			}

			System.out.println("\nEntrainement complet termine avec succes !");
			
			
			System.out.println("\nSauvegarde du modèle et du dictionnaire...");

			File modelFile = new File("chess_resnet_model.zip");
			File indexerFile = new File("move_indexer.ser");

			// 1. Sauvegarde du réseau de neurones (modèle + état de l'optimiseur Adam)
			boolean saveUpdater = true; // Permet de reprendre l'entraînement plus tard si besoin
			ModelSerializer.writeModel(model, modelFile, saveUpdater);
			System.out.println("Modèle sauvegardé dans : " + modelFile.getAbsolutePath());

			// 2. Sauvegarde du dictionnaire d'index
			moveIndexer.saveToFile(indexerFile);
			System.out.println("Dictionnaire sauvegardé dans : " + indexerFile.getAbsolutePath());
			
			System.out.println(model.summary());
			

		} catch (IOException e) {
			System.err.println("Erreur lors de la lecture du fichier dans le classpath : " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Lit le CSV depuis le Classpath une premiere fois pour remplir le MoveIndexer.
	 */
	private static void scanUniqueMoves(String resourcePath, MoveIndexer moveIndexer) throws IOException {
		InputStream is = TrainFullCsv.class.getClassLoader()
			.getResourceAsStream(resourcePath);
		if (is == null) {
			throw new IllegalArgumentException("Ressource introuvable dans le classpath : " + resourcePath);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line = reader.readLine(); // Ignore l'en-tete
			while ((line = reader.readLine()) != null) {
				String[] columns = line.split(",");
				if (columns.length < 13)
					continue;

				String movesStr = columns[12].trim();
				String[] moves = movesStr.split("\\s+");

				for (String move : moves) {
					if (!move.isEmpty()) {
						moveIndexer.getOrCreateIndex(move);
					}
				}
			}
		}
	}

	/**
	 * Parcourt le CSV depuis le Classpath, rejoue les parties et entraine le modele
	 * par batchs.
	 */
	private static int trainOneEpoch(ComputationGraph model, String resourcePath, MoveIndexer moveIndexer,
			ChessBoardSimulator simulator, int batchSize, int numClasses) throws IOException {
		int sampleCount = 0;
		int batchCount = 0;

		List<INDArray> inputsList = new ArrayList<>();
		List<INDArray> targetsList = new ArrayList<>();

		long size = countCsvLines(resourcePath);
		long current = 0;
		
		InputStream is = TrainFullCsv.class.getClassLoader()
			.getResourceAsStream(resourcePath);
		if (is == null) {
			throw new IllegalArgumentException("Ressource introuvable dans le classpath : " + resourcePath);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			reader.readLine(); // Ignore l'en-tete
			String line;

			while ((line = reader.readLine()) != null) {

				System.out.println(current++  + "/" + size);
				
//				if (current == 10)
//					break;

				String[] columns = line.split(",");
				if (columns.length < 13)
					continue;

				String winner = columns[6].trim()
					.toLowerCase(); // "white", "black", ou "draw"
				String movesStr = columns[12].trim();
				String[] moves = movesStr.split("\\s+");

				// Reinitialise le plateau pour chaque nouvelle partie
				simulator.reset();

				for (String move : moves) {
					if (move.isEmpty())
						continue;

					boolean isWhiteTurn = simulator.isWhiteTurn();

					// FILTRAGE : On n'apprend QUE des coups joues par le GAGNANT
					boolean isWinnerMove = (isWhiteTurn && winner.equals("white"))
							|| (!isWhiteTurn && winner.equals("black"));

					if (isWinnerMove) {
						// 1. Capture de l'etat du plateau AVANT d'appliquer le coup
						INDArray inputTensor = ChessEncoder.boardToINDArray(simulator.getBoard(), isWhiteTurn);

						// 2. Creation du vecteur cible One-Hot
						int moveIdx = moveIndexer.getOrCreateIndex(move);
						INDArray targetOutput = Nd4j.zeros(1, numClasses);
						targetOutput.putScalar(new int[] { 0, moveIdx }, 1.0);

						inputsList.add(inputTensor);
						targetsList.add(targetOutput);
						sampleCount++;

						// 3. Quand la taille du batch est atteinte, on entraine le reseau
						if (inputsList.size() == batchSize) {
							fitBatch(model, inputsList, targetsList);
							batchCount++;
							if (batchCount % 100 == 0) {
								System.out.printf("  > Batches traites : %d (%d positions)%n", batchCount, sampleCount);
							}
						}
					}

					// Avancer le plateau pour le coup suivant
					simulator.applyMove(move);
				}
			}

			// Traitement des exemples restants dans le dernier batch
			if (!inputsList.isEmpty()) {
				fitBatch(model, inputsList, targetsList);
			}
		}

		return sampleCount;
	}

	/**
	 * Empile les tenseurs de la liste et lance model.fit().
	 */
	private static void fitBatch(ComputationGraph model, List<INDArray> inputsList, List<INDArray> targetsList) {
		INDArray batchInputs = Nd4j.vstack(inputsList); // Forme : [batchSize, 14, 8, 8]
		INDArray batchTargets = Nd4j.vstack(targetsList); // Forme : [batchSize, numClasses]

		DataSet dataSet = new DataSet(batchInputs, batchTargets);
		model.fit(dataSet);

		// Liberation du buffer
		inputsList.clear();
		targetsList.clear();
	}

	public static long countCsvLines(String resourcePath) throws IOException {
		InputStream is = TrainFullCsv.class.getClassLoader()
			.getResourceAsStream(resourcePath);
		if (is == null) {
			throw new IllegalArgumentException("Ressource introuvable dans le classpath : " + resourcePath);
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			// Compte toutes les lignes et soustrait 1 pour ignorer l'en-tête
			long totalLines = reader.lines()
				.count();
			return Math.max(0, totalLines - 1);
		}
	}
}
