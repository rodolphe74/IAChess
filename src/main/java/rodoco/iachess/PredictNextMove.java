package rodoco.iachess;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.io.File;

public class PredictNextMove {

	public static void main(String[] args) throws Exception {
		// 1. Chargement du modèle et du dictionnaire
		ComputationGraph model = ModelSerializer.restoreComputationGraph(new File("chess_resnet_model.zip"));
		MoveIndexer moveIndexer = MoveIndexer.loadFromFile(new File("move_indexer.ser"));

		// 2. Préparation du plateau dans une position donnée
		ChessBoardSimulator simulator = new ChessBoardSimulator();
		simulator.applyMove("e4");
		simulator.applyMove("e5");

		// 3. Conversion du plateau en tenseur d'entrée
		INDArray inputTensor = ChessEncoder.boardToINDArray(simulator.getBoard(), simulator.isWhiteTurn());

		// 4. Inférence (pass avant)
		INDArray[] output = model.output(inputTensor);
		INDArray probabilities = output[0]; // Vecteur de probabilités (Softmax)

		// 5. Récupération du coup avec la plus haute probabilité
		int bestMoveIndex = Nd4j.argMax(probabilities, 1)
			.getInt(0);
		String predictedMove = moveIndexer.getMoveFromIndex(bestMoveIndex);
		simulator.applyMove(predictedMove);
		ChessBoardPanel.doChessBoard(simulator);

		System.out.println("Coup prédit par le réseau : " + predictedMove);
	}
}