package rodoco.iachess;

import java.util.Arrays;

import io.github.wolfraam.chessgame.ChessGame;
import io.github.wolfraam.chessgame.board.Side;
import io.github.wolfraam.chessgame.notation.NotationType;

public class ChessBoardSimulator {
	private final char[][] board = new char[8][8];
	private boolean isWhiteTurn = true;
	private ChessGame chessGame = new ChessGame();

	public ChessBoardSimulator() {
		reset();
	}

	public void reset() {
		board[0] = new char[] { 'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r' };
		Arrays.fill(board[1], 'p');
		for (int i = 2; i < 6; i++)
			Arrays.fill(board[i], '.');
		Arrays.fill(board[6], 'P');
		board[7] = new char[] { 'R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R' };
		isWhiteTurn = true;
		chessGame = new ChessGame();
	}

	public char[][] getBoard() {
		return board;
	}

	public boolean isWhiteTurn() {
		return isWhiteTurn;
	}

	public void applyMove(String move) {
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