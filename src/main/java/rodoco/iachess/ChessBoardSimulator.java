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
            // 1. Application du coup SAN (gestion automatique du roque, promotions, prises, etc.)
            chessGame.playMove(NotationType.SAN, move.trim());

            // 2. Synchronisation de la matrice 2D à partir du FEN généré
            syncMatrixFromFen(chessGame.getFen());

            // 3. Mise à jour du trait
            this.isWhiteTurn = chessGame.getSideToMove().equals(Side.WHITE);
            
            System.out.println(chessGame.getASCII());

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

	private int[] findSourcePiece(char piece, int destRow, int destCol, Character disambigCol, Character disambigRow) {
	    char pUpper = Character.toUpperCase(piece);

	    // 1. GESTION DES PIONS
	    if (pUpper == 'P') {
	        int dir = isWhiteTurn ? 1 : -1;
	        if (isValid(destRow + dir, destCol) && board[destRow + dir][destCol] == piece)
	            return new int[] { destRow + dir, destCol };
	        if (isValid(destRow + 2 * dir, destCol) && board[destRow + 2 * dir][destCol] == piece)
	            return new int[] { destRow + 2 * dir, destCol };
	        if (isValid(destRow + dir, destCol - 1) && board[destRow + dir][destCol - 1] == piece)
	            return new int[] { destRow + dir, destCol - 1 };
	        if (isValid(destRow + dir, destCol + 1) && board[destRow + dir][destCol + 1] == piece)
	            return new int[] { destRow + dir, destCol + 1 };
	    }

	    // 2. OFFSETS DU CAVALIER
	    int[][] knightOffsets = {
	        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
	        {1, -2}, {1, 2}, {2, -1}, {2, 1}
	    };

	    for (int r = 0; r < 8; r++) {
	        for (int c = 0; c < 8; c++) {
	            if (board[r][c] == piece) {

	                // Filtres de désambiguïsation
	                if (disambigCol != null && c != (disambigCol - 'a')) continue;
	                if (disambigRow != null && r != (8 - Character.getNumericValue(disambigRow))) continue;

	                int dr = Math.abs(r - destRow);
	                int dc = Math.abs(c - destCol);

	                // Validation selon le type de pièce
	                boolean validMove = false;
	                switch (pUpper) {
	                    case 'N': // Cavalier
	                        for (int[] offset : knightOffsets) {
	                            if (r + offset[0] == destRow && c + offset[1] == destCol) {
	                                validMove = true;
	                                break;
	                            }
	                        }
	                        break;

	                    case 'B': // Fou (déplacement diagonal strict + chemin libre)
	                        if (dr == dc && dr > 0) {
	                            validMove = isPathClear(r, c, destRow, destCol);
	                        }
	                        break;

	                    case 'R': // Tour (déplacement rectiligne + chemin libre)
	                        if ((r == destRow || c == destCol) && (dr > 0 || dc > 0)) {
	                            validMove = isPathClear(r, c, destRow, destCol);
	                        }
	                        break;

	                    case 'Q': // Dame (combiné Tour + Fou + chemin libre)
	                        if ((dr == dc || r == destRow || c == destCol) && (dr > 0 || dc > 0)) {
	                            validMove = isPathClear(r, c, destRow, destCol);
	                        }
	                        break;

	                    case 'K': // Roi (1 seule case autour)
	                        if (dr <= 1 && dc <= 1 && (dr > 0 || dc > 0)) {
	                            validMove = true;
	                        }
	                        break;

	                    default:
	                        validMove = true;
	                }

	                if (validMove) {
	                    return new int[] { r, c };
	                }
	            }
	        }
	    }
	    return null;
	}

	// Méthode utilitaire pour vérifier qu'aucune pièce n'obstacle la trajectoire
	private boolean isPathClear(int srcRow, int srcCol, int destRow, int destCol) {
	    int stepRow = Integer.compare(destRow, srcRow);
	    int stepCol = Integer.compare(destCol, srcCol);

	    int r = srcRow + stepRow;
	    int c = srcCol + stepCol;

	    while (r != destRow || c != destCol) {
	        if (board[r][c] != '.') {
	            return false; // Obstacle trouvé
	        }
	        r += stepRow;
	        c += stepCol;
	    }
	    return true;
	}

	private void applyCastling(boolean kingside) {
		int row = isWhiteTurn ? 7 : 0;
		char king = isWhiteTurn ? 'K' : 'k';
		char rook = isWhiteTurn ? 'R' : 'r';

		if (kingside) {
			board[row][4] = '.';
			board[row][7] = '.';
			board[row][6] = king;
			board[row][5] = rook;
		} else {
			board[row][4] = '.';
			board[row][0] = '.';
			board[row][2] = king;
			board[row][3] = rook;
		}
	}

	private boolean isValid(int r, int c) {
		return r >= 0 && r < 8 && c >= 0 && c < 8;
	}
}