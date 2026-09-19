package rodoco.iachess;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class ChessEncoder {

	/**
	 * Convertit une matrice de plateau 8x8 en un INDArray pour ND4J (14 plans x 8 x
	 * 8)
	 */
	public static INDArray boardToINDArray(char[][] board, boolean isWhiteTurn) {
		// Format DL4J Conv2D : [Batch=1, Channels=14, Height=8, Width=8]
		INDArray tensor = Nd4j.zeros(1, 14, 8, 8);

		for (int r = 0; r < 8; r++) {
			for (int c = 0; c < 8; c++) {
				char piece = board[r][c];
				if (piece == '.')
					continue;

				int planeIndex = getPlaneIndex(piece);
				if (planeIndex != -1) {
					tensor.putScalar(new int[] { 0, planeIndex, r, c }, 1.0);
				}
			}
		}

		// Plan 12 : Trait aux Blancs (1.0) ou aux Noirs (0.0)
		if (isWhiteTurn) {
			tensor.slice(0)
				.slice(12)
				.assign(1.0);
		}

		return tensor;
	}

	private static int getPlaneIndex(char piece) {
		switch (piece) {
		case 'P':
			return 0;
		case 'N':
			return 1;
		case 'B':
			return 2;
		case 'R':
			return 3;
		case 'Q':
			return 4;
		case 'K':
			return 5;
		case 'p':
			return 6;
		case 'n':
			return 7;
		case 'b':
			return 8;
		case 'r':
			return 9;
		case 'q':
			return 10;
		case 'k':
			return 11;
		default:
			return -1;
		}
	}
}