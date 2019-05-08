package com.mastfrog.testmain.suites;

import com.mastfrog.annotation.AnnotationUtils;
import com.mastfrog.annotation.registries.AbstractLineOrientedRegistrationAnnotationProcessor;
import com.mastfrog.util.service.ServiceProvider;
import java.util.regex.Pattern;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(Processor.class)
@SupportedAnnotationTypes({"com.mastfrog.testmain.suites.Suites"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SuitesProcessor extends AbstractLineOrientedRegistrationAnnotationProcessor  {

    private static final Pattern whitespace = Pattern.compile("\\s");
    private int ix;

    @Override
    protected void handleOne(Element el, AnnotationMirror anno, int order, AnnotationUtils utils) {
        TypeElement te = (TypeElement) el;
        Suites suites = te.getAnnotation(Suites.class);
        for (String suiteName : suites.value()) {
            if (suiteName.contains(":")) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Suite name may not contain a colon: '" + suiteName + "'", te);
                continue;
            }
            if (whitespace.matcher(suiteName).matches()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Suite name may not contain whitespace: '" + suiteName + "'", te);
                continue;
            }

            String line = suiteName + ":" + te.getQualifiedName();
            super.addLine(Suites.SUITES_FILE, line, el);
        }
    }

    @Override
    protected int getOrder(AnnotationMirror anno) {
        return ix++;
    }
}
