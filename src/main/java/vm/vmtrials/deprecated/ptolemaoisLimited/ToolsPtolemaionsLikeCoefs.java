package vm.vmtrials.deprecated.ptolemaoisLimited;

/**
 *
 * @author Vlada
 */
@Deprecated
public class ToolsPtolemaionsLikeCoefs {

    public static final double[] evaluateEq(float[] anglesRad, int order) {
        switch (order) {
            case 1: {
                return evaluateFirstEq(anglesRad);
            }
            case 2: {
                return evaluateSecondEq(anglesRad);
            }
            case 3: {
                return evaluateThirdEq(anglesRad);
            }
            case 4: {
                return evaluateFourthEq(anglesRad);
            }
        }
        return null;
    }

    // 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
    private static double[] evaluateFirstEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 - Math.cos(alphao)) * (1 - Math.cos(gamma1)) / ((Math.cos(beta1) - Math.cos(alphao + beta1)) * (Math.cos(deltao) - Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) + 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = Math.sin(betaq) / Math.sin(betaq + gamma2) + Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    // 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
    private static double[] evaluateSecondEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 - Math.cos(alphao)) * (1 + Math.cos(gamma1)) / ((Math.cos(beta1) - Math.cos(alphao + beta1)) * (Math.cos(deltao) + Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) - 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = Math.sin(betaq) / Math.sin(betaq + gamma2) - Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    private static double[] evaluateThirdEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 + Math.cos(alphao)) * (1 - Math.cos(gamma1)) / ((Math.cos(beta1) + Math.cos(alphao + beta1)) * (Math.cos(deltao) - Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) - 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = -Math.sin(betaq) / Math.sin(betaq + gamma2) + Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }

    private static double[] evaluateFourthEq(float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float deltao = anglesRad[4];
        float gamma1 = anglesRad[7];
        double A = (1 + Math.cos(alphao)) * (1 + Math.cos(gamma1)) / ((Math.cos(beta1) + Math.cos(alphao + beta1)) * (Math.cos(deltao) + Math.cos(gamma1 + deltao)));
        double B = Math.sin(alphao + beta1) / Math.sin(beta1) * Math.sin(gamma1 + deltao) / Math.sin(deltao) + 1;
        float delta2 = anglesRad[1];
        float gamma2 = anglesRad[2];
        float betaq = anglesRad[5];
        float alphaq = anglesRad[6];
        double C = -Math.sin(betaq) / Math.sin(betaq + gamma2) - Math.sin(delta2) / Math.sin(delta2 + alphaq);
        return new double[]{A * B, A * C};
    }
}
