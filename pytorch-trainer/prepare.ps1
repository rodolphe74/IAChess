# entrainement via cuda

py -3.12 -m venv pytorch-env
.\pytorch-env\Scripts\activate
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu128
pip install python-chess
pip install onnx onnxscript
pip install tensorboard

tensorboard --logdir=runs