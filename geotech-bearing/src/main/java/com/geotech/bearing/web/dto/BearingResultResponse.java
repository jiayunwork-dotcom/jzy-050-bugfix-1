package com.geotech.bearing.web.dto;

import com.geotech.bearing.domain.BearingFactors;
import com.geotech.bearing.domain.BearingResult;
import com.geotech.bearing.domain.BearingTerms;
import com.geotech.bearing.domain.ShapeFactorSet;

/**
 * 核算结果对外视图，含三个因子、三项形状系数、三项分项与 qu，便于手工核对。
 */
public record BearingResultResponse(String source,
                                    String profileName,
                                    SoilView soil,
                                    GeometryView geometry,
                                    FactorsView factors,
                                    ShapeFactorsView shapeFactors,
                                    TermsView terms) {

    public record SoilView(double cohesionKpa, double frictionAngleDeg, double unitWeightKnM3) {
    }

    public record GeometryView(double widthM, double depthM, String shape) {
    }

    public record FactorsView(double nc, double nq, double ngamma) {
        static FactorsView of(BearingFactors f) {
            return new FactorsView(f.nc(), f.nq(), f.ngamma());
        }
    }

    public record ShapeFactorsView(double sc, double sq, double sGamma) {
        static ShapeFactorsView of(ShapeFactorSet s) {
            return new ShapeFactorsView(s.sc(), s.sq(), s.sGamma());
        }
    }

    public record TermsView(double cohesionTermKpa,
                            double surchargeTermKpa,
                            double weightTermKpa,
                            double quKpa) {
        static TermsView of(BearingTerms t) {
            return new TermsView(t.cohesionTerm(), t.surchargeTerm(), t.weightTerm(), t.qu());
        }
    }

    public static BearingResultResponse from(BearingResult r, String source, String profileName) {
        return new BearingResultResponse(
                source,
                profileName,
                new SoilView(r.soilParameters().cohesionKpa(),
                        r.soilParameters().frictionAngleDeg(),
                        r.soilParameters().unitWeightKnM3()),
                new GeometryView(r.geometry().widthM(), r.geometry().depthM(),
                        r.geometry().shape().name()),
                FactorsView.of(r.factors()),
                ShapeFactorsView.of(r.shapeFactors()),
                TermsView.of(r.terms()));
    }
}
