package rodoco.iachess;

import javax.swing.*;
import java.awt.*;

public class ChessBoardPanel extends JPanel {

	private final ChessBoardSimulator simulator;
	private final int tileSize = 64; // Taille en pixels de chaque case
	private final int marginSize = 24; // Marge en pixels pour la numérotation (lignes et colonnes)

	// Symboles Unicode pour afficher les pièces sans charger d'images
	private static final String UNICODE_PIECES = "♟♜♞♝♛♚♙♖♘♗♕♔";

	public ChessBoardPanel(ChessBoardSimulator simulator) {
		this.simulator = simulator;
		// La taille globale du panneau prend en compte le plateau + les marges tout
		// autour
		int totalWidth = 8 * tileSize + 2 * marginSize;
		int totalHeight = 8 * tileSize + 2 * marginSize;
		this.setPreferredSize(new Dimension(totalWidth, totalHeight));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		char[][] board = simulator.getBoard();

		// 1. Dessin des étiquettes des lignes (1-8) et colonnes (a-h) dans les marges
		g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
		g2d.setColor(Color.DARK_GRAY);
		FontMetrics labelFm = g2d.getFontMetrics();

		for (int i = 0; i < 8; i++) {
			// A. Numéros de lignes (8 en haut, 1 en bas)
			String rowLabel = String.valueOf(8 - i);
			int yText = marginSize + i * tileSize + (tileSize + labelFm.getAscent() - labelFm.getDescent()) / 2;

			// Marge gauche
			g2d.drawString(rowLabel, (marginSize - labelFm.stringWidth(rowLabel)) / 2, yText);
			// Marge droite
			g2d.drawString(rowLabel, marginSize + 8 * tileSize + (marginSize - labelFm.stringWidth(rowLabel)) / 2,
					yText);

			// B. Lettres des colonnes ('a' à 'h')
			String colLabel = String.valueOf((char) ('a' + i));
			int xText = marginSize + i * tileSize + (tileSize - labelFm.stringWidth(colLabel)) / 2;

			// Marge haute
			g2d.drawString(colLabel, xText, (marginSize + labelFm.getAscent()) / 2);
			// Marge basse
			g2d.drawString(colLabel, xText, marginSize + 8 * tileSize + (marginSize + labelFm.getAscent()) / 2);
		}

		// 2. Dessin du damier et des pièces
		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				int x = marginSize + c * tileSize;
				int y = marginSize + r * tileSize;

				// Dessin des cases du damier
				boolean isLight = (r + c) % 2 != 0;
				g2d.setColor(isLight ? new Color(240, 217, 181) : new Color(181, 136, 99));
				g2d.fillRect(x, y, tileSize, tileSize);

				// Dessin des pièces Unicode
				char piece = board[r][c];
				if (piece != '.') {
					String symbol = getPieceSymbol(piece);
					g2d.setColor(Character.isUpperCase(piece) ? Color.WHITE : Color.BLACK);
					g2d.setFont(new Font("Serif", Font.BOLD, 48));

					FontMetrics fm = g2d.getFontMetrics();
					int px = x + (tileSize - fm.stringWidth(symbol)) / 2;
					int py = y + (tileSize + fm.getAscent() - fm.getDescent()) / 2;

					g2d.drawString(symbol, px, py);
				}
			}
		}
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
		case 'n':
			return "♞";
		case 'B':
			return "♗";
		case 'b':
			return "♝";
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

	public static void doChessBoard(ChessBoardSimulator chessBoardSimulator) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Plateau d'Échecs");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(new ChessBoardPanel(chessBoardSimulator));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	// Exemple rapide d'utilisation dans un JFrame
	public static void main(String[] args) {
		ChessBoardSimulator sim = new ChessBoardSimulator();
		sim.applyMove("e4");
		sim.applyMove("e5");
		sim.applyMove("Nf3");
		doChessBoard(sim);
	}
}