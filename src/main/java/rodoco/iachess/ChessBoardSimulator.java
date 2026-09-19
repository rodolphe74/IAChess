package rodoco.iachess;

import java.util.Arrays;

public class ChessBoardSimulator {
	private final char[][] board = new char[8][8];
	private boolean isWhiteTurn = true;

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
		if (move == null || move.isEmpty())
			return;

		// 1. Nettoyage des symboles de fin (+, #)
		String cleanMove = move.replace("+", "")
			.replace("#", "")
			.trim();

		// 2. GESTION DU ROQUE
		if (cleanMove.equalsIgnoreCase("O-O") || cleanMove.equalsIgnoreCase("0-0")) {
			applyCastling(true);
			isWhiteTurn = !isWhiteTurn;
			return;
		} else if (cleanMove.equalsIgnoreCase("O-O-O") || cleanMove.equalsIgnoreCase("0-0-0")) {
			applyCastling(false);
			isWhiteTurn = !isWhiteTurn;
			return;
		}

		// 3. GESTION DE LA PROMOTION (ex: "b8=Q" -> promotionPiece = 'Q')
		char promotionPiece = ' ';
		if (cleanMove.contains("=")) {
			int eqIdx = cleanMove.indexOf("=");
			if (eqIdx + 1 < cleanMove.length()) {
				char promotedChar = cleanMove.charAt(eqIdx + 1);
				promotionPiece = isWhiteTurn ? Character.toUpperCase(promotedChar)
						: Character.toLowerCase(promotedChar);
			}
			cleanMove = cleanMove.substring(0, eqIdx);
		}

		// 4. IDENTIFICATION DE LA PIÈCE DE DÉPART
		char piece = isWhiteTurn ? 'P' : 'p';
		if (Character.isUpperCase(cleanMove.charAt(0))) {
			piece = isWhiteTurn ? cleanMove.charAt(0) : Character.toLowerCase(cleanMove.charAt(0));
			cleanMove = cleanMove.substring(1);
		}

		// Retrait du symbole de prise 'x'
		cleanMove = cleanMove.replace("x", "");

		// 5. CALCUL DE LA CASE DE DESTINATION ET DÉSAMBIGUÏSATATION
		if (cleanMove.length() < 2) {
			isWhiteTurn = !isWhiteTurn;
			return;
		}

		char colChar = cleanMove.charAt(cleanMove.length() - 2);
		char rowChar = cleanMove.charAt(cleanMove.length() - 1);

		int destCol = colChar - 'a';
		int destRow = 8 - Character.getNumericValue(rowChar);

		// Extraction de l'indicateur de désambiguïsation s'il existe (ex: 'b' dans
		// "Nbd2")
		Character disambigCol = null;
		Character disambigRow = null;
		if (cleanMove.length() > 2) {
			char extra = cleanMove.charAt(0);
			if (extra >= 'a' && extra <= 'h') {
				disambigCol = extra;
			} else if (extra >= '1' && extra <= '8') {
				disambigRow = extra;
			}
		}

		if (!isValid(destRow, destCol)) {
			isWhiteTurn = !isWhiteTurn;
			return;
		}

		// 6. DÉPLACEMENT ET PROMOTION SUR LE PLATEAU
		int[] src = findSourcePiece(piece, destRow, destCol, disambigCol, disambigRow);
		if (src != null) {
			board[src[0]][src[1]] = '.';
			board[destRow][destCol] = (promotionPiece != ' ') ? promotionPiece : piece;
		}

		isWhiteTurn = !isWhiteTurn;
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