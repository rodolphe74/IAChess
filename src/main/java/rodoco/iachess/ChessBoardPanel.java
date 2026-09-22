package rodoco.iachess;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import io.github.wolfraam.chessgame.move.Move;
import io.github.wolfraam.chessgame.notation.NotationHelper;

public class ChessBoardPanel extends JPanel {

	private final ChessBoardSimulator simulator;
	private final int tileSize = 64; // Taille en pixels de chaque case
	private final int marginSize = 24; // Marge en pixels pour la numérotation

	// Liste des coups autorisés (ex: "e2e4", "g1f3", etc.)
	private List<String> validMoves = new ArrayList<>();

	// État pour la gestion du drag and drop
	private boolean isDragging = false;
	private int fromRow = -1;
	private int fromCol = -1;
	private int dragX = 0;
	private int dragY = 0;

	public ChessBoardPanel(ChessBoardSimulator simulator) {
		this.simulator = simulator;
		int totalWidth = 8 * tileSize + 2 * marginSize;
		int totalHeight = 8 * tileSize + 2 * marginSize;
		this.setPreferredSize(new Dimension(totalWidth, totalHeight));

		// Écouteurs de la souris
		ChessMouseAdapter adapter = new ChessMouseAdapter();
		this.addMouseListener(adapter);
		this.addMouseMotionListener(adapter);
	}

	public void setValidMoves(List<String> validMoves) {
		this.validMoves = (validMoves != null) ? validMoves : new ArrayList<>();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		char[][] board = simulator.getBoard();

		// 1. Dessin des étiquettes (1-8 et a-h)
		g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
		g2d.setColor(Color.DARK_GRAY);
		FontMetrics labelFm = g2d.getFontMetrics();

		for (int i = 0; i < 8; i++) {
			String rowLabel = String.valueOf(8 - i);
			int yText = marginSize + i * tileSize + (tileSize + labelFm.getAscent() - labelFm.getDescent()) / 2;

			g2d.drawString(rowLabel, (marginSize - labelFm.stringWidth(rowLabel)) / 2, yText);
			g2d.drawString(rowLabel, marginSize + 8 * tileSize + (marginSize - labelFm.stringWidth(rowLabel)) / 2,
					yText);

			String colLabel = String.valueOf((char) ('a' + i));
			int xText = marginSize + i * tileSize + (tileSize - labelFm.stringWidth(colLabel)) / 2;

			g2d.drawString(colLabel, xText, (marginSize + labelFm.getAscent()) / 2);
			g2d.drawString(colLabel, xText, marginSize + 8 * tileSize + (marginSize + labelFm.getAscent()) / 2);
		}

		// 2. Dessin du damier et des pièces
		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				int x = marginSize + c * tileSize;
				int y = marginSize + r * tileSize;

				boolean isLight = (r + c) % 2 != 0;
				g2d.setColor(isLight ? new Color(240, 217, 181) : new Color(181, 136, 99));
				g2d.fillRect(x, y, tileSize, tileSize);

				// Si la pièce est actuellement déplacée au survol, on ne la dessine pas sur sa
				// case d'origine
				if (isDragging && r == fromRow && c == fromCol) {
					continue;
				}

				char piece = board[r][c];
				if (piece != '.') {
					drawPieceSymbol(g2d, piece, x, y);
				}
			}
		}

		// 3. Dessin de la pièce glissée sous le curseur
		if (isDragging && fromRow != -1 && fromCol != -1) {
			char piece = board[fromRow][fromCol];
			drawPieceSymbol(g2d, piece, dragX - tileSize / 2, dragY - tileSize / 2);
		}
	}

	private void drawPieceSymbol(Graphics2D g2d, char piece, int x, int y) {
		String symbol = getPieceSymbol(piece);
		g2d.setColor(Character.isUpperCase(piece) ? Color.WHITE : Color.BLACK);
		g2d.setFont(new Font("Serif", Font.BOLD, 48));

		FontMetrics fm = g2d.getFontMetrics();
		int px = x + (tileSize - fm.stringWidth(symbol)) / 2;
		int py = y + (tileSize + fm.getAscent() - fm.getDescent()) / 2;

		g2d.drawString(symbol, px, py);
	}

	private String getPieceSymbol(char piece) {
		switch (piece) {
		case 'P':
			return "♙";
		case 'p':
			return "♟";
		case 'R':
			return "♖";
		case 'r':
			return "♜";
		case 'N':
			return "♘";
		case 'b':
			return "♝";
		case 'B':
			return "♗";
		case 'n':
			return "♞";
		case 'Q':
			return "♕";
		case 'q':
			return "♛";
		case 'K':
			return "♔";
		case 'k':
			return "♚";
		default:
			return "";
		}
	}

	// Conversion des coordonnées du tableau (r, c) en notation algébrique (ex: 6,4
	// -> "e2")
	private String squareToString(int r, int c) {
		char colChar = (char) ('a' + c);
		char rowChar = (char) ('8' - r);
		return "" + colChar + rowChar;
	}

	// Vérifie si le coup tenté figure dans la liste validMoves
	private boolean isValidMove(int fRow, int fCol, int tRow, int tCol) {
		String fromSq = squareToString(fRow, fCol);
		String toSq = squareToString(tRow, tCol);

		String moveCompact = fromSq + toSq; // Ex: "e2e4"
		String moveDash = fromSq + "-" + toSq; // Ex: "e2-e4"

		for (String move : validMoves) {
			if (move.equalsIgnoreCase(moveCompact) || move.equalsIgnoreCase(moveDash)) {
				return true;
			}
		}
		return false;
	}

	// Gestion des évènements de la souris
	private class ChessMouseAdapter extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			int c = (e.getX() - marginSize) / tileSize;
			int r = (e.getY() - marginSize) / tileSize;

			if (r >= 0 && r < 8 && c >= 0 && c < 8) {
				char piece = simulator.getBoard()[r][c];
				if (piece != '.') {
					isDragging = true;
					fromRow = r;
					fromCol = c;
					dragX = e.getX();
					dragY = e.getY();
					repaint();
				}
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (isDragging) {
				dragX = e.getX();
				dragY = e.getY();
				repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (isDragging) {
				int toC = (e.getX() - marginSize) / tileSize;
				int toR = (e.getY() - marginSize) / tileSize;

				if (toR >= 0 && toR < 8 && toC >= 0 && toC < 8) {
					if (isValidMove(fromRow, fromCol, toR, toC)) {
						String moveStr = squareToString(fromRow, fromCol) + squareToString(toR, toC);
						simulator.applyUCIMove(moveStr);
					}
				}

				isDragging = false;
				fromRow = -1;
				fromCol = -1;
				repaint();
			}
		}
	}

	public static void doChessBoard(ChessBoardSimulator chessBoardSimulator, List<String> validMoves) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Plateau d'Échecs");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			ChessBoardPanel panel = new ChessBoardPanel(chessBoardSimulator);
			panel.setValidMoves(validMoves);
			frame.add(panel);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	public static void main(String[] args) {
		ChessBoardSimulator sim = new ChessBoardSimulator();

		// Exemple de liste de coups autorisés au format coordonnées
		List<String> validMoves = new ArrayList<>();
		validMoves.add("e2e4");
		validMoves.add("d2d4");
		validMoves.add("g1f3");

		doChessBoard(sim, validMoves);
	}
}