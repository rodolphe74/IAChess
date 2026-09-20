package rodoco.iachess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.wolfraam.chessgame.ChessGame;
import io.github.wolfraam.chessgame.notation.NotationType;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.model.Analysis;

public class CentipawnLossCalculator {

	List<String> exercicesBlancs = new ArrayList<>(Arrays.asList("2r3k1/1p3ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
			"r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
			"r4rk1/ppp2ppp/2n5/1B1p4/3P2b1/2P2N2/P1P2PPP/R3R1K1 w - - 0 1",
			"3r2k1/pp3ppp/8/8/8/3R4/PP3PPP/6K1 w - - 0 1",
			"r1bqk2r/pppp1ppp/2n2n2/4p3/1bB1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 6 5",
			"5rk1/pp3ppp/8/8/1B6/8/PP3PPP/3R2K1 w - - 0 1", "2r3k1/pp3ppp/8/2b5/8/8/PP3PPP/3R2K1 w - - 0 1",
			"r1b1kb1r/pppp1ppp/2n2n2/4p3/2B1P3/1PN2N2/P1PP1PPP/R1BQK2R w KQkq - 1 5",
			"3r2k1/pp3ppp/1b6/8/8/8/PP3PPP/3R2K1 w - - 0 1",
			"r1bqk2r/pppp1ppp/2n2n2/1B2p3/4P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 5",
			"r4rk1/pp3ppp/2n1b3/2b5/8/2N2N2/PP2PPPP/R3KB1R w KQ - 0 1", "4r1k1/pp3ppp/8/8/8/1P6/P4PPP/3R2K1 w - - 0 1",
			"r1bqkb1r/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 2 4",
			"rnbqk2r/ppp2ppp/3p1n2/4p3/2B1P3/2P2N2/PPP2PPP/R1BQK2R w KQkq - 0 5",
			"3r2k1/pp3ppp/8/2B5/8/P7/1P3PPP/3R2K1 w - - 0 1",
			"r1bqk2r/ppp2ppp/2n5/3pp3/2B1P3/3P1N2/PPP2PPP/RN1QK2R w KQkq - 0 6",
			"2r3k1/pp3ppp/b7/8/8/8/PP3PPP/3R2K1 w - - 0 1", "r4rk1/pp3ppp/n1p5/8/3P4/2N5/PP3PPP/R4RK1 w - - 0 1",
			"r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 4 4",
			"4r1k1/pp3ppp/2b5/8/8/8/PP3PPP/3R2K1 w - - 0 1",
			"r3kb1r/ppp2ppp/2n5/3qp3/3P4/5N2/PPP2PPP/R1BQK2R w KQkq - 0 8",
			"3r2k1/pp3ppp/8/b7/8/8/PP3PPP/3R2K1 w - - 0 1",
			"r1bqk2r/pppp1ppp/2n5/4p3/2B1P1n1/3P1N2/PPP2PPP/RNBQK2R w KQkq - 1 5",
			"2r3k1/pp3ppp/8/8/3P4/8/PP3PPP/3R2K1 w - - 0 1",
			"r1bqk2r/ppp2ppp/2n5/2bpp3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 0 6"));

	// ⚫ Liste des exercices pour les Noirs (Le trait est aux Noirs : "b")
	List<String> exercicesNoirs = new ArrayList<>(Arrays.asList("3r2k1/5ppp/8/8/8/8/5PPP/3R2K1 b - - 0 1",
			"rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR b KQkq - 1 3",
			"r1bqk2r/ppp2ppp/2n5/3pp3/2B1P3/3P1N2/PPP2PPP/RN1QK2R b KQkq - 0 6",
			"3r2k1/5ppp/8/8/8/3R4/5PPP/6K1 b - - 0 1",
			"r1bqk2r/pppp1ppp/2n2n2/4p3/1bB1P3/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 6 5",
			"5rk1/5ppp/8/8/1B6/8/5PPP/3R2K1 b - - 0 1", "2r3k1/5ppp/8/2b5/8/8/5PPP/3R2K1 b - - 0 1",
			"r1bqkb1r/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 2 4",
			"3r2k1/5ppp/1b6/8/8/8/5PPP/3R2K1 b - - 0 1",
			"r1bqk2r/pppp1ppp/2n2n2/1B2p3/4P3/2N2N2/PPPP1PPP/R1BQK2R b KQkq - 4 5",
			"r4rk1/5ppp/2n1b3/2b5/8/2N2N2/4PPPP/R3KB1R b KQ - 0 1", "4r1k1/5ppp/8/8/8/1P6/5PPP/3R2K1 b - - 0 1",
			"rnbqk2r/ppp2ppp/3p1n2/4p3/2B1P3/2P2N2/PPP2PPP/R1BQK2R b KQkq - 0 5",
			"3r2k1/5ppp/8/2B5/8/P7/5PPP/3R2K1 b - - 0 1", "2r3k1/5ppp/b7/8/8/8/5PPP/3R2K1 b - - 0 1",
			"r4rk1/5ppp/n1p5/8/3P4/2N5/5PPP/R4RK1 b - - 0 1",
			"r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R b KQkq - 4 4",
			"4r1k1/5ppp/2b5/8/8/8/5PPP/3R2K1 b - - 0 1", "r3kb1r/ppp2ppp/2n5/3qp3/3P4/5N2/PPP2PPP/R1BQK2R b KQkq - 0 8",
			"3r2k1/5ppp/8/b7/8/8/5PPP/3R2K1 b - - 0 1",
			"r1bqk2r/pppp1ppp/2n5/4p3/2B1P1n1/3P1N2/PPP2PPP/RNBQK2R b KQkq - 1 5",
			"2r3k1/5ppp/8/8/3P4/8/5PPP/3R2K1 b - - 0 1",
			"r1bqk2r/ppp2ppp/2n5/2bpp3/4P3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 6",
			"2r3k1/5ppp/p7/8/8/8/5PPP/3R2K1 b - - 0 1",
			"r1b1kb1r/pppp1ppp/2n2n2/4p3/2B1P3/1PN2N2/P1PP1PPP/R1BQK2R b KQkq - 1 5"));

	private UCI uci;

	// Démarrer Stockfish via neatchess
	public void startEngine(String stockfishPath) {
		this.uci = new UCI();
		this.uci.start(stockfishPath);
	}

	/**
	 * Évalue une position FEN du point de vue du joueur qui vient de jouer (ou qui
	 * doit jouer).
	 */
	public int evaluateFen(String fen, int depth) {
		uci.positionFen(fen);
		Analysis analysis = uci.analysis(depth)
			.getResult();
		net.andreinc.neatchess.client.model.Move bestMove = analysis.getBestMove();

		if (bestMove == null || bestMove.getStrength() == null) {
			return 0;
		}

		var strength = bestMove.getStrength();

		// 1. Gestion des opportunités de Mat
//	    if (strength.getMateIn() != 0) {
//	        int mateIn = strength.getMateIn();
//	        return mateIn > 0 ? (10000 - mateIn * 100) : (-10000 - mateIn * 100);
//	    }

		// 2. Score en centipawns (Stockfish donne un score en pions float, ex: 1.5 ->
		// 150 cp)
		if (strength.getScore() != null) {
			return (int) Math.round(strength.getScore() * 100.0);
		}

		return 0;
	}

	public int getCentipawnLoss(String fenBeforeMove, String playerMoveSan, int depth) {
		// 1. Évaluation de la meilleure option AVANT le coup (du point de vue du
		// joueur)
		int evalBefore = evaluateFen(fenBeforeMove, depth);

		// 2. Application du coup sur l'échiquier virtuel (wolfraam)
		ChessGame chessGame = new ChessGame(fenBeforeMove);

		String targetSan = playerMoveSan.trim();
		io.github.wolfraam.chessgame.move.Move matchedMove = null;

		for (io.github.wolfraam.chessgame.move.Move m : chessGame.getLegalMoves()) {
			String encodedSan = chessGame.getNotation(NotationType.SAN, m);
			if (targetSan.equals(encodedSan)) {
				matchedMove = m;
				break;
			}
		}

		if (matchedMove == null) {
			throw new IllegalArgumentException("Coup illégal ou mal formé : " + targetSan);
		}

		chessGame.playMove(matchedMove);

		// 3. Évaluation APRÈS le coup
		// Le trait a changé pour l'adversaire, donc on inverse le signe (-)
		// pour garder la valeur du point de vue de NOTRE joueur
		int evalAfter = -evaluateFen(chessGame.getFen(), depth);

		// 4. Perte en centipawns = Évaluation idéale - Évaluation réelle
		int loss = evalBefore - evalAfter;

		return Math.max(0, loss);
	}

	public void stopEngine() {
		if (this.uci != null) {
			this.uci.close();
		}
	}

	public static void main(String[] args) {
		CentipawnLossCalculator calculator = new CentipawnLossCalculator();
		try {
			String stockfishPath = "bin/stockfish-windows-x86-64-universal.exe";
			calculator.startEngine(stockfishPath);

			System.out.println("--- ANALYSE FIABLE AVEC CHESS-GAME ---");

			// Position initiale (Trait aux Blancs)
			String fenInitiale = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3";

			// Le coup Blunder joué par le Blanc au format standard SAN
			String coupJoueurSAN = "Nxe5";
//			String coupJoueurSAN = "Bb5";

			System.out.println("Calcul en cours...");
			int centipawnLoss = calculator.getCentipawnLoss(fenInitiale, coupJoueurSAN, 14);

			System.out.println("\n--- RÉSULTAT ---");
			System.out.println("❌ Perte en centipawns sur ce coup : " + centipawnLoss + " CPL");

			if (centipawnLoss == 0)
				System.out.println("=> Coup parfait (Top Engine Move).");
			else if (centipawnLoss <= 10)
				System.out.println("=> Excellent coup.");
			else if (centipawnLoss <= 30)
				System.out.println("=> Inexactitude (Inaccuracy).");
			else if (centipawnLoss <= 70)
				System.out.println("=> Erreur (Mistake).");
			else
				System.out.println("=> Gaffe majeure (Blunder).");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			calculator.stopEngine();
			System.out.println("Moteur arrêté.");
		}
	}
}
