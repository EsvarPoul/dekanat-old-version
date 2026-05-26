package com.esvar.dekanat.generate.pdf;

import com.esvar.dekanat.generate.DataModelForZalik;
import com.esvar.dekanat.generate.ZalikGenerator;
import org.springframework.stereotype.Component;

/**
 * PDF generator for "Відомість обліку успішності" (залік).
 */
@Component
public class ZalikPdfGenerator extends BaseZalikStylePdfGenerator {

    public static final String NAME = ZalikGenerator.NAME;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String outputSuffix(DataModelForZalik data) {
        return "zalik";
    }
}
