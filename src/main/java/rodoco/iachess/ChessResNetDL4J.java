package rodoco.iachess;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class ChessResNetDL4J {

	public static ComputationGraph createChessModel(int numInputChannels, int numClasses) {
		int numFilters = 64; // Nombre de filtres de convolution

		ComputationGraphConfiguration.GraphBuilder graph = new NeuralNetConfiguration.Builder().seed(12345)
			.updater(new Adam(0.001))
			.weightInit(WeightInit.XAVIER)
			.graphBuilder()
			// Entrée : Tenseur 8x8 avec numInputChannels (ex: 14 plans)
			.addInputs("input")
			.setInputTypes(InputType.convolutional(8, 8, numInputChannels));

		// 1. Couche de convolution initiale
		graph.addLayer("init_conv", new ConvolutionLayer.Builder(3, 3).nOut(numFilters)
			.padding(1, 1) // Conserve la taille 8x8
			.activation(Activation.LEAKYRELU)
			.build(), "input");

		String currentOutput = "init_conv";

		// 2. Empilement de 4 Blocs Résiduels
		for (int i = 0; i < 4; i++) {
			currentOutput = addResidualBlock(graph, currentOutput, i, numFilters);
		}

		// 3. Tête de sortie (Policy Head)
		graph.addLayer("policy_conv", new ConvolutionLayer.Builder(1, 1).nOut(32)
			.activation(Activation.LEAKYRELU)
			.build(), currentOutput)

			.addLayer("flatten", new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT).nOut(numClasses) // Ex: 4672
																												// pour
																												// le
																												// mapping
																												// UCI
																												// complet
																												// ou N
																												// coups
																												// uniques
				.activation(Activation.SOFTMAX)
				.build(), "policy_conv");

		graph.setOutputs("flatten");

		ComputationGraph model = new ComputationGraph(graph.build());
		model.init();
		return model;
	}

	/**
	 * Ajoute un bloc résiduel : Conv -> BN -> ReLU -> Conv -> BN -> Sum(Input,
	 * Output) -> ReLU
	 */
	private static String addResidualBlock(ComputationGraphConfiguration.GraphBuilder graph, String inputName,
			int blockIdx, int numFilters) {
		String c1 = "res_" + blockIdx + "_c1";
		String bn1 = "res_" + blockIdx + "_bn1";
		String c2 = "res_" + blockIdx + "_c2";
		String bn2 = "res_" + blockIdx + "_bn2";
		String add = "res_" + blockIdx + "_add";
		String act = "res_" + blockIdx + "_out";

		graph.addLayer(c1, new ConvolutionLayer.Builder(3, 3).nOut(numFilters)
			.padding(1, 1)
			.build(), inputName)
			.addLayer(bn1, new BatchNormalization(), c1)
			.addLayer(c2, new ConvolutionLayer.Builder(3, 3).nOut(numFilters)
				.padding(1, 1)
				.build(), bn1)
			.addLayer(bn2, new BatchNormalization(), c2)
			// Addition résiduelle : Input + Output du bloc
			.addVertex(add, new ElementWiseVertex(ElementWiseVertex.Op.Add), inputName, bn2)
			.addLayer(act, new ActivationLayer.Builder().activation(Activation.LEAKYRELU)
				.build(), add);

		return act;
	}
}