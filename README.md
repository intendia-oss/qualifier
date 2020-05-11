# Qualifier: The metadata sharing library 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.intendia.qualifier/qualifier-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.intendia.qualifier/qualifier-parent)
[![Build Status](https://travis-ci.org/intendia-oss/qualifier.svg)](https://travis-ci.org/intendia-oss/qualifier) 
[![Join the chat at https://gitter.im/intendia-oss/qualifier](https://badges.gitter.im/intendia-oss/qualifier.svg)](https://gitter.im/intendia-oss/qualifier?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Java already support metadata using annotations ([JSR 175](https://www.jcp.org/en/jsr/detail?id=175)) and you can use 
processors ([JSR 269](https://www.jcp.org/en/jsr/detail?id=269)) to process this metadata at compile time. This lib
add high-level utilities to extract and share the metadata added using annotations at compile time based on processors.

## So, what is this for?

Sharing data, yep this is it. For example, you are creating a UI with tables, charts or forms. You will have a model,
for example Person. If you use it in your code you will do something like…
```java
TableBuilder<Person> table = TableBuilder.builder()
    .column("Full name", o -> o.firstName + " " + o.lastName)
    .column("Weight", o -> MeasureRenderer.render(o.weight, KILOGRAM))
    .column("Registered at", o -> DateRenderer.render(o.registeredAt, DATE))
    .build();
FormBuilder<Person> form = FormBuilder.builder()
    .entry("First name", new StringBox())    
    .entry("Last name", new StringBox())    
    .entry("Weight", new MeasureBox(KILOGRAM))
    .build();    
```
Those are just e imaginary builders, but those represent just a simple tool to build a widget to show or edit our model
in a UI. There are a lot of metadata in those builders, how to represent the property, how to edit, how to describe, etc.
So, first, where is the right place to put this metadata? yep, right, it is the model. What you expect is to move all
those metadata to the model so you can generalize those builders and extract the metadata from the model itself. If
we move this metadata to the model, the model will looks like this: 
```java
class Person {
    public String firstName; // here there is a lot of metadata too
    public String lastName; // the type (String), the field name (lastName) and even that String is a comparable type!
    public String getFullName() { return firstName + " " + lastName; }
    
    @I18n(description = "Person weight.")
    @Measure(KILOGRAM) public doule weight;
    
    @I18n(description = "Registration date.")
    @Time(DATE) public Date registeredAt;
}                                                       
```
At this point you should note that annotations are just supported in java by default, and that you can create a builder
that extract the metadata using reflection, this is a super nice solution and you should use it and not this lib if it
work for you. So, what does this lib. This lib defines a High-Level API (and kind of best-practices) to extract and use
this metadata so both metadata extractor and metadata consumer can be developed independently and shared.

This lib will generate a metamodel, and this typed metamodel is called `Qualifier`. For example, person will generate
a `Person_Metadata` with each property as a static field like `Person_Metadata.fullName`:
```java
TableBuilder<Person> table = TableBuilder.builder()
    .column(Person_Metadata.fullName)
    .column(Person_Metadata.weight)
    .column(Person_Metadata.registeredAt)
    .build();
FormBuilder<Person> form = FormBuilder.builder()
    .entry(Person_Metadata.firstName)    
    .entry(Person_Metadata.lastName)    
    .entry(Person_Metadata.weight)
    .build();
```  
And the `column` method looks like:
```java
public <T> TableBuilder<Person> column(PropertyQualifier<Person, T> property) {
    Function<T, String> propertyRender = property.getTextRender();
    Function<Person, T> propertyGetter = property.getGetter();
    return column(property.getSummary(), propertyGetter.andThen(propertyRender));
}
````
I.e. the qualifier is a metadata container, and you can create your factories and builders around this idea. Extending
it is super easy, `getTextRender` is itself an extension, and you should define the one you will need in your builders.

There are a special static field that represent the type itself, for the `Person` example it is called 
`Person_Metadata.PersonMetamodel` (thought to be used with static imports). So you can generate a bulk editor for all
`Person` properties using:
```java
TableBuilder<Person> table = TableBuilder.bulk(PersonMetamodel);
FormBuilder<Person> form = FormBuilder.bulk(PersonMetamodel);
```
And build will look like:
```java
public static <T> TableBuilder<T> bulk(Qualifier<Person> bean) {
    TableBuilder<T> builder = TableBuilder.build();
    bean.getProperties().forEach(builder::column);
    return builder;  
} 
```

And this is it, qualifiers. The metadata sharing library.
