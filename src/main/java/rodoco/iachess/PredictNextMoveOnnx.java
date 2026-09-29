package rodoco.iachess;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class PredictNextMoveOnnx {

	public static void main(String[] args) throws Exception {
		// 1. Initialisation du moteur ONNX Runtime et chargement du modèle .onnx
		try (OrtEnvironment env = OrtEnvironment.getEnvironment();
				OrtSession session = env.createSession("pytorch-trainer/chess_resnet_model.onnx",
						new OrtSession.SessionOptions())) {

			// Chargement du dictionnaire des coups (mis à jour au format JSON)
			MoveIndexer moveIndexer = MoveIndexer.loadFromJson(new File("pytorch-trainer/move_indexer.json"));

			// 2. Préparation du plateau
			ChessBoardSimulator simulator = new ChessBoardSimulator();
			simulator.applySANMove("e4");
			simulator.applySANMove("e5");

			// 3. Conversion du plateau en données float4D [batch_size=1, channels=14,
			// height=8, width=8]
			float[][][][] boardData = ChessEncoder.boardToFloatArray(simulator.getBoard(), simulator.isWhiteTurn());

			// Création du tenseur d'entrée ONNX
			try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, boardData)) {

				// 4. Inférence (correspond au nom d'entrée "board_input" configuré lors de
				// l'export PyTorch)
				Map<String, OnnxTensor> inputs = Collections.singletonMap("board_input", inputTensor);

				try (OrtSession.Result results = session.run(inputs)) {
					// Extrait la matrice de sortie [1, num_classes] ("policy_output")
					float[][] logits = (float[][]) results.get("policy_output")
						.get()
						.getValue();
					float[] probabilities = logits[0];

					// 5. Recherche de l'index du coup ayant le meilleur score (ArgMax)
					int bestMoveIndex = findArgMax(probabilities);
					String predictedMove = moveIndexer.getMoveFromIndex(bestMoveIndex);

					simulator.applySANMove(predictedMove);

					System.out.println("Coup prédit par le réseau : " + predictedMove);
				}
			}
		}
	}

	/**
	 * Calcule l'index de la valeur maximale dans le tableau de probabilités/logits.
	 */
	private static int findArgMax(float[] array) {
		int maxIdx = 0;
		float maxVal = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] > maxVal) {
				maxVal = array[i];
				maxIdx = i;
			}
		}
		return maxIdx;
	}
}