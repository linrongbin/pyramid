package edu.neu.ccs.pyramid.regression.linear_regression;

import edu.neu.ccs.pyramid.dataset.DataSet;
import org.apache.mahout.math.Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by chengli on 2/18/15.
 */
public class ElasticNetLinearRegTrainer {
    private double regularization;
    private double l1Ratio;
    private double epsilon;

    public static Builder getBuilder(){
        return new Builder();
    }

    /**
     * weighted least square fit by coordinate descent
     * @param linearRegression
     * @param dataSet
     * @param labels
     * @param instanceWeights
     */
    public void train(LinearRegression linearRegression, DataSet dataSet, double[] labels, double[] instanceWeights){
        double[] scores = new double[dataSet.getNumDataPoints()];
        IntStream.range(0,dataSet.getNumDataPoints()).parallel().forEach(i->
            scores[i] = linearRegression.predict(dataSet.getRow(i)));
        double lastLoss = loss(linearRegression,scores,labels,instanceWeights);
        while(true){
            iterate(linearRegression,dataSet,labels,instanceWeights,scores);
            double loss = loss(linearRegression,scores,labels,instanceWeights);
            if (Math.abs(lastLoss-loss)<epsilon){
                break;
            }
            lastLoss = loss;
        }
    }

    /**
     * one cycle of coordinate descent
     * @param linearRegression
     * @param dataSet
     * @param labels
     * @param instanceWeights
     */
    private void iterate(LinearRegression linearRegression, DataSet dataSet, double[] labels, double[] instanceWeights, double[] scores){
        double totalWeight = Arrays.stream(instanceWeights).parallel().sum();
        double oldBias = linearRegression.getWeights().getBias();
        double newBias = IntStream.range(0,dataSet.getNumDataPoints()).parallel().mapToDouble(i ->
        instanceWeights[i]*(labels[i]-scores[i] + oldBias)).sum()/totalWeight;
        linearRegression.getWeights().setBias(newBias);
        //update scores
        double difference = newBias - oldBias;
        IntStream.range(0,dataSet.getNumDataPoints()).parallel().forEach(i -> scores[i] = scores[i] + difference);
        for (int j=0;j<dataSet.getNumFeatures();j++){
            optimizeOneFeature(linearRegression,dataSet,labels,instanceWeights,scores,j);
        }
    }

    private void optimizeOneFeature(LinearRegression linearRegression, DataSet dataSet,
                                    double[] labels, double[] instanceWeights,
                                    double[] scores, int featureIndex){
        double oldCoeff = linearRegression.getWeights().getWeightsWithoutBias().get(featureIndex);
        double fit = 0;
        double denominator = 0;
        Vector featureColumn = dataSet.getColumn(featureIndex);
        for (Vector.Element element: featureColumn.nonZeroes()){
            int i = element.index();
            double x = element.get();
            double partialResidual = labels[i] - scores[i] + x*oldCoeff;
            fit += instanceWeights[i]*x*partialResidual;
            denominator += x*x*instanceWeights[i];
        }
        double numerator = softThreshold(fit);
        denominator += regularization*(1-l1Ratio);
        double newCoeff = numerator/denominator;
        linearRegression.getWeights().setWeight(featureIndex,newCoeff);
        //update scores
        double difference = newCoeff - oldCoeff;
        for (Vector.Element element: featureColumn.nonZeroes()){
            int i = element.index();
            double x = element.get();
            scores[i] = scores[i] +  difference*x;
        }
    }


    public double loss(LinearRegression linearRegression, double[] scores, double[] labels, double[] instanceWeights){
        double mse = IntStream.range(0,scores.length).parallel().mapToDouble(i ->
                0.5 * instanceWeights[i] * Math.pow(labels[i] - scores[i], 2))
                .sum();
        double penalty = penalty(linearRegression);
        return mse + penalty;
    }

    public double loss(LinearRegression linearRegression, DataSet dataSet, double[] labels, double[] instanceWeights){
        double mse = IntStream.range(0,dataSet.getNumDataPoints()).parallel().mapToDouble(i ->
                0.5*instanceWeights[i]*Math.pow(labels[i]-linearRegression.predict(dataSet.getRow(i)),2))
                .sum();
        double penalty = penalty(linearRegression);
        return mse + penalty;
    }

    public double penalty(LinearRegression linearRegression){
        Vector vector = linearRegression.getWeights().getWeightsWithoutBias();
        double normCombination = (1-l1Ratio)*0.5*Math.pow(vector.norm(2),2) +
                l1Ratio*vector.norm(1);
        return regularization * normCombination;
    }

    private static double softThreshold(double z, double gamma){
        if (z>0 && gamma < Math.abs(z)){
            return z-gamma;
        }
        if (z<0 && gamma < Math.abs(z)){
            return z+gamma;
        }
        return 0;
    }

    private double softThreshold(double z){
        return softThreshold(z, regularization*l1Ratio);
    }


    public static class Builder{
        private double regularization=0;
        private double l1Ratio=0;
        private double epsilon=0.001;

        public Builder setRegularization(double regularization) {
            this.regularization = regularization;
            return this;
        }


        public Builder setL1Ratio(double l1Ratio) {
            this.l1Ratio = l1Ratio;
            return this;
        }

        public Builder setEpsilon(double epsilon) {
            this.epsilon = epsilon;
            return this;
        }

        public ElasticNetLinearRegTrainer build(){
            ElasticNetLinearRegTrainer trainer = new ElasticNetLinearRegTrainer();
            trainer.regularization = this.regularization;
            trainer.l1Ratio = this.l1Ratio;
            trainer.epsilon = this.epsilon;
            return trainer;
        }
    }


}