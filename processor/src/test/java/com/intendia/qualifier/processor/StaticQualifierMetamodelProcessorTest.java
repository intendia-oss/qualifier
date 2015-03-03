// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.intendia.qualifier.processor.ProcessorTestUtils.qualifierProcessors;
import static org.truth0.Truth.ASSERT;

import com.google.common.base.Joiner;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

public class StaticQualifierMetamodelProcessorTest {
    @Test
    public void basicQualifier() {
        //@formatter:off
        JavaFileObject sourceFile = JavaFileObjects.forSourceString("Model", Joiner.on("\n").join(
                "import com.intendia.qualifier.annotation.*;",
                "@Qualify interface Model {",
                "  @Qualify(unitOfMeasure = \"ms\")",
                "  Integer getInteger();",
                "  void setInteger(Integer integer);",
                "  @Qualify(extend = @QualifyExtension(key = \"testInteger\", type = Integer.class, value = \"1\"))",
                "  Integer getConstantInteger();",
                "}"));
        ASSERT.about(javaSource()).that(sourceFile).processedWith(qualifierProcessors()).compilesWithoutError();
        //.and().generatesSources(expectedStaticQualifier);
    }
}
