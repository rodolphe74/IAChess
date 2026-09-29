package rodoco.iachess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.wolfraam.chessgame.ChessGame;
import io.github.wolfraam.chessgame.board.Piece;
import io.github.wolfraam.chessgame.board.PieceType;
import io.github.wolfraam.chessgame.board.Side;
import io.github.wolfraam.chessgame.move.Move;
import io.github.wolfraam.chessgame.notation.NotationType;
import io.github.wolfraam.chessgame.result.ChessGameResultType;

public class ChessBoardSimulator {
	private final char[][] board = new char[8][8];
	private boolean isWhiteTurn = true;
	private ChessGame chessGame = new ChessGame();

	
	public static ComputationGraph model;
	public static MoveIndexer moveIndexer;
	
	public static OrtEnvironment onnxEnv;
	public static OrtSession onnxSession;
	public static MoveIndexer moveIndexerOnnx;

	static {
		try {
			model = ModelSerializer.restoreComputationGraph(new File("chess_resnet_model.zip"));
			moveIndexer = MoveIndexer.loadFromFile(new File("move_indexer.ser"));

			// MOdele Onnx
			onnxEnv = OrtEnvironment.getEnvironment();
			onnxSession = onnxEnv.createSession("pytorch-trainer/chess_resnet_model.onnx",
					new OrtSession.SessionOptions());

			// Chargement du dictionnaire des coups (mis à jour au format JSON)
			moveIndexerOnnx = MoveIndexer.loadFromJson(new File("pytorch-trainer/move_indexer.json"));
		} catch (IOException | ClassNotFoundException | OrtException e) {
			e.printStackTrace();
		}

	}
	
	
	public ChessBoardSimulator() {
		reset();
	}
	
	public ChessBoardSimulator(String fen) {
		reset(fen);
	}

	public void reset() {
		chessGame = new ChessGame();
		syncMatrixFromFen(chessGame.getFen());
		isWhiteTurn = (chessGame.getSideToMove() == Side.WHITE);
	}
	
	public void reset(String fen) {
		chessGame = new ChessGame(fen);
		syncMatrixFromFen(fen);
		isWhiteTurn = (chessGame.getSideToMove() == Side.WHITE);
	}

	public char[][] getBoard() {
		return board;
	}

	public boolean isWhiteTurn() {
		return isWhiteTurn;
	}

	public void applySANMove(String move) {
		if (move == null || move.isBlank()) {
			return;
		}

		try {
			// 1. Application du coup SAN (gestion automatique du roque, promotions, prises,
			// etc.)
			chessGame.playMove(NotationType.SAN, move.trim());

			// 2. Synchronisation de la matrice 2D à partir du FEN généré
			syncMatrixFromFen(chessGame.getFen());

			// 3. Mise à jour du trait
			this.isWhiteTurn = chessGame.getSideToMove()
				.equals(Side.WHITE);

			System.out.println(chessGame.getASCII());
			printBoard(board);

		} catch (IllegalArgumentException e) {
			// Le coup est invalide ou illégal dans la position actuelle
			System.err.println("Coup illégal ou mal formé : " + move);
		}
	}
	
	public void applyUCIMove(String move) {
		Move m  = chessGame.getMove(NotationType.UCI, move);
		String encodedSan = chessGame.getNotation(NotationType.SAN, m);
		applySANMove(encodedSan);
	}

	/**
	 * Reconstruit la matrice char[8][8] à partir de la notation FEN
	 */
	private void syncMatrixFromFen(String fen) {
		String placement = fen.split(" ")[0];
		int row = 0;
		int col = 0;

		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				this.board[r][c] = '.';
			}
		}

		for (char c : placement.toCharArray()) {
			if (c == '/') {
				row++;
				col = 0;
			} else if (Character.isDigit(c)) {
				col += Character.getNumericValue(c);
			} else {
				this.board[row][col] = c;
				col++;
			}
		}
	}
	
	public List<String> getMoves() {
		Set<Move> moves = chessGame.getLegalMoves();
		ArrayList<String> legalMoves = new ArrayList<String>();
		for (Move m : moves) {
			legalMoves.add(chessGame.getNotation(NotationType.UCI, m));
		}
		return legalMoves;
	}
	
	
	
	public String predictMove() {
		Set<Move> legalMoves = chessGame.getLegalMoves();

		// S'il n'y a aucun coup légal (mat ou pat), la partie est terminée
		if (legalMoves.isEmpty()) {
			return null;
		}
		
		// --- HEURISTIQUE 1 : Mat immédiat (Priorité absolue) ---
	    for (Move move : legalMoves) {
	        if (isCheckmateMove(move)) {
	        	System.out.println("CHECKMATE!!");
	            return chessGame.getNotation(NotationType.SAN, move);
	        }
	    }

		// Passage dans le réseau de neurones
		INDArray inputTensor = ChessEncoder.boardToINDArray(getBoard(), isWhiteTurn());
		INDArray[] output = model.output(inputTensor);

		// On travaille idéalement sur les logits avant Softmax, sinon sur le vecteur de
		// probabilités
		INDArray probabilities = output[0].dup(); // Copie pour ne pas altérer l'output du modèle

		// Création du masque d'illégalité
		// On initialise un tableau où 0 = légal, et -Infinity = illégal
		int numPossibleMoves = (int) probabilities.length();
		boolean[] legalMask = new boolean[numPossibleMoves];

		for (Move move : legalMoves) {
			try {
				String sanMove = chessGame.getNotation(NotationType.SAN, move);
				int moveIndex = moveIndexer.getOrCreateIndex(sanMove); // Méthode inverse de getMoveFromIndex
				if (moveIndex >= 0 && moveIndex < numPossibleMoves) {
					legalMask[moveIndex] = true;
					
					// --- HEURISTIQUE 2 : Bonus tactiques (Captures, Promotions, Échecs) ---
	                double currentScore = probabilities.getDouble(0, moveIndex);
	                double bonus = evaluateMoveHeuristic(move);
	                
	                System.out.println(move + ": s=" + currentScore + " b=" + bonus);
	                // ajuste le score brut du réseau
	                probabilities.putScalar(new int[] { 0, moveIndex }, currentScore + bonus);
				}
			} catch (NoSuchElementException e) {
				System.err.println("FEN illégale détectée, impossible de calculer le SAN : " + chessGame.getFen());
				return null;
			}
		}

		// Masquage des coups illégaux
		for (int i = 0; i < numPossibleMoves; i++) {
			if (!legalMask[i]) {
				// On met la probabilité à une valeur extrêmement basse
				// pour empêcher argMax de la sélectionner
				probabilities.putScalar(new int[] { 0, i }, -1e9);
			}
		}

		// Extraction du meilleur coup PARMI LES COUPS LÉGAUX
		int bestLegalMoveIndex = Nd4j.argMax(probabilities, 1)
			.getInt(0);
		String predictedMove = moveIndexer.getMoveFromIndex(bestLegalMoveIndex);

		// Fallback de sécurité extrême (si aucun index valide n'a été trouvé par le
		// moveIndexer)
		if (predictedMove == null || createMoveFromSan(chessGame, predictedMove, null) == null) {
			Move defaultMove = ((Move) chessGame.getLegalMoves()
				.toArray()[0]);
			return chessGame.getNotation(NotationType.SAN, defaultMove);
		}

		return predictedMove;
	}
	
	
	public String predictMoveOnnx() {
		Set<Move> legalMoves = chessGame.getLegalMoves();

		// S'il n'y a aucun coup légal (mat ou pat), la partie est terminée
		if (legalMoves.isEmpty()) {
			return null;
		}

		// 1. Encodage du plateau au format float[1][14][8][8]
		float[][][][] boardData = ChessEncoder.boardToFloatArray(getBoard(), isWhiteTurn());

		float[] probabilities;

		// 2. Inférence ONNX Runtime
		try (OnnxTensor inputTensor = OnnxTensor.createTensor(onnxEnv, boardData);
				OrtSession.Result results = onnxSession.run(Collections.singletonMap("board_input", inputTensor))) {
			float[][] logits = (float[][]) results.get("policy_output")
				.get()
				.getValue();
			probabilities = logits[0].clone(); // Copie locale pour le masquage
		} catch (OrtException e) {
			e.printStackTrace();
			return null;
		}

		// 3. Masquage des coups illégaux
		int numPossibleMoves = probabilities.length;
		boolean[] legalMask = new boolean[numPossibleMoves];

		for (Move move : legalMoves) {
			try {
				String sanMove = chessGame.getNotation(NotationType.SAN, move);
				int moveIndex = moveIndexerOnnx.getOrCreateIndex(sanMove);
				if (moveIndex >= 0 && moveIndex < numPossibleMoves) {
					legalMask[moveIndex] = true;
				}
			} catch (NoSuchElementException e) {
				System.err.println("FEN illégale détectée, impossible de calculer le SAN : " + chessGame.getFen());
				return null;
			}
		}

		// On applique la pénalité sur le tableau float
		for (int i = 0; i < numPossibleMoves; i++) {
			if (!legalMask[i]) {
				probabilities[i] = -1e9f;
			}
		}

		// 4. ArgMax : Récupération du meilleur coup parmi les coups légaux
		int bestLegalMoveIndex = 0;
		float maxVal = probabilities[0];
		for (int i = 1; i < numPossibleMoves; i++) {
			if (probabilities[i] > maxVal) {
				maxVal = probabilities[i];
				bestLegalMoveIndex = i;
			}
		}

		String predictedMove = moveIndexerOnnx.getMoveFromIndex(bestLegalMoveIndex);

		// 5. Fallback de sécurité
		if (predictedMove == null || createMoveFromSan(chessGame, predictedMove, null) == null) {
			Move defaultMove = chessGame.getLegalMoves()
				.iterator()
				.next();
			return chessGame.getNotation(NotationType.SAN, defaultMove);
		}

		return predictedMove;
		
	}
	
	
	/**
	 * Vérifie si le coup mène immédiatement au mat en simulant le coup sur une
	 * copie de la partie.
	 */
	private boolean isCheckmateMove(Move move) {
		// Si ta bibliothèque permet de simuler facilement un coup :
		ChessGame copy = chessGame.clone(); // Adapt selon ton API de jeu
		copy.playMove(move);
		return (copy.getGameResult() != null
				&& (copy.getGameResult().chessGameResultType == ChessGameResultType.BLACK_WINS
						|| copy.getGameResult().chessGameResultType == ChessGameResultType.WHITE_WINS));
	}

	private double evaluateMoveHeuristic(Move move) {
		double bonus = 0.0;

		// On récupère les pièces depuis l'état actuel du plateau / jeu
		Piece attacker = chessGame.getPiece(move.from);
		Piece target = chessGame.getPiece(move.to);

		// 1. Capture classique (la case de destination contient une pièce ennemie)
		if (target != null) {
			double victimValue = getPieceValue(target);
			double attackerValue = getPieceValue(attacker);
			bonus += 1.0 + (victimValue - attackerValue * 0.1);
		}
		// Cas particulier : En Passant (si la pièce attaquante est un pion qui change
		// de colonne vers une case vide)
		else if (isPawn(attacker) && move.from.x != move.to.x) {
			bonus += 1.0; // Capture en passant (valeur d'un pion)
		}

		// 2. Bonus pour la promotion d'un pion
		if (move.promotion != null) {
			bonus += 4.0;
		}

		return bonus;
	}

	private boolean isPawn(Piece piece) {
		if (piece == null)
			return false;
		return piece.pieceType == PieceType.PAWN; // Adapter selon les getters de ta classe Piece
	}

	private double getPieceValue(Piece piece) {
		if (piece == null)
			return 0.0;
		switch (piece.pieceType) {
		case PAWN:
			return 1.0;
		case KNIGHT:
			return 3.0;
		case BISHOP:
			return 3.0;
		case ROOK:
			return 5.0;
		case QUEEN:
			return 9.0;
		default:
			return 0.0;
		}
	}
	
	private io.github.wolfraam.chessgame.move.Move createMoveFromSan(ChessGame chessGame, String targetSan,
			io.github.wolfraam.chessgame.move.Move matchedMove) {
		for (io.github.wolfraam.chessgame.move.Move m : chessGame.getLegalMoves()) {
			String encodedSan = chessGame.getNotation(NotationType.SAN, m);
			if (targetSan.equals(encodedSan)) {
				matchedMove = m;
				break;
			}
		}
		return matchedMove;
	}
	
	

	public static void printBoard(char[][] board) {
		System.out.println("    a b c d e f g h");
		System.out.println("   -----------------");

		// On parcourt de la ligne 0 (haut/Noirs) à la ligne 7 (bas/Blancs)
		for (int i = 0; i < board.length; i++) {
			// Affiche le numéro de la rangée (8 en haut, 1 en bas)
			System.out.print((8 - i) + " | ");

			for (int j = 0; j < board[i].length; j++) {
				System.out.print(board[i][j] + " ");
			}

			// Rappel du numéro de ligne à droite pour la lisibilité
			System.out.println("| " + (8 - i));
		}

		System.out.println("   -----------------");
		System.out.println("    a b c d e f g h");
	}

}