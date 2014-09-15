package com.intendia.qualifier.rebind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.*;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.intendia.qualifier.BeanQualifier;
import com.intendia.qualifier.Qualifier;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/** Will generate a {@link com.intendia.qualifier.StaticQualifierLoader}. */
public class StaticQualifierLoaderGenerator extends Generator {

    private TreeLogger treeLogger;
    private TypeOracle typeOracle;
    private JClassType typeClass;
    private PropertyOracle propertyOracle;

    private String packageName = "";
    private String className = "";

    public PropertyOracle getPropertyOracle() {
        return propertyOracle;
    }

    public void setPropertyOracle(PropertyOracle propertyOracle) {
        this.propertyOracle = propertyOracle;
    }

    public void setTreeLogger(TreeLogger treeLogger) {
        this.treeLogger = treeLogger;
    }

    public TreeLogger getTreeLogger() {
        return treeLogger;
    }

    public TypeOracle getTypeOracle() {
        return typeOracle;
    }

    public void setTypeOracle(TypeOracle typeOracle) {
        this.typeOracle = typeOracle;
    }

    public JClassType getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(JClassType typeName) {
        this.typeClass = typeName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    protected String getSimpleNameFromTypeName(String typeName) {
        return typeName.substring(typeName.lastIndexOf(".") + 1);
    }

    protected PrintWriter tryCreatePrintWriter(GeneratorContext generatorContext,
            String suffix) throws UnableToCompleteException {
        setPackageName(getTypeClass().getPackage().getName());
        setClassName(getTypeClass().getName() + suffix);

        return generatorContext.tryCreate(getTreeLogger(), getPackageName(), getClassName());
    }

    protected JClassType getType(String typeName) throws UnableToCompleteException {
        try {
            return getTypeOracle().getType(typeName);
        } catch (NotFoundException e) {
            getTreeLogger().log(TreeLogger.ERROR, "Cannot find " + typeName, e);
            throw new UnableToCompleteException();
        }
    }

    protected ConfigurationProperty findConfigurationProperty(String prop) throws UnableToCompleteException {
        try {
            return getPropertyOracle().getConfigurationProperty(prop);
        } catch (BadPropertyValueException e) {
            getTreeLogger().log(TreeLogger.ERROR, "Cannot find " + prop + " property in your module.gwt.xml file.", e);
            throw new UnableToCompleteException();
        }
    }

    protected void closeDefinition(SourceWriter sourceWriter) {
        sourceWriter.commit(getTreeLogger());
    }

    private static final String SUFFIX = "Impl";
    protected static final String QUALIFIER_PACKAGE_NAME = Qualifier.class.getPackage().getName();

    @Override
    public String generate(TreeLogger treeLogger, GeneratorContext generatorContext, String typeName)
            throws UnableToCompleteException {
        setTypeOracle(generatorContext.getTypeOracle());
        setPropertyOracle(generatorContext.getPropertyOracle());
        setTreeLogger(treeLogger);
        setTypeClass(getType(typeName));

        PrintWriter printWriter = tryCreatePrintWriter(generatorContext, SUFFIX);

        if (printWriter == null) {
            return typeName + SUFFIX;
        }

        ClassSourceFileComposerFactory composer = initComposer();
        SourceWriter sourceWriter = composer.createSourceWriter(generatorContext, printWriter);

        List<JClassType> beanQualifiers = getBeanQualifiers();

        writeInit(sourceWriter, beanQualifiers);

        closeDefinition(sourceWriter);

        return getPackageName() + "." + getClassName();
    }

    private ClassSourceFileComposerFactory initComposer() {
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(getPackageName(), getClassName());
        composer.addImport(getTypeClass().getQualifiedSourceName());
        composer.addImplementedInterface(getTypeClass().getName());

        // Imports
        composer.addImport(Map.class.getCanonicalName());
        composer.addImport(ImmutableMap.class.getCanonicalName());
        composer.addImport(GWT.class.getCanonicalName());

        return composer;
    }

    private List<JClassType> getBeanQualifiers() throws UnableToCompleteException {
        final JClassType beanQualifierType;
        try {
            beanQualifierType = getTypeOracle().getType(BeanQualifier.class.getName());
        } catch (NotFoundException e) {
            throw new UnableToCompleteException();
        }

        final ImmutableList.Builder<JClassType> builder = ImmutableList.builder();
        for (JClassType type : getTypeOracle().getTypes()) {
            if (type.isAssignableTo(beanQualifierType)) {
                if (type.getName().endsWith("Self")) continue; // discard self instances
                if (type.getPackage().getName().startsWith(QUALIFIER_PACKAGE_NAME)) continue; // discard library
                getTreeLogger().log(TreeLogger.DEBUG, "Added type " + type + " to static qualifier loader");
                builder.add(type);
            }
        }
        return builder.build();
    }

    private void writeInit(SourceWriter sourceWriter, List<JClassType> beanQualifiers) {
        final String genericType = "Class<? extends BeanQualifier<?>>, BeanQualifier<?>";
        final String instanceName = "beanQualifierInstanceMap";
        sourceWriter.println("private Map<%s> %s = ImmutableMap", genericType, instanceName);
        sourceWriter.indent();
        sourceWriter.println(".<%s> builder()", genericType);
        for (JClassType type : beanQualifiers) {
            sourceWriter.println(".put(%1$s.class,%1$s.self)", type.getQualifiedSourceName());
        }
        sourceWriter.println(".build();");
        sourceWriter.outdent();

        sourceWriter.println("public Map<%s> getBeanQualifiers() {", genericType);
        sourceWriter.indentln("return " + instanceName + ";");
        sourceWriter.println("}");
    }
}
