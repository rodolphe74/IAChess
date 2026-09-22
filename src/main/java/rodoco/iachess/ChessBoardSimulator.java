package rodoco.iachess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.wolfraam.chessgame.ChessGame;
import io.github.wolfraam.chessgame.board.Side;
import io.github.wolfraam.chessgame.move.Move;
import io.github.wolfraam.chessgame.notation.NotationType;

public class ChessBoardSimulator {
	private final char[][] board = new char[8][8];
	private boolean isWhiteTurn = true;
	private ChessGame chessGame = new ChessGame();

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