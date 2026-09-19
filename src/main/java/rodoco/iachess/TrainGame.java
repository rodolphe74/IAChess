package rodoco.iachess;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;

public class TrainGame {

    public static void main(String[] args) {
        // 1. Données de la partie CSV
        String line = "TZJHLljE,FALSE,1.50421E+12,1.50421E+12,13,outoftime,white,15+2,bourgris,1500,a-00,1191,d4 d5 c4 c6 cxd5 e6 dxe6 fxe6 Nf3 Bb4+ Nc3 Ba5 Bf4,D10,Slav Defense: Exchange Variation,5";
        String[] columns = line.split(",");

        String winner = columns[6].trim();      // "white"
        String movesStr = columns[12].trim();   // "d4 d5 c4 c6..."
        String[] moves = movesStr.split("\\s+");

        // 2. Initialisation des composants
        MoveIndexer moveIndexer = new MoveIndexer();
        ChessBoardSimulator simulator = new ChessBoardSimulator();
        ComputationGraph model = ChessResNetDL4J.createChessModel(14, 4672);

        List<INDArray> inputsList = new ArrayList<>();
        List<INDArray> targetsList = new ArrayList<>();

        // 3. Boucle sur chaque coup de la partie
        for (String move : moves) {
            boolean isWhiteTurn = simulator.isWhiteTurn();

            // On extrait UNIQUEMENT les coups joués par le gagnant (ici Blanc)
            boolean isWinnerMove = (isWhiteTurn && winner.equals("white")) || (!isWhiteTurn && winner.equals("black"));

            if (isWinnerMove) {
                // A. Capturer l'état du plateau AVANT le coup (14, 8, 8)
                INDArray inputTensor = ChessEncoder.boardToINDArray(simulator.getBoard(), isWhiteTurn);

                // B. Récupérer l'index du coup et créer le vecteur One-Hot (4672)
                int moveIndex = moveIndexer.getOrCreateIndex(move);
                INDArray targetOutput = Nd4j.zeros(1, 4672);
                targetOutput.putScalar(new int[]{0, moveIndex}, 1.0);

                inputsList.add(inputTensor);
                targetsList.add(targetOutput);
            }

            // C. Mettre à jour le plateau pour le coup suivant
            simulator.applyMove(move);
        }

        // 4. Regroupement des exemples en un seul DataSet (Batch)
        INDArray batchInputs = Nd4j.vstack(inputsList);   // Forme : [7, 14, 8, 8]
        INDArray batchTargets = Nd4j.vstack(targetsList); // Forme : [7, 4672]

        DataSet dataSet = new DataSet(batchInputs, batchTargets);

        // 5. Entraînement du modèle
        model.fit(dataSet);

        System.out.println("Entraînement réussi sur " + inputsList.size() + " coups gagants de la partie !");
    }
}