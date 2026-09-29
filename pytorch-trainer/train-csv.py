import csv
import json
import os
import time
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.tensorboard import SummaryWriter
import chess

# ==============================================================================
# 1. ENCODEUR DU PLATEAU (Équivalent de ChessEncoder.java)
# ==============================================================================
def board_to_tensor(board: chess.Board) -> torch.Tensor:
    """
    Convertit un objet chess.Board en un tenseur de forme (14, 8, 8).
    - Plans 0 à 5  : Pièces blanches (P, N, B, R, Q, K)
    - Plans 6 à 11 : Pièces noires  (p, n, b, r, q, k)
    - Plan 12      : Trait aux blancs (1.0) ou aux noirs (0.0)
    - Plan 13      : Constante 1.0 (padding / bias)
    """
    tensor = torch.zeros((14, 8, 8), dtype=torch.float32)
    
    piece_idx = {
        chess.PAWN: 0, chess.KNIGHT: 1, chess.BISHOP: 2,
        chess.ROOK: 3, chess.QUEEN: 4, chess.KING: 5
    }
    
    for square, piece in board.piece_map().items():
        row = 7 - chess.square_rank(square)
        col = chess.square_file(square)
        
        offset = 0 if piece.color == chess.WHITE else 6
        plane = offset + piece_idx[piece.piece_type]
        tensor[plane, row, col] = 1.0

    # Indication du trait
    if board.turn == chess.WHITE:
        tensor[12, :, :] = 1.0
        
    tensor[13, :, :] = 1.0
    return tensor

# ==============================================================================
# 2. ARCHITECTURE DU RÉSEAU RESNET (Équivalent de ChessResNetDL4J.java)
# ==============================================================================
class ResNetBlock(nn.Module):
    def __init__(self, channels=64):
        super().__init__()
        self.conv1 = nn.Conv2d(channels, channels, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm2d(channels)
        self.relu = nn.ReLU()
        self.conv2 = nn.Conv2d(channels, channels, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm2d(channels)

    def forward(self, x):
        residual = x
        out = self.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))
        out += residual
        return self.relu(out)

class ChessResNet(nn.Module):
    def __init__(self, input_channels=14, num_classes=1000, num_blocks=4):
        super().__init__()
        self.prep = nn.Sequential(
            nn.Conv2d(input_channels, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU()
        )
        self.res_blocks = nn.ModuleList([ResNetBlock(64) for _ in range(num_blocks)])
        self.policy_head = nn.Sequential(
            nn.Conv2d(64, 32, kernel_size=1),
            nn.BatchNorm2d(32),
            nn.ReLU(),
            nn.Flatten(),
            nn.Linear(32 * 8 * 8, num_classes)
        )

    def forward(self, x):
        x = self.prep(x)
        for block in self.res_blocks:
            x = block(x)
        return self.policy_head(x)

# ==============================================================================
# 3. GESTION DU DICTIONNAIRE DE COUPS (Équivalent de MoveIndexer.java)
# ==============================================================================
class MoveIndexer:
    def __init__(self):
        self.move_to_idx = {}
        self.idx_to_move = {}

    def get_or_create_index(self, move_str: str) -> int:
        if move_str not in self.move_to_idx:
            idx = len(self.move_to_idx)
            self.move_to_idx[move_str] = idx
            self.idx_to_move[idx] = move_str
        return self.move_to_idx[move_str]

    def size(self) -> int:
        return len(self.move_to_idx)

    def save_to_file(self, filepath: str):
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(self.move_to_idx, f, indent=2)

# ==============================================================================
# 4. FONCTIONS DE SCAN ET D'ENTRAÎNEMENT (Équivalent de TrainFullCsv.java)
# ==============================================================================
def scan_unique_moves(csv_file: str, move_indexer: MoveIndexer):
    """ Étape 1 : Premier passage pour recenser tous les coups uniques. """
    with open(csv_file, mode='r', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader, None)  # Ignore l'en-tête
        for row in reader:
            if len(row) < 13:
                continue
            moves_str = row[12].strip()
            for move in moves_str.split():
                if move:
                    move_indexer.get_or_create_index(move)

def fit_batch(model, optimizer, criterion, inputs_list, targets_list, device) -> float:
    X = torch.stack(inputs_list).to(device)                      # [batch_size, 14, 8, 8]
    y = torch.tensor(targets_list, dtype=torch.long).to(device)  # [batch_size]

    optimizer.zero_grad()
    outputs = model(X)
    loss = criterion(outputs, y)
    loss.backward()
    optimizer.step()

    return loss.item()

def train_one_epoch(model, optimizer, criterion, csv_file, move_indexer, device, writer, global_step, epoch, batch_size=128):
    """ Étape 3 : Boucle d'entraînement par époque. """
    model.train()
    inputs_batch = []
    targets_batch = []
    sample_count = 0
    batch_count = 0
    running_loss = 0.0

    with open(csv_file, mode='r', encoding='utf-8') as f:
        reader = csv.reader(f)
        next(reader, None)  # Ignore l'en-tête

        for row in reader:
            if len(row) < 13:
                continue

            winner = row[6].strip().lower()  # "white", "black", ou "draw"
            moves_str = row[12].strip()
            moves = moves_str.split()

            board = chess.Board()

            for move_san in moves:
                if not move_san:
                    continue

                is_white_turn = (board.turn == chess.WHITE)

                # FILTRAGE : N'apprend QUE des coups joués par le GAGNANT
                is_winner_move = (is_white_turn and winner == "white") or \
                                 (not is_white_turn and winner == "black")

                if is_winner_move:
                    # 1. Enregistre l'état du plateau AVANT de jouer le coup
                    input_tensor = board_to_tensor(board)
                    move_idx = move_indexer.get_or_create_index(move_san)

                    inputs_batch.append(input_tensor)
                    targets_batch.append(move_idx)
                    sample_count += 1

                    # 2. Entraînement au passage de la taille du batch
                    if len(inputs_batch) == batch_size:
                        loss_val = fit_batch(model, optimizer, criterion, inputs_batch, targets_batch, device)
                        batch_count += 1
                        global_step += 1
                        running_loss += loss_val

                        # Journalisation TensorBoard par batch
                        writer.add_scalar("Loss/batch", loss_val, global_step)

                        inputs_batch.clear()
                        targets_batch.clear()

                        if batch_count % 100 == 0:
                            print(f"  > Batches traités : {batch_count} ({sample_count} positions) | Loss: {loss_val:.4f}")

                # Avancer le plateau
                try:
                    board.push_san(move_san)
                except ValueError:
                    break  # Si la notation est corrompue dans le CSV

        # Dernier batch restant
        if len(inputs_batch) > 0:
            loss_val = fit_batch(model, optimizer, criterion, inputs_batch, targets_batch, device)
            batch_count += 1
            global_step += 1
            running_loss += loss_val
            writer.add_scalar("Loss/batch", loss_val, global_step)

    avg_loss = running_loss / max(1, batch_count)
    writer.add_scalar("Loss/epoch_avg", avg_loss, epoch)

    return sample_count, global_step, avg_loss

# ==============================================================================
# 5. SCRIPT PRINCIPAL (main)
# ==============================================================================
def main():
    csv_file = "games.csv"
    batch_size = 128
    epochs = 1
    learning_rate = 0.001
    checkpoint_file = "chess_resnet_model.pt"
    indexer_file = "move_indexer.json"

    device = torch.device("cuda" if torch.cuda.is_available() else ("mps" if torch.backends.mps.is_available() else "cpu"))
    print(f"Exécution sur l'appareil : {device}")

    move_indexer = MoveIndexer()

    # STEP 1 : Scan du fichier CSV
    print("1. Scan du fichier CSV...")
    scan_unique_moves(csv_file, move_indexer)
    num_classes = move_indexer.size()
    print(f"Dictionnaire créé : {num_classes} coups uniques identifiés.")

    # STEP 2 : Initialisation du réseau
    print("2. Initialisation du réseau ResNet PyTorch...")
    model = ChessResNet(input_channels=14, num_classes=num_classes, num_blocks=4).to(device)
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)
    criterion = nn.CrossEntropyLoss()

    start_epoch = 1
    global_step = 0

    # REPRISE : Chargement du checkpoint s'il existe
    if os.path.exists(checkpoint_file):
        print(f"\n[REPRISE] Modèle trouvé ! Chargement depuis '{checkpoint_file}'...")
        checkpoint = torch.load(checkpoint_file, map_location=device)
        
        # Si c'est un dictionnaire complet de checkpoint
        if isinstance(checkpoint, dict) and "model_state" in checkpoint:
            model.load_state_dict(checkpoint["model_state"])
            optimizer.load_state_dict(checkpoint["optimizer_state"])
            start_epoch = checkpoint["epoch"] + 1
            global_step = checkpoint.get("global_step", 0)
            print(f"[REPRISE] Reprise à partir de l'époque {start_epoch} (step {global_step}).")
        else:
            # Fallback si seul le state_dict simple du modèle avait été sauvegardé
            model.load_state_dict(checkpoint)
            print("[REPRISE] Poids du modèle chargés (l'optimiseur repart à zéro).")

    # STEP 2b : Initialisation de TensorBoard
    writer = SummaryWriter(log_dir="runs/chess_resnet_experiment")

    if global_step == 0:
        dummy_input = torch.randn(1, 14, 8, 8, device=device)
        writer.add_graph(model, dummy_input)

    # STEP 3 : Boucle d'entraînement
    print("\n3. Début de l'entraînement...")
    end_epoch = start_epoch + epochs - 1

    for epoch in range(start_epoch, end_epoch + 1):
        print(f"--- Époque {epoch} / {end_epoch} ---")
        start_time = time.time()
        
        total_samples, global_step, avg_loss = train_one_epoch(
            model, optimizer, criterion, csv_file, move_indexer, device, writer, global_step, epoch, batch_size
        )
        
        duration = int(time.time() - start_time)
        print(f"Époque {epoch} terminée : {total_samples} exemples | Durée: {duration}s | Loss moy: {avg_loss:.4f}")

        # Sauvegarde intermédiaire à la fin de chaque époque
        torch.save({
            'epoch': epoch,
            'global_step': global_step,
            'model_state': model.state_dict(),
            'optimizer_state': optimizer.state_dict(),
        }, checkpoint_file)
        print(f"  > Checkpoint sauvegardé dans '{checkpoint_file}'.")

    writer.close()
    print("\nEntraînement terminé avec succès !")

    # STEP 4 : Sauvegardes finales
    move_indexer.save_to_file(indexer_file)

    # Exportation ONNX
    print("Exportation au format ONNX pour Java...")
    model.eval()
    dummy_input = torch.randn(1, 14, 8, 8, device=device)
    onnx_file = "chess_resnet_model.onnx"
    
    torch.onnx.export(
        model,
        dummy_input,
        onnx_file,
        input_names=["board_input"],
        output_names=["policy_output"],
        dynamic_axes={"board_input": {0: "batch_size"}, "policy_output": {0: "batch_size"}}
    )
    print(f"Modèle ONNX prêt : {os.path.abspath(onnx_file)}")

if __name__ == "__main__":
    main()