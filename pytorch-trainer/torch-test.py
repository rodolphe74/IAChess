import torch

# 1. Vérifier la disponibilité de CUDA
cuda_dispo = torch.cuda.is_available()
print(f"CUDA disponible : {cuda_dispo}")

if cuda_dispo:
    # 2. Nombre de GPU détectés
    print(f"Nombre de GPU : {torch.cuda.device_count()}")
    
    # 3. Nom du GPU principal
    print(f"GPU utilisé : {torch.cuda.get_device_name(0)}")
    
    # 4. Test d'allocation sur le GPU
    x = torch.rand(3, 3).to("cuda")
    print(f"Tenseur alloué sur : {x.device}")
else:
    print("PyTorch tourne actuellement sur le CPU.")